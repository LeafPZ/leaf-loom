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
 * Copyright (c) 2023 FabricMC
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Supplier;

import org.cadixdev.lorenz.MappingSet;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Nested;

import net.fabricmc.lorenztiny.TinyMappingsReader;

import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.configuration.providers.mappings.RemapMappingConfiguration;
import dev.aoqia.leaf.loom.util.Lazy;
import dev.aoqia.leaf.loom.util.service.Service;
import dev.aoqia.leaf.loom.util.service.ServiceFactory;
import dev.aoqia.leaf.loom.util.service.ServiceType;

public final class LorenzMappingService extends Service<LorenzMappingService.Options> {
	public static final ServiceType<Options, LorenzMappingService> TYPE = new ServiceType<>(Options.class, LorenzMappingService.class);

	public interface Options extends Service.Options {
		@Nested
		Property<MappingsService.Options> getMappings();
	}

	public static Provider<Options> createOptions(Project project, RemapMappingConfiguration mappingConfiguration, MappingsNamespace from, MappingsNamespace to) {
		return TYPE.create(project, options -> options.getMappings().set(
			MappingsService.createOptions(
				project,
				mappingConfiguration.tinyMappings,
				from.toString(),
				to.toString(),
				false)
		));
	}

	private final Supplier<MappingSet> mappings = Lazy.of(this::readMappings);

	public LorenzMappingService(Options options, ServiceFactory serviceFactory) {
		super(options, serviceFactory);
	}

	private MappingSet readMappings() {
		MappingsService mappingsService = getServiceFactory().get(getOptions().getMappings().get());

		try {
			try (var reader = new TinyMappingsReader(mappingsService.getMemoryMappingTree(), mappingsService.getFrom(), mappingsService.getTo())) {
				return reader.read();
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read lorenz mappings", e);
		}
	}

	public MappingSet getMappings() {
		return mappings.get();
	}
}
