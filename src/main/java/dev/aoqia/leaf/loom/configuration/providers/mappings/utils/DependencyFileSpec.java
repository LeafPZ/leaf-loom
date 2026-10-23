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
 * Copyright (c) 2021 FabricMC
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

package dev.aoqia.leaf.loom.configuration.providers.mappings.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.FileCollectionDependency;

import dev.aoqia.leaf.loom.api.mappings.layered.MappingContext;
import dev.aoqia.leaf.loom.api.mappings.layered.spec.FileSpec;

public record DependencyFileSpec(Dependency dependency) implements FileSpec {
	@Override
	public Path get(MappingContext context) {
		if (dependency instanceof FileCollectionDependency fileCollectionDependency) {
			Set<File> files = fileCollectionDependency.getFiles().getFiles();

			if (files.isEmpty()) {
				throw new RuntimeException("FileCollectionDependency (%s) resolved no files".formatted(fileCollectionDependency.toString()));
			} else if (files.size() > 1) {
				throw new RuntimeException("FileCollectionDependency (%s) resolved too many files (%d) only 1 is expected".formatted(fileCollectionDependency.toString(), files.size()));
			}

			return files.iterator().next().toPath();
		}

		return context.resolveDependency(dependency);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dependency.getGroup(), dependency.getName(), dependency.getVersion());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DependencyFileSpec other) {
			return other.dependency().equals(this.dependency());
		}

		return false;
	}
}
