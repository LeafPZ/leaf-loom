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
 * Copyright (c) 2018-2021 FabricMC
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

package dev.aoqia.leaf.loom.build.nesting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.aoqia.leaf.loom.util.Check;
import dev.aoqia.leaf.loom.util.ZipReprocessorUtil;
import dev.aoqia.leaf.loom.util.fmj.LeafModJsonFactory;

public class JarNester {
	private static final Logger LOGGER = LoggerFactory.getLogger(JarNester.class);
	private static final Gson GSON = new Gson();

	public static void nestJars(Collection<File> jars, File modJar) {
		if (jars.isEmpty()) {
			LOGGER.debug("Nothing to nest into {}", modJar.getName());
			return;
		}

		Check.require(LeafModJsonFactory.isModJar(modJar), "Cannot nest jars into none mod jar " + modJar.getName());

		// Ensure deterministic ordering of entries in fabric.mod.json
		Collection<File> sortedJars = jars.stream().sorted(Comparator.comparing(File::getName)).toList();

		try {
			Map<String, Path> nestedJarPaths = new LinkedHashMap<>();

			for (File file : sortedJars) {
				String nestedJarPath = "META-INF/jars/" + file.getName();
				Check.require(LeafModJsonFactory.isModJar(file), "Cannot nest none mod jar: " + file.getName());
				Check.require(nestedJarPaths.putIfAbsent(nestedJarPath, file.toPath()) == null, "Cannot nest multiple jars at " + nestedJarPath);
			}

			ZipReprocessorUtil.appendZipEntries(modJar.toPath(), nestedJarPaths);
			nestedJarPaths.keySet().forEach(path -> LOGGER.debug("Nested {} into {}", path, modJar.getName()));

            // TODO(leaf): Support leaf.mod.json
			ZipReprocessorUtil.transformZipEntry(modJar.toPath(), "leaf.mod.json", bytes -> {
				JsonObject json = GSON.fromJson(new String(bytes), JsonObject.class);
				JsonArray nestedJars = json.has("jars") ? json.getAsJsonArray("jars") : new JsonArray();

				for (File file : sortedJars) {
					String nestedJarPath = "META-INF/jars/" + file.getName();
					JsonObject entry = new JsonObject();
					entry.addProperty("file", nestedJarPath);
					nestedJars.add(entry);
				}

				json.add("jars", nestedJars);
				return GSON.toJson(json).getBytes();
			});
		} catch (IOException e) {
			throw new java.io.UncheckedIOException("Failed to nest jars into " + modJar.getName(), e);
		}
	}
}
