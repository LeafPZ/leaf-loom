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

// spotless:on

package dev.aoqia.leaf.loom.task.service;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.IMappingProvider;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.configuration.providers.mappings.RemapMappingConfiguration;
import dev.aoqia.leaf.loom.task.AbstractRemapJarTask;
import dev.aoqia.leaf.loom.util.TinyRemapperHelper;
import dev.aoqia.leaf.loom.util.service.Service;
import dev.aoqia.leaf.loom.util.service.ServiceFactory;
import dev.aoqia.leaf.loom.util.service.ServiceType;

/**
 * A service that provides mappings for remapping.
 */
public final class MappingsService extends Service<MappingsService.Options> implements Closeable {
    public static ServiceType<Options, MappingsService> TYPE = new ServiceType<>(Options.class, MappingsService.class);

    private static final Logger LOGGER = LoggerFactory.getLogger(TinyRemapperService.class);

    private IMappingProvider mappingProvider = null;

    private MemoryMappingTree memoryMappingTree = null;

    public MappingsService(Options options, ServiceFactory serviceFactory) {
        super(options, serviceFactory);
    }

    public static Provider<MappingsService.Options> createForRemapTask(AbstractRemapJarTask remapJarTask) {
        final Project project = remapJarTask.getProject();

        return project.provider(() -> {
            if (remapJarTask.getCustomMappings().isEmpty()) {
                LOGGER.debug("Using default project mappings for remapping");
                return MappingsService
                    .createOptionsWithProjectMappings(project, remapJarTask.getSourceNamespace(),
                        remapJarTask.getTargetNamespace())
                    .get();
            }

            // Custom mappings:
            File mappingsFile = remapJarTask.getCustomMappings().getSingleFile();

            if (mappingsFile.getName().endsWith(".zip") || mappingsFile.getName().endsWith(".jar")) {
                mappingsFile = project.zipTree(mappingsFile)
                    .matching(patternFilterable -> patternFilterable.include("mappings/mappings.tiny"))
                    .getSingleFile();
            }

            LOGGER.info("Using custom mappings for remap task: {}", mappingsFile);
            return MappingsService
                .createOptions(project, mappingsFile.toPath(), remapJarTask.getSourceNamespace(),
                    remapJarTask.getTargetNamespace(), true)
                .get();
        });
    }

    /**
     * Returns options for creating a new mappings service, with a given mappings file.
     */
    public static Provider<Options> createOptions(Project project, Path mappingsFile, Provider<String> from,
        Provider<String> to, boolean remapLocals)
    {
        return TYPE.create(project, o -> {
            o.getMappingsFile().set(mappingsFile.toFile());
            o.getFrom().set(from);
            o.getTo().set(to);
            o.getRemapLocals().set(remapLocals);
        });
    }

    /**
     * Returns options for creating a new mappings service, with a given mappings file.
     */
    public static Provider<Options> createOptions(Project project, Path mappingsFile, String from, String to,
        boolean remapLocals)
    {
        return createOptions(project, mappingsFile, project.provider(() -> from), project.provider(() -> to),
            remapLocals);
    }

    /**
     * Returns options for creating a new mappings service, using the mappings as specified in the project's mapping
     * configuration.
     */
    public static Provider<Options> createOptionsWithProjectMappings(Project project, Provider<String> from,
        Provider<String> to)
    {
        final RemapMappingConfiguration mappingConfiguration = LoomGradleExtension.get(project)
            .getMappingConfiguration();
        return createOptions(project, mappingConfiguration.tinyMappings, from, to, true);
    }

    @Override
    public void close() {
        mappingProvider = null;
    }

    public String getFrom() {
        return getOptions().getFrom().get();
    }

    public Path getMappingsPath() {
        return getOptions().getMappingsFile().get().getAsFile().toPath();
    }

    public IMappingProvider getMappingsProvider() {
        if (mappingProvider == null) {
            try {
                mappingProvider = TinyRemapperHelper.create(getMappingsPath(), getFrom(), getTo(),
                    getOptions().getRemapLocals().get());
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read mappings from: " + getMappingsPath(), e);
            }
        }

        return mappingProvider;
    }

    public MemoryMappingTree getMemoryMappingTree() {
        if (memoryMappingTree == null) {
            memoryMappingTree = new MemoryMappingTree();

            try {
                MappingReader.read(getMappingsPath(), memoryMappingTree);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read mappings from: " + getMappingsPath(), e);
            }
        }

        return memoryMappingTree;
    }

    public String getTo() {
        return getOptions().getTo().get();
    }

    // TODO use a nested TinyMappingsService instead of duplicating it
    public interface Options extends Service.Options {
        @Input
        Property<String> getFrom();

        @InputFile
        @PathSensitive(PathSensitivity.NONE)
        RegularFileProperty getMappingsFile();

        @Input
        Property<Boolean> getRemapLocals();

        @Input
        Property<String> getTo();
    }
}
