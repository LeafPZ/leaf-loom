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

package dev.aoqia.leaf.loom.task.launch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import dev.aoqia.leaf.loom.api.RemapConfigurationSettings;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.task.AbstractLoomTask;
import dev.aoqia.leaf.loom.util.Constants;

@DisableCachingByDefault
public abstract class GenerateRemapClasspathTask extends AbstractLoomTask {
	@InputFiles
	@PathSensitive(PathSensitivity.ABSOLUTE)
	public abstract ConfigurableFileCollection getRemapClasspath();

	@OutputFile
	public abstract RegularFileProperty getRemapClasspathFile();

	public GenerateRemapClasspathTask() {
		final ConfigurationContainer configurations = getProject().getConfigurations();

		getRemapClasspath().from(configurations.named(Constants.Configurations.ZOMBOID_COMPILE_LIBRARIES));
		getExtension().getRuntimeRemapConfigurations().stream()
				.map(RemapConfigurationSettings::getName)
				.map(configurations::named)
				.forEach(getRemapClasspath()::from);

		for (Path minecraftJar : getExtension().getZomboidJars(getExtension().getProductionNamespaceEnum().get())) {
			getRemapClasspath().from(minecraftJar.toFile());
		}

		getRemapClasspathFile().set(getExtension().getFiles().getRemapClasspathFile());
	}

	@TaskAction
	public void run() {
		final List<File> remapClasspath = new ArrayList<>(getRemapClasspath().getFiles());

		String str = remapClasspath.stream()
				.map(File::getAbsolutePath)
				.collect(Collectors.joining(File.pathSeparator));

		try {
			Files.writeString(getRemapClasspathFile().getAsFile().get().toPath(), str);
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate remap classpath", e);
		}
	}
}
