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
 * Copyright (c) 2022 FabricMC
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

package dev.aoqia.leaf.loom.configuration.mods;

import java.nio.file.Path;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.jspecify.annotations.Nullable;

import dev.aoqia.leaf.loom.util.Checksum;

import static dev.aoqia.leaf.loom.configuration.mods.ModConfigurationRemapper.MISSING_GROUP;
import static dev.aoqia.leaf.loom.configuration.mods.ModConfigurationRemapper.replaceIfNullOrEmpty;

public interface ArtifactRef {
	Path path();

	@Nullable Path sources();

	String name();

	String group();

	String version();

	@Nullable String classifier();

	void applyToConfiguration(Project project, NamedDomainObjectProvider<? extends Configuration> configuration);

	record ResolvedArtifactRef(ResolvedArtifact artifact, @Nullable Path sources) implements ArtifactRef {
		@Override
		public Path path() {
			return artifact.getFile().toPath();
		}

		public String group() {
			return replaceIfNullOrEmpty(artifact.getModuleVersion().getId().getGroup(), () -> MISSING_GROUP);
		}

		public String name() {
			return artifact.getModuleVersion().getId().getName();
		}

		public String version() {
			return replaceIfNullOrEmpty(artifact.getModuleVersion().getId().getVersion(), () -> Checksum.of(artifact.getFile()).sha256().hex(10));
		}

		public String classifier() {
			return artifact.getClassifier();
		}

		@Override
		public void applyToConfiguration(Project project, NamedDomainObjectProvider<? extends Configuration> configuration) {
			final DependencyHandler dependencies = project.getDependencies();

			Dependency dep = dependencies.create(artifact.getModuleVersion() + (artifact.getClassifier() == null ? "" : ':' + artifact.getClassifier())); // the owning module of the artifact

			if (dep instanceof ModuleDependency moduleDependency) {
				moduleDependency.setTransitive(false);
			}

			dependencies.add(configuration.getName(), dep);
		}
	}

	record FileArtifactRef(Path path, String group, String name, String version) implements ArtifactRef {
		@Override
		public @Nullable Path sources() {
			return null;
		}

		@Override
		public @Nullable String classifier() {
			return null;
		}

		@Override
		public void applyToConfiguration(Project project, NamedDomainObjectProvider<? extends Configuration> configuration) {
			final DependencyHandler dependencies = project.getDependencies();

			dependencies.add(configuration.getName(), project.files(path.toFile()));
		}
	}
}
