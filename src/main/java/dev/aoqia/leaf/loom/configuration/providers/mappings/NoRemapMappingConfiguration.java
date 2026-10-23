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
 * Copyright (c) 2026 FabricMC
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

package dev.aoqia.leaf.loom.configuration.providers.mappings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import dev.aoqia.leaf.loom.api.decompilers.JavadocStyle;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.configuration.DependencyInfo;
import dev.aoqia.leaf.loom.configuration.providers.mappings.tiny.TinyJarInfo;
import dev.aoqia.leaf.loom.configuration.providers.mappings.unpick.UnpickMetadata;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidProvider;
import dev.aoqia.leaf.loom.util.Checksum;
import dev.aoqia.leaf.loom.util.service.ServiceFactory;

public final class NoRemapMappingConfiguration extends MappingConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(NoRemapMappingConfiguration.class);

	private NoRemapMappingConfiguration(String mappingsIdentifier, Path inputJar) {
		super(mappingsIdentifier, inputJar);
	}

	public static NoRemapMappingConfiguration create(Project project, DependencyInfo dependency, ZomboidProvider gameProvider) {
		final String version = dependency.getResolvedVersion();
		final Path inputJar = resolveInputJar(dependency, "annotations");
		final String[] dependencyParts = dependency.getDepString().split(":");
		final String mappingsName = "annotations.%s.%s.%s".formatted(dependencyParts[0], dependencyParts[1], Checksum.of(inputJar).sha256().hex(12));
		final TinyJarInfo jarInfo = readJarInfo(inputJar, dependency, gameProvider, "annotations");
		final String mappingsIdentifier = createMappingsIdentifier(mappingsName, version, getMappingsClassifier(dependency, jarInfo.v2()), gameProvider.gameVersion());
		var mappingConfiguration = new NoRemapMappingConfiguration(mappingsIdentifier, inputJar);
		mappingConfiguration.setup(project, gameProvider, dependency, "annotations");
		return mappingConfiguration;
	}

	@Override
	protected void setupMappings(Project project, ZomboidProvider gameProvider, FileSystem inputJar) throws IOException {
		for (var annotationsData : getAnnotationsData()) {
			if (!MappingsNamespace.OFFICIAL.toString().equals(annotationsData.namespace())) {
				throw new IOException("Annotations patches must use the official namespace");
			}
		}

		if (hasUnpickDefinitions()
				&& getUnpickMetadata() instanceof UnpickMetadata.V2 metadata
				&& !MappingsNamespace.OFFICIAL.toString().equals(metadata.namespace())) {
			throw new IOException("Annotations unpick definitions must use the official namespace");
		}
	}

	static void validateMappings(Path mappings) throws IOException {
		MemoryMappingTree mappingTree = new MemoryMappingTree();
		MappingReader.read(mappings, mappingTree);
		validateMappings(mappingTree);
	}

	private static void validateMappings(MemoryMappingTree mappingTree) throws IOException {
		if (!MappingsNamespace.OFFICIAL.toString().equals(mappingTree.getSrcNamespace()) || !mappingTree.getDstNamespaces().isEmpty()) {
			throw new IOException("Annotations mappings must contain only the official namespace");
		}

		if (mappingTree.getMetadata(MARKDOWN_METADATA_KEY).isEmpty()) {
			LOGGER.warn("Annotations mappings should have the " + MARKDOWN_METADATA_KEY + " metadata entry. Comments are still assumed to be in Markdown.");
		}
	}

	@Override
	public TinyMappingsService getMappingsService(Project project, ServiceFactory serviceFactory) {
		TinyMappingsService mappingsService = super.getMappingsService(project, serviceFactory);

		try {
			validateMappings(mappingsService.getMappingTree());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to validate annotations mappings", e);
		}

		return mappingsService;
	}

	@Override
	public Provider<TinyMappingsService.Options> getMappingsServiceOptions(Project project) {
		return TinyMappingsService.createOptions(project, project.provider(inputJar()::toFile), TinyJarInfo.MAPPINGS_PATH);
	}

	@Override
	public String getMappingsHash() {
		return Checksum.of(inputJar()).sha256().hex();
	}

	@Override
	public MappingsNamespace getRuntimeNamespace() {
		return MappingsNamespace.OFFICIAL;
	}

	@Override
	public JavadocStyle getJavadocStyle() {
		return JavadocStyle.MARKDOWN;
	}
}
