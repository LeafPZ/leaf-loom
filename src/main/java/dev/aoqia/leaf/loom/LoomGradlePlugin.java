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
 * Copyright (c) 2016-2025 FabricMC
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

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginAware;

import dev.aoqia.leaf.loom.api.LoomGradleExtensionAPI;
import dev.aoqia.leaf.loom.configuration.CompileConfiguration;
import dev.aoqia.leaf.loom.configuration.LoomConfigurations;
import dev.aoqia.leaf.loom.configuration.MavenPublication;
import dev.aoqia.leaf.loom.configuration.ide.idea.IdeaConfiguration;
import dev.aoqia.leaf.loom.configuration.sandbox.SandboxConfiguration;
import dev.aoqia.leaf.loom.decompilers.DecompilerConfiguration;
import dev.aoqia.leaf.loom.extension.LoomFiles;
import dev.aoqia.leaf.loom.extension.LoomGradleExtensionImpl;
import dev.aoqia.leaf.loom.task.LoomTasks;
import dev.aoqia.leaf.loom.task.RemapTaskConfiguration;
import dev.aoqia.leaf.loom.util.LibraryLocationLogger;
import dev.aoqia.leaf.loom.util.OneDrive;

public class LoomGradlePlugin implements Plugin<PluginAware> {
	public static final String NAME = "dev.aoqia.leaf.loom";
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final String LOOM_VERSION = Objects.requireNonNullElse(LoomGradlePlugin.class.getPackage().getImplementationVersion(), "0.0.0+unknown");

	/**
	 * An ordered list of setup job classes.
	 */
	private static final List<Class<? extends Runnable>> SETUP_JOBS = List.of(
			LoomConfigurations.class,
			CompileConfiguration.class,
			MavenPublication.class,
			RemapTaskConfiguration.class,
			LoomTasks.class,
			DecompilerConfiguration.class,
			IdeaConfiguration.class,
			SandboxConfiguration.class
	);

	@Override
	public void apply(PluginAware target) {
		target.getPlugins().apply(LoomRepositoryPlugin.class);

		if (target instanceof Project project) {
			apply(project);
		}
	}

	private void apply(Project project) {
		project.getLogger().lifecycle("Leaf Loom: " + LOOM_VERSION);

		LibraryLocationLogger.logLibraryVersions();
		OneDrive.verify(project);

		// Apply default plugins
		project.apply(Map.of("plugin", "java-library"));
		project.apply(Map.of("plugin", "eclipse"));

		// Setup extensions
		project.getExtensions().create(LoomGradleExtensionAPI.class, "loom", LoomGradleExtensionImpl.class, project, LoomFiles.create(project));
		// NOTE(leaf): Disable leaf API
		// project.getExtensions().create(LeafApiExtension.class, "fabricApi", LeafApiExtensionImpl.class);

		for (Class<? extends Runnable> jobClass : SETUP_JOBS) {
			project.getObjects().newInstance(jobClass).run();
		}

		project.apply(Map.of("plugin", LoomCompanionGradlePlugin.NAME));
	}
}
