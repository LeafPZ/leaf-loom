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
// spotless:on

package dev.aoqia.leaf.loom.configuration.ide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedModuleVersion;

import dev.aoqia.leaf.loom.api.RunConfiguration;
import dev.aoqia.leaf.loom.util.Constants;

public class RuntimeLibraries {
	public static List<String> getExcludedLibraryPaths(Project project, RunConfiguration runConfiguration) {
		if (!runConfiguration.getRuntimeEnvironment().get().toLowerCase(Locale.ROOT).equals("server")) {
			return Collections.emptyList();
		}

		final Set<ResolvedArtifact> clientLibraries = getArtifacts(project, Constants.Configurations.ZOMBOID_CLIENT_RUNTIME_LIBRARIES);
		final Set<ResolvedArtifact> serverLibraries = getArtifacts(project, Constants.Configurations.ZOMBOID_SERVER_RUNTIME_LIBRARIES);
		final List<String> clientOnlyLibraries = new ArrayList<>();

		for (ResolvedArtifact library : clientLibraries) {
			if (!containsLibrary(serverLibraries, library.getModuleVersion().getId())) {
				clientOnlyLibraries.add(library.getFile().getAbsolutePath());
			}
		}

		return clientOnlyLibraries;
	}

	private static Set<ResolvedArtifact> getArtifacts(Project project, String configuration) {
		return project.getConfigurations().getByName(configuration).getHierarchy()
				.stream()
				.map(c -> c.getResolvedConfiguration().getResolvedArtifacts())
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());
	}

	private static boolean containsLibrary(Set<ResolvedArtifact> artifacts, ModuleVersionIdentifier identifier) {
		return artifacts.stream()
				.map(ResolvedArtifact::getModuleVersion)
				.map(ResolvedModuleVersion::getId)
				.anyMatch(test -> test.getGroup().equals(identifier.getGroup()) && test.getName().equals(identifier.getName()));
	}
}
