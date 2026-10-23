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
 * Copyright (c) 2022-2023 FabricMC
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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.jar.Manifest;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;

import dev.aoqia.leaf.loom.util.FileSystemUtil;

public record TinyJarInfo(boolean v2, Optional<String> gameVersionId) {
	public static final String MAPPINGS_PATH = "mappings/mappings.tiny";
	private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
	private static final String MANIFEST_VERSION_ID_ATTRIBUTE = "Leaf-Version-Id";

	public static TinyJarInfo get(Path jar) {
		try (FileSystemUtil.Delegate delegate = FileSystemUtil.getJarFileSystem(jar)) {
			return new TinyJarInfo(doesJarContainV2Mappings(delegate), getGameVersionId(delegate));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read tiny jar info", e);
		}
	}

	public static void extractMappings(Path jar, Path extractTo) throws IOException {
		try (FileSystemUtil.Delegate delegate = FileSystemUtil.getJarFileSystem(jar)) {
			extractMappings(delegate.fs(), extractTo);
		}
	}

	public static void extractMappings(FileSystem jar, Path extractTo) throws IOException {
		Files.copy(jar.getPath(MAPPINGS_PATH), extractTo, StandardCopyOption.REPLACE_EXISTING);
	}

	private static boolean doesJarContainV2Mappings(FileSystemUtil.Delegate fs) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(fs.getPath("mappings", "mappings.tiny"))) {
			return MappingReader.detectFormat(reader) == MappingFormat.TINY_2_FILE;
		}
	}

	private static Optional<String> getGameVersionId(FileSystemUtil.Delegate fs) throws IOException {
		final Path manifestPath = fs.getPath(MANIFEST_PATH);

		if (Files.exists(manifestPath)) {
			final var manifest = new Manifest();

			try (InputStream in = Files.newInputStream(manifestPath)) {
				manifest.read(in);
			}

			final String versionId = manifest.getMainAttributes().getValue(MANIFEST_VERSION_ID_ATTRIBUTE);
			return Optional.ofNullable(versionId);
		}

		return Optional.empty();
	}
}
