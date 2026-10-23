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

package dev.aoqia.leaf.loom.configuration.decompile;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

import dev.aoqia.leaf.loom.api.decompilers.DecompilerOptions;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidJar;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.mapped.MappedZomboidProvider;
import dev.aoqia.leaf.loom.task.GenerateSourcesTask;
import dev.aoqia.leaf.loom.util.Constants;
import dev.aoqia.leaf.loom.util.Strings;

public final class SplitDecompileConfiguration extends DecompileConfiguration<MappedZomboidProvider.Split> {
	public SplitDecompileConfiguration(Project project, MappedZomboidProvider.Split zomboidProvider) {
		super(project, zomboidProvider);
	}

	@Override
	public String getTaskName(ZomboidJar.Type type) {
		return "gen%sSources".formatted(Strings.capitalize(type.toString()));
	}

	@Override
	public void afterEvaluation() {
		final ZomboidJar commonJar = zomboidProvider.getCommonJar();
		final ZomboidJar clientOnlyJar = zomboidProvider.getClientOnlyJar();

		final TaskProvider<Task> commonDecompileTask = createDecompileTasks("Common", task -> {
			task.getInputJarName().set(commonJar.getName());
			task.getSourcesOutputJar().fileValue(GenerateSourcesTask.getJarFileWithSuffix("-sources.jar", commonJar.getPath()));
		});

		final TaskProvider<Task> clientOnlyDecompileTask = createDecompileTasks("ClientOnly", task -> {
			task.getInputJarName().set(clientOnlyJar.getName());
			task.getSourcesOutputJar().fileValue(GenerateSourcesTask.getJarFileWithSuffix("-sources.jar", clientOnlyJar.getPath()));

			// Don't allow them to run at the same time.
			task.mustRunAfter(commonDecompileTask);
		});

		for (DecompilerOptions options : extension.getDecompilerOptions()) {
			final String decompilerName = options.getFormattedName();

			var commonTask = project.getTasks().named("gen%sSourcesWith%s".formatted("Common", decompilerName));
			var clientOnlyTask = project.getTasks().named("gen%sSourcesWith%s".formatted("ClientOnly", decompilerName));

			clientOnlyTask.configure(task -> {
				task.mustRunAfter(commonTask);
			});

			project.getTasks().register("genSourcesWith" + decompilerName, task -> {
				task.setDescription("Decompile PZ using %s.".formatted(decompilerName));
				task.setGroup(Constants.TaskGroup.LEAF);

				task.dependsOn(commonTask);
				task.dependsOn(clientOnlyTask);
			});
		}

		project.getTasks().register("genSources", task -> {
			task.setDescription("Decompile PZ using the default decompiler.");
			task.setGroup(Constants.TaskGroup.LEAF);

			task.dependsOn(commonDecompileTask);
			task.dependsOn(clientOnlyDecompileTask);
		});
	}

	private TaskProvider<Task> createDecompileTasks(String name, Action<GenerateSourcesTask> configureAction) {
		extension.getDecompilerOptions().forEach(options -> {
			final String decompilerName = options.getFormattedName();
			final String taskName = "gen%sSourcesWith%s".formatted(name, decompilerName);

			project.getTasks().register(taskName, GenerateSourcesTask.class, options).configure(task -> {
				configureAction.execute(task);
				task.dependsOn(project.getTasks().named("validateAccessWidener"));
				task.setDescription("Decompile PZ using %s.".formatted(decompilerName));
				task.setGroup(Constants.TaskGroup.LEAF);
			});
		});

		return project.getTasks().register("gen%sSources".formatted(name), task -> {
			task.setDescription("Decompile PZ (%s) using the default decompiler.".formatted(name));
			task.setGroup(Constants.TaskGroup.LEAF);

			task.dependsOn(project.getTasks().named("gen%sSourcesWith%s".formatted(name, DecompileConfiguration.DEFAULT_DECOMPILER)));
		});
	}
}
