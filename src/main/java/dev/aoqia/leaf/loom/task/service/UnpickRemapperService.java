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
 * Copyright (c) 2025 FabricMC
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Reader;
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Remapper;
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Writer;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Nested;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.api.TrClass;
import net.fabricmc.tinyremapper.api.TrField;

import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.configuration.providers.mappings.unpick.UnpickMetadata;
import dev.aoqia.leaf.loom.util.JarPackageIndex;
import dev.aoqia.leaf.loom.util.service.Service;
import dev.aoqia.leaf.loom.util.service.ServiceFactory;
import dev.aoqia.leaf.loom.util.service.ServiceType;

public class UnpickRemapperService extends Service<UnpickRemapperService.Options> {
	public static final ServiceType<Options, UnpickRemapperService> TYPE = new ServiceType<>(Options.class, UnpickRemapperService.class);

	public interface Options extends Service.Options {
		@Nested
		Property<TinyRemapperService.Options> getTinyRemapper();
	}

	public static Provider<Options> createOptions(Project project, UnpickMetadata.V2 metadata) {
		return TYPE.create(project, options -> {
			options.getTinyRemapper().set(TinyRemapperService.createSimple(project,
					project.provider(metadata::namespace),
					project.provider(MappingsNamespace.NAMED::toString),
					TinyRemapperService.ClasspathLibraries.INCLUDE // Must include the full set of libraries on classpath so fields can be looked up. This does use a lot of memory however...
			));
		});
	}

	public UnpickRemapperService(Options options, ServiceFactory serviceFactory) {
		super(options, serviceFactory);
	}

	/**
	 * Return the remapped definitions.
	 */
	public String remap(File input) throws IOException {
		try (Reader reader = Files.newBufferedReader(input.toPath())) {
			return remap(reader);
		}
	}

	public String remap(Reader input) throws IOException {
		TinyRemapperServiceInterface tinyRemapperService = getServiceFactory().get(getOptions().getTinyRemapper());
		TinyRemapper tinyRemapper = tinyRemapperService.getTinyRemapperForRemapping();

		List<Path> classpath = getOptions().getTinyRemapper().get().getClasspath().getFiles().stream().map(File::toPath).toList();
		JarPackageIndex packageIndex = JarPackageIndex.create(classpath);

		return doRemap(input, tinyRemapper, packageIndex);
	}

	private String doRemap(Reader input, TinyRemapper remapper, JarPackageIndex packageIndex) throws IOException {
		try (var reader = new UnpickV3Reader(new BufferedReader(input))) {
			var writer = new UnpickV3Writer();
			reader.accept(new UnpickRemapper(writer, remapper, packageIndex));
			return writer.getOutput().replace(System.lineSeparator(), "\n");
		}
	}

	private static final class UnpickRemapper extends UnpickV3Remapper {
		private final TinyRemapper tinyRemapper;
		private final Remapper remapper;
		private final JarPackageIndex jarPackageIndex;

		private UnpickRemapper(UnpickV3Visitor downstream, TinyRemapper tinyRemapper, JarPackageIndex jarPackageIndex) {
			super(downstream);
			this.tinyRemapper = tinyRemapper;
			this.remapper = tinyRemapper.getEnvironment().getRemapper();
			this.jarPackageIndex = jarPackageIndex;
		}

		@Override
		protected String mapClassName(String className) {
			return remapper.map(className.replace('.', '/')).replace('/', '.');
		}

		@Override
		protected String mapFieldName(String className, String fieldName, String fieldDesc) {
			return remapper.mapFieldName(className.replace('.', '/'), fieldName, fieldDesc);
		}

		@Override
		protected String mapMethodName(String className, String methodName, String methodDesc) {
			return remapper.mapMethodName(className.replace('.', '/'), methodName, methodDesc);
		}

		// Return all classes in the given package, not recursively.
		@Override
		protected List<String> getClassesInPackage(String pkg) {
			return jarPackageIndex.packages().getOrDefault(pkg, Collections.emptyList())
					.stream()
					.map(className -> pkg + "." + className)
					.toList();
		}

		@Override
		protected String getFieldDesc(String className, String fieldName) {
			TrClass trClass = tinyRemapper.getEnvironment().getClass(className.replace('.', '/'));

			if (trClass != null) {
				for (TrField trField : trClass.getFields()) {
					if (trField.getName().equals(fieldName)) {
						return trField.getDesc();
					}
				}
			}

			String fieldDesc = getFieldDescFromReflection(className, fieldName);

			if (fieldDesc == null) {
				throw new IllegalStateException("Could not find field " + fieldName + " in class " + className);
			}

			return fieldDesc;
		}

		private static String getFieldDescFromReflection(String className, String fieldName) {
			try {
				// Use the bootstrap class loader, which should only resolve classes from the JDK.
				// Don't run the static initializer.
				Class<?> clazz = Class.forName(className, false, null);
				Field field = clazz.getDeclaredField(fieldName);
				return Type.getDescriptor(field.getType());
			} catch (ClassNotFoundException | NoSuchFieldException e) {
				return null;
			}
		}
	}
}
