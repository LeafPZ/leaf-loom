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
 * Copyright (c) 2024 FabricMC
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

package dev.aoqia.leaf.loom.configuration.sandbox;

import java.nio.file.Path;
import java.util.Objects;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.api.RunConfiguration;
import dev.aoqia.leaf.loom.util.Constants;
import dev.aoqia.leaf.loom.util.Platform;
import dev.aoqia.leaf.loom.util.gradle.GradleUtils;

/**
 * Allows the user to specify a sandbox maven artifact as a gradle property.
 * The sandbox jar is read to figure out if it's supported on the current platform.
 * If it is, its added to the runtime classpath and a new client run config is created
 */
public abstract class SandboxConfiguration implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SandboxConfiguration.class);

    @Inject
    public abstract DependencyFactory getDependencyFactory();

    @Override
    public void run() {
        LoomGradleExtension extension = LoomGradleExtension.get(getProject());

        if (extension.isProjectIsolationActive()) {
            LOGGER.debug("Skipping sandbox configuration as project isolation is enabled.");
            return;
        }

        if (getProject().findProperty(Constants.Properties.SANDBOX) == null) {
            LOGGER.debug("No fabric sandbox property set");
            return;
        }

        GradleUtils.afterSuccessfulEvaluation(getProject(), this::evaluate);
    }

    @Inject
    protected abstract Project getProject();

    private void evaluate() {
        final String sandboxNotation = (String) Objects
            .requireNonNull(getProject().findProperty(Constants.Properties.SANDBOX));
        final LoomGradleExtension extension = LoomGradleExtension.get(getProject());
        final ExternalModuleDependency dependency = getDependencyFactory().create(sandboxNotation);
        final Configuration configuration = getProject().getConfigurations().detachedConfiguration(dependency);
        final Path sandboxJar = configuration.getSingleFile().toPath();
        final SandboxMetadata metadata = SandboxMetadata.readFromJar(sandboxJar);

        if (!metadata.supportsPlatform(Platform.CURRENT)) {
            LOGGER.info("Sandbox does not support the current platform");
            return;
        }

        getProject().getDependencies().add(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME, dependency);

        extension.getRuns().create("clientSandbox", settings -> {
            RunConfiguration clientRun = extension.getRuns().getByName("client");

            settings.inherit(clientRun);

            settings.getDisplayName().set("Client Sandbox");

            // The sandbox also acts as DLI
            // Set the sandbox as the true main class
            settings.getDevLaunchMainClass().set(metadata.mainClass());
            settings.getSystemProperties().put("leaf.sandbox.realMain", clientRun.getMainClass().get());
        });
    }
}
