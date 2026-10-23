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
 * Copyright (c) 2021-2022 FabricMC
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

package dev.aoqia.leaf.loom;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactRepositoryContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.PluginAware;

import dev.aoqia.leaf.loom.extension.LoomFiles;
import dev.aoqia.leaf.loom.util.MirrorUtil;

public class LoomRepositoryPlugin implements Plugin<PluginAware> {
	@Override
	public void apply(PluginAware target) {
		if (target instanceof Settings settings) {
			declareRepositories(settings.getDependencyResolutionManagement().getRepositories(), LoomFiles.create(settings), settings);

			// Leave a project-local marker so projects don't try to override these.
			settings.getGradle().beforeProject(project -> project.getPluginManager().apply(RepositoryManagementPlugin.class));
		} else if (target instanceof Project project) {
			if (project.getPlugins().hasPlugin(RepositoryManagementPlugin.class)) {
				return;
			}

			declareRepositories(project.getRepositories(), LoomFiles.create(project), project);
		} else if (target instanceof Gradle) {
			return;
		} else {
			throw new IllegalArgumentException("Expected target to be a Project or Settings, but was a " + target.getClass());
		}
	}

	public static final class RepositoryManagementPlugin implements Plugin<Project> {
		@Override
		public void apply(Project target) {
		}
	}

	private void declareRepositories(RepositoryHandler repositories, LoomFiles files, ExtensionAware target) {
		declareLocalRepositories(repositories, files);

        repositories.maven(repo -> {
			repo.setName("Leaf");
			repo.setUrl(MirrorUtil.getLeafReleaseRepository(target));
		});

		repositories.maven(repo -> {
			repo.setName("Fabric");
			repo.setUrl(MirrorUtil.getFabricRepository(target));
		});

		repositories.mavenCentral();
	}

	private void declareLocalRepositories(RepositoryHandler repositories, LoomFiles files) {
		repositories.maven(repo -> {
			repo.setName("LoomLocalRemappedMods");
			repo.setUrl(files.getRemappedModCache());
		});

		repositories.maven(repo -> {
			repo.setName("LoomGlobalZomboid");
			repo.setUrl(files.getGlobalZomboidRepo());
		});

		repositories.maven(repo -> {
			repo.setName("LoomLocalZomboid");
			repo.setUrl(files.getLocalZomboidRepo());
		});
	}

	public static void forceLWJGLFromMavenCentral(RepositoryHandler repositories) {
		if (repositories.findByName("MavenCentralLWJGL") != null) {
			// Already applied.
			return;
		}

		// Force LWJGL from central, as it contains all the platform natives.
		MavenArtifactRepository central = repositories.maven(repo -> {
			repo.setName("MavenCentralLWJGL");
			repo.setUrl(ArtifactRepositoryContainer.MAVEN_CENTRAL_URL);
			repo.content(content -> {
				content.includeGroup("org.lwjgl");
			});
		});

		repositories.exclusiveContent(repository -> {
			repository.forRepositories(central);
			repository.filter(filter -> {
				filter.includeGroup("org.lwjgl");
			});
		});
	}
}
