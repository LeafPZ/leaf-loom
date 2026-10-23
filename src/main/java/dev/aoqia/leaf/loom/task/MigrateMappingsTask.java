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
 * Copyright (c) 2019-2025 FabricMC
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

package dev.aoqia.leaf.loom.task;

import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.api.tasks.options.Option;

import dev.aoqia.leaf.loom.task.service.MigrateSourceCodeMappingsService;
import dev.aoqia.leaf.loom.util.DeletingFileVisitor;
import dev.aoqia.leaf.loom.util.service.ScopedServiceFactory;

@UntrackedTask(because = "Always rerun this task.")
public abstract class MigrateMappingsTask extends AbstractMigrateMappingsTask {
	@InputDirectory
	@PathSensitive(PathSensitivity.ABSOLUTE)
	@SkipWhenEmpty
	@Option(option = "input", description = "Java source file directory")
	public abstract DirectoryProperty getInputDir();

	@OutputDirectory
	@Option(option = "output", description = "Remapped source output directory")
	public abstract DirectoryProperty getOutputDir();

	@Nested
	protected abstract Property<MigrateSourceCodeMappingsService.Options> getMigrationServiceOptions();

	public MigrateMappingsTask() {
		getInputDir().convention(getProject().getLayout().getProjectDirectory().dir("src/main/java"));
		getOutputDir().convention(getProject().getLayout().getProjectDirectory().dir("remappedSrc"));
		getMigrationServiceOptions().set(MigrateSourceCodeMappingsService.createOptions(getProject(), getMappings(), getInputDir(), getOutputDir()));
	}

	@TaskAction
	public void doTask() throws Throwable {
		try (var serviceFactory = new ScopedServiceFactory()) {
			MigrateSourceCodeMappingsService service = serviceFactory.get(getMigrationServiceOptions().get());
			service.migrateMappings();
		}

		if (getOverrideInputs().get()) {
			Path inputPath = getInputDir().getAsFile().get().toPath();
			Path outputPath = getOutputDir().getAsFile().get().toPath();

			DeletingFileVisitor.deleteDirectory(inputPath);
			Files.move(outputPath, inputPath);
		}
	}
}
