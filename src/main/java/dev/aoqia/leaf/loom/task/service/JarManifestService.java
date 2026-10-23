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

package dev.aoqia.leaf.loom.task.service;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.util.GradleVersion;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.LoomGradlePlugin;
import dev.aoqia.leaf.loom.configuration.InstallerData;
import dev.aoqia.leaf.loom.util.Constants;
import dev.aoqia.leaf.loom.util.LoomVersions;
import net.fabricmc.tinyremapper.TinyRemapper;

public abstract class JarManifestService implements BuildService<JarManifestService.Params> {
	interface Params extends BuildServiceParameters {
		Property<String> getGradleVersion();
		Property<String> getLoomVersion();
		Property<String> getMCEVersion();
		Property<String> getGameVersion();
		Property<String> getTinyRemapperVersion();
		Property<String> getLoaderVersion();
		Property<MixinVersion> getMixinVersion();
	}

	public static Provider<JarManifestService> get(Project project) {
		return project.getGradle().getSharedServices().registerIfAbsent("LoomJarManifestService:" + project.getPath(), JarManifestService.class, spec -> {
			spec.parameters(params -> {
				LoomGradleExtension extension = LoomGradleExtension.get(project);
				Optional<String> tinyRemapperVersion = Optional.ofNullable(TinyRemapper.class.getPackage().getImplementationVersion());

				params.getGradleVersion().set(GradleVersion.current().getVersion());
				params.getLoomVersion().set(LoomGradlePlugin.LOOM_VERSION);
				params.getMCEVersion().set(LoomVersions.MIXIN_COMPILE_EXTENSIONS.version());
				params.getGameVersion().set(project.provider(() -> extension.getZomboidProvider().gameVersion()));
				params.getTinyRemapperVersion().set(tinyRemapperVersion.orElse("unknown"));
				params.getLoaderVersion().set(project.provider(() -> Optional.ofNullable(extension.getInstallerData()).map(InstallerData::version).orElse("unknown")));
				params.getMixinVersion().set(getMixinVersion(project));
			});
		});
	}

	public void apply(Manifest manifest, Map<String, String> extraValues) {
		Attributes attributes = manifest.getMainAttributes();

		extraValues.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> {
					attributes.putValue(entry.getKey(), entry.getValue());
				});

		// Don't set version attributes when running the reproducible build tests as it will break them when anything updates
		if (Boolean.getBoolean("loom.test.reproducible")) {
			return;
		}

		Params p = getParameters();

		attributes.putValue(Constants.Manifest.GRADLE_VERSION, p.getGradleVersion().get());
		attributes.putValue(Constants.Manifest.LOOM_VERSION, p.getLoomVersion().get());
		attributes.putValue(Constants.Manifest.MIXIN_COMPILE_EXTENSIONS_VERSION, p.getMCEVersion().get());
		attributes.putValue(Constants.Manifest.ZOMBOID_VERSION, p.getGameVersion().get());
		attributes.putValue(Constants.Manifest.TINY_REMAPPER_VERSION, p.getTinyRemapperVersion().get());
		attributes.putValue(Constants.Manifest.LEAF_LOADER_VERSION, p.getLoaderVersion().get());

		// This can be overridden by mods if required
		if (!attributes.containsKey(Constants.Manifest.MIXIN_VERSION)) {
			attributes.putValue(Constants.Manifest.MIXIN_VERSION, p.getMixinVersion().get().version());
			attributes.putValue(Constants.Manifest.MIXIN_GROUP, p.getMixinVersion().get().group());
		}
	}

	// Must be public for configuration cache
	public record MixinVersion(String group, String version) implements Serializable { }

	private static Provider<MixinVersion> getMixinVersion(Project project) {
		return project.getConfigurations().named(Constants.Configurations.LOADER_DEPENDENCIES).map(configuration -> {
			// Not super ideal that this uses the mod compile classpath, should prob look into making this not a thing at somepoint
			Optional<Dependency> dependency = configuration
					.getDependencies()
					.stream()
					.filter(dep -> "sponge-mixin".equals(dep.getName()))
					.findFirst();

			if (dependency.isEmpty()) {
				project.getLogger().warn("Could not determine Mixin version for jar manifest");
			}

			return dependency.map(d -> new MixinVersion(d.getGroup(), d.getVersion()))
					.orElse(new MixinVersion("unknown", "unknown"));
		});
	}
}
