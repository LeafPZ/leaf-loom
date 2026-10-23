// spotless:off
/*
 * Copyright 2026 aoqia
 *
 * This file was originally released under LICENSE_UPSTREAM (see below for the original header) and has been relicensed
 * to GNU's AGPLv3 license, with additional terms in the LICENSE_EXCEPTION.md file.
 *
 * All modifications made to this file after the original version were created separate to the original copyright.
 * These modifications are provided under the AGPLv3 license (including exception terms). The portions of this file that
 * were not modified remain subject to the original copyright notice and license, and the original LICENSE is kept in
 * LICENSE_UPSTREAM located alongside LICENSE.
 *
 * The complete record of changes to this file is publicly available in the project's Git history, which anyone may view
 * at any time for any reason.
 *
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2022-2025 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
// spotless:on

package dev.aoqia.leaf.loom.configuration.providers.mappings.tiny;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.configuration.providers.mappings.IntermediateMappingsService;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidProvider;
import dev.aoqia.leaf.loom.util.Pair;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MappingTree;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

public final class MappingsMerger {
	private static final Logger LOGGER = LoggerFactory.getLogger(MappingsMerger.class);

	public static void mergeAndSaveMappings(Path from, Path out, ZomboidProvider provider, IntermediateMappingsService intermediateMappingsService) throws IOException {
		long start = System.currentTimeMillis();
		LOGGER.info(":merging mappings");
        mergeAndSaveMappings(from, out, intermediateMappingsService);
		LOGGER.info(":merged mappings in {}ms", System.currentTimeMillis() - start);
	}

	@VisibleForTesting
	public static void mergeAndSaveMappings(Path from, Path out, IntermediateMappingsService intermediateMappingsService) throws IOException {
		MemoryMappingTree intermediaryTree = new MemoryMappingTree();
		intermediateMappingsService.getMemoryMappingTree().accept(new MappingSourceNsSwitch(intermediaryTree, MappingsNamespace.INTERMEDIARY.toString()));

		try (BufferedReader reader = Files.newBufferedReader(from, StandardCharsets.UTF_8)) {
			Tiny2FileReader.read(reader, intermediaryTree);
		}

		MemoryMappingTree officialTree = new MemoryMappingTree();
		UnobfuscatedMappingNsCompleter nsCompleter = new UnobfuscatedMappingNsCompleter(officialTree, MappingsNamespace.NAMED.toString(), Map.of(MappingsNamespace.OFFICIAL.toString(), MappingsNamespace.INTERMEDIARY.toString()));
		MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsCompleter, MappingsNamespace.OFFICIAL.toString());
		intermediaryTree.accept(nsSwitch);

		inheritMappedNamesOfEnclosingClasses(officialTree);
		cleanupMappingLeakageToOfficial(officialTree);

		try (var writer = new Tiny2FileWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8), false)) {
			officialTree.accept(writer);
		}
	}

	@VisibleForTesting
	public static void legacyMergedMergeAndSaveMappings(Path from, Path out, IntermediateMappingsService intermediateMappingsService) throws IOException {
		MemoryMappingTree intermediaryTree = new MemoryMappingTree();
		intermediateMappingsService.getMemoryMappingTree().accept(intermediaryTree);

		try (BufferedReader reader = Files.newBufferedReader(from, StandardCharsets.UTF_8)) {
			Tiny2FileReader.read(reader, intermediaryTree);
		}

		MemoryMappingTree officialTree = new MemoryMappingTree();
		UnobfuscatedMappingNsCompleter nsCompleter = new UnobfuscatedMappingNsCompleter(officialTree, MappingsNamespace.NAMED.toString(), Map.of(MappingsNamespace.CLIENT_OFFICIAL.toString(), MappingsNamespace.INTERMEDIARY.toString(), MappingsNamespace.SERVER_OFFICIAL.toString(), MappingsNamespace.INTERMEDIARY.toString()));
		intermediaryTree.accept(nsCompleter);

		// versions this old strip inner class attributes
		// from the obfuscated jars anyway
		//inheritMappedNamesOfEnclosingClasses(officialTree);

		try (var writer = new Tiny2FileWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8), false)) {
			officialTree.accept(writer);
		}
	}

	/**
	 * Searches the mapping tree for inner classes with no mapped name, whose enclosing classes have mapped names.
	 * Currently, Yarn does not export mappings for these inner classes.
	 */
	private static void inheritMappedNamesOfEnclosingClasses(MemoryMappingTree tree) {
		assert tree.getNamespaceId("intermediary") > MappingTree.SRC_NAMESPACE_ID;

		// Create an index by intermediary names for faster lookups during the propagation
		tree.setIndexByDstNames(true);

		tree.propagateOuterClassNames("intermediary", List.of("named"), false);
	}

	/**
	 * When merging mappings, intermediary names for methods can ended up leaking into the official namespace.
	 * This is because mapping-io does not have class inheritance information when doing so.
	 * Workaround this problem by deleting invalid mapping entries.
	 */
	private static void cleanupMappingLeakageToOfficial(MemoryMappingTree tree) {
		int intermediaryId = tree.getNamespaceId("intermediary");
		int officialId = tree.getNamespaceId("official");

		List<Pair<String, String>> entriesToRemove = new ArrayList<>();

		for (MappingTree.ClassMapping classMapping : tree.getClasses()) {
			for (MappingTree.MethodMapping methodMapping : classMapping.getMethods()) {
				String intermediary = methodMapping.getName(intermediaryId);
				String official = methodMapping.getName(officialId);

				if (intermediary != null && official != null && intermediary.startsWith("method_") && intermediary.equals(official)) {
					entriesToRemove.add(new Pair<>(methodMapping.getSrcName(), methodMapping.getSrcDesc()));
				}
			}

			for (Pair<String, String> entry : entriesToRemove) {
				classMapping.removeMethod(entry.left(), entry.right());
			}
		}
	}
}
