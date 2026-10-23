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
 * Copyright (c) 2021-2023 FabricMC
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

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import dev.aoqia.leaf.loom.api.RunConfiguration;
import dev.aoqia.leaf.loom.task.LoomTasks;
import dev.aoqia.leaf.loom.task.RunGameTask;

public abstract class RunConfigSettings implements Named, RunConfiguration, RunConfigurationInternal {
    // The base name of the run configuration, which is the name it is created with, i.e. 'client'
    private final String name;
    private final Project project;

    @Inject
    public RunConfigSettings(Project project, String name) {
        this.name = name;
        this.project = project;
        DefaultRunConfigurationSettings.configureDefaults(this, project);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void task(Action<RunGameTask> action) {
        String taskName = LoomTasks.getRunConfigTaskName(this);

        project.getTasks().named(taskName::equals).withType(RunGameTask.class).configureEach(action);
    }

    // Backwards compatibility shims:

    // Note: Overridden for backwards compatibility
    @Override
    public abstract Property<Boolean> getAppendProjectPathToDisplayName();

    // Note: Overridden for backwards compatibility
    @Override
    public abstract Property<String> getMainClass();

    // Note: Overridden for backwards compatibility
    @Override
    public void client() {
        RunConfigurationInternal.super.client();
    }

    // Note: Overridden for backwards compatibility
    @Override
    public void server() {
        RunConfigurationInternal.super.server();
    }

    // Note: Overload method for backwards compatibility
    public void inherit(RunConfigSettings parent) {
        RunConfigurationInternal.super.inherit(parent);
    }

    // Note: Overridden for backwards compatibility
    @Override
    public abstract Property<String> getIdeConfigFolder();

    // Deprecated methods:

    /**
     * Technically deprecated, but as it shadows the "project" property from the parent class, it reports as
     * deprecated in IDEs, which is undesirable.
     */
    public Project getProject() {
        return project;
    }
}
