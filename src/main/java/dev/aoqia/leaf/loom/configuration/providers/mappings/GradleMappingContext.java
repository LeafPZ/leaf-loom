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
 * Copyright (c) 2018-2025 FabricMC
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

package dev.aoqia.leaf.loom.configuration.providers.mappings;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.logging.Logger;

import net.fabricmc.mappingio.tree.MemoryMappingTree;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingContext;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidProvider;
import dev.aoqia.leaf.loom.util.copygamefile.CopyGameFileBuilder;
import dev.aoqia.leaf.loom.util.download.DownloadBuilder;
import dev.aoqia.leaf.loom.util.gradle.GradleUtils;
import dev.aoqia.leaf.loom.util.service.ScopedServiceFactory;

public class GradleMappingContext implements MappingContext {
    private final LoomGradleExtension extension;
    private final Project project;
    private final String workingDirName;

    public GradleMappingContext(Project project, String workingDirName) {
        this.project = project;
        this.extension = LoomGradleExtension.get(project);
        this.workingDirName = workingDirName;
    }

    @Override
    public CopyGameFileBuilder copyGameFile(String url) {
        return extension.copyGameFile(url);
    }

    @Override
    public DownloadBuilder download(String url) {
        return extension.download(url);
    }

    public LoomGradleExtension getExtension() {
        return extension;
    }

    @Override
    public Logger getLogger() {
        return project.getLogger();
    }

    public Project getProject() {
        return project;
    }

    @Override
    public boolean hasProperty(String property) {
        return GradleUtils.getBooleanProperty(project, property);
    }

    @Override
    public Supplier<MemoryMappingTree> intermediaryTree() {
        return () -> {
            if (!extension.getUseIntermediateMappings().get()) {
                throw new IllegalStateException("Intermediary mappings is disabled");
            }

            try (var serviceFactory = new ScopedServiceFactory()) {
                IntermediateMappingsService intermediateMappingsService = serviceFactory
                    .get(IntermediateMappingsService.createOptions(project, zomboidProvider()));
                return intermediateMappingsService.getMemoryMappingTree();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    @Override
    public boolean isUsingIntermediateMappings() {
        return extension.getUseIntermediateMappings().get();
    }

    @Override
    public String productionNamespace() {
        return extension.getProductionNamespace().get();
    }

    @Override
    public boolean refreshDeps() {
        return extension.refreshDeps();
    }

    @Override
    public Path resolveDependency(Dependency dependency) {
        Configuration configuration = project.getConfigurations().detachedConfiguration(dependency);
        // Don't allow changing versions as this breaks down with how we cache layered mappings.
        configuration.resolutionStrategy(ResolutionStrategy::failOnNonReproducibleResolution);
        return configuration.getSingleFile().toPath();
    }

    @Override
    public Path resolveDependency(MinimalExternalModuleDependency dependency) {
        return resolveDependency(project.getDependencies().create(dependency));
    }

    @Override
    public Path resolveMavenDependency(String mavenNotation) {
        return resolveDependency(project.getDependencies().create(mavenNotation));
    }

    @Override
    public Path workingDirectory(String name) {
        return new File(zomboidProvider().dir("layered/working_dir/" + workingDirName), name).toPath();
    }

    @Override
    public ZomboidProvider zomboidProvider() {
        return extension.getZomboidProvider();
    }
}
