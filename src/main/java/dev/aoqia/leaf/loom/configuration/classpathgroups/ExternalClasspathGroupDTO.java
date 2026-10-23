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
 * Copyright (c) 2025 FabricMC
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

package dev.aoqia.leaf.loom.configuration.classpathgroups;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import dev.aoqia.leaf.loom.LoomGradlePlugin;
import dev.aoqia.leaf.loom.util.gradle.SourceSetHelper;
import dev.aoqia.leaf.loom.util.gradle.SourceSetReference;

/**
 * This object is exported by projects as a json file to be consumed by others to correctly populate the classpath groups.
 */
public record ExternalClasspathGroupDTO(String projectPath, Map<String, List<String>> classpaths) implements Serializable {
	public static ExternalClasspathGroupDTO createFromProject(Project project) {
		SourceSetContainer sourceSets = SourceSetHelper.getSourceSets(project);

		Map<String, List<String>> classpaths = new HashMap<>();

		for (SourceSet sourceSet : sourceSets) {
			SourceSetReference ref = new SourceSetReference(sourceSet, project);
			List<File> classpath = SourceSetHelper.getClasspath(ref, true);
			classpaths.put(sourceSet.getName(), classpath.stream().map(File::getAbsolutePath).toList());
		}

		return new ExternalClasspathGroupDTO(project.getPath(), Collections.unmodifiableMap(classpaths));
	}

	public static Map<String, ExternalClasspathGroupDTO> resolveExternal(Set<File> files) {
		Map<String, ExternalClasspathGroupDTO> map = new HashMap<>();

		for (File file : files) {
			String json;

			try {
				json = Files.readString(file.toPath());
			} catch (IOException e) {
				throw new UncheckedIOException("Failed to read external classpath group file: " + file, e);
			}

			ExternalClasspathGroupDTO dto = LoomGradlePlugin.GSON.fromJson(json, ExternalClasspathGroupDTO.class);
			map.put(dto.projectPath(), dto);
		}

		return Collections.unmodifiableMap(map);
	}

	public List<String> getForSourceSet(String sourceSetName) {
		return Objects.requireNonNull(classpaths.get(sourceSetName), "No classpath found for source set: " + sourceSetName);
	}
}
