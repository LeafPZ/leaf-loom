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
 * Copyright (c) 2024-2025 FabricMC
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

import java.io.File;

import org.gradle.api.IllegalDependencyNotation;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Nested;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.configuration.providers.mappings.TinyMappingsService;
import dev.aoqia.leaf.loom.util.service.Service;
import dev.aoqia.leaf.loom.util.service.ServiceFactory;
import dev.aoqia.leaf.loom.util.service.ServiceType;

public final class MigrateMappingsService extends Service<MigrateMappingsService.Options> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MigrateMappingsService.class);
	private static final ServiceType<Options, MigrateMappingsService> TYPE = new ServiceType<>(Options.class, MigrateMappingsService.class);

	public MigrateMappingsService(Options options, ServiceFactory serviceFactory) {
		super(options, serviceFactory);
	}

	public interface Options extends Service.Options {
		@Nested
		Property<MappingsService.Options> getSourceMappings();
		@Nested
		Property<TinyMappingsService.Options> getTargetMappings();
		@Classpath
		ConfigurableFileCollection getClasspath();
	}

	public static Provider<Options> createOptions(Project project, Provider<String> targetMappings) {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		final Provider<String> from = project.provider(() -> "intermediary");
		final Provider<String> to = project.provider(() -> "named");

		ConfigurableFileCollection classpath = project.getObjects().fileCollection();
		classpath.from(project.getConfigurations().named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME));
		// Question: why are both of these needed?
		classpath.from(extension.getZomboidJars(MappingsNamespace.INTERMEDIARY));
		classpath.from(extension.getZomboidJars(MappingsNamespace.NAMED));

		return TYPE.create(project, (o) -> {
			Provider<File> targetMappingsFile = getTargetMappingsFile(project, targetMappings.get());
			o.getSourceMappings().set(MappingsService.createOptionsWithProjectMappings(project, from, to));
			o.getTargetMappings().set(TinyMappingsService.createOptions(project, targetMappingsFile, "mappings/mappings.tiny"));
			o.getClasspath().from(classpath);
		});
	}

	public MappingsService getSourceMappingsService() {
		return getServiceFactory().get(getOptions().getSourceMappings().get());
	}

	public TinyMappingsService getTargetMappingsService() {
		return getServiceFactory().get(getOptions().getTargetMappings().get());
	}

	public FileCollection getClasspath() {
		return getOptions().getClasspath();
	}

	/**
	 * Return a mappings file for the requested mappings.
	 */
	private static Provider<File> getTargetMappingsFile(Project project, String mappings) {
		if (mappings == null || mappings.isEmpty()) {
			throw new IllegalArgumentException("No mappings were specified. Use --mappings=\"\" to specify target mappings");
		}

		try {
            Dependency dependency = project.getDependencies().create(mappings);
            return project.provider(() -> project.getConfigurations().detachedConfiguration(dependency).getSingleFile());
		} catch (IllegalDependencyNotation ignored) {
			LOGGER.info("Could not locate mappings, presuming V2 Yarn");
			String mavenNotation = "dev.aoqia.leaf:yarn:%s:v2".formatted(mappings);
			return project.provider(() -> project.getConfigurations().detachedConfiguration(project.getDependencies().create(mavenNotation)).getSingleFile());
		}
	}
}
