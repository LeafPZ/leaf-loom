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
 * Copyright (c) 2018-2023 FabricMC
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

package dev.aoqia.leaf.loom.decompilers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import javax.inject.Inject;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.api.decompilers.DecompilationMetadata;
import dev.aoqia.leaf.loom.api.decompilers.JavadocStyle;
import dev.aoqia.leaf.loom.api.decompilers.LoomDecompiler;
import dev.aoqia.leaf.loom.decompilers.cfr.LoomCFRDecompiler;
import dev.aoqia.leaf.loom.decompilers.vineflower.VineflowerDecompiler;
import dev.aoqia.leaf.loom.util.LoomVersions;
import dev.aoqia.leaf.loom.util.ZipUtils;

public abstract class DecompilerConfiguration implements Runnable {
	@Inject
	protected abstract Project getProject();

	@Override
	public void run() {
		var cfrConfiguration = createConfiguration("cfr", LoomVersions.CFR);
		var vineflowerConfiguration = createConfiguration("vineflower", LoomVersions.VINEFLOWER);

		registerDecompiler(getProject(), "cfr", BuiltinCfr.class, cfrConfiguration);
		registerDecompiler(getProject(), "vineflower", BuiltinVineflower.class, vineflowerConfiguration);
	}

	private NamedDomainObjectProvider<Configuration> createConfiguration(String name, LoomVersions version) {
		final String configurationName = name + "DecompilerClasspath";
		NamedDomainObjectProvider<Configuration> configuration = getProject().getConfigurations().register(configurationName);
		getProject().getDependencies().add(configurationName, version.mavenNotation());
		return configuration;
	}

	private void registerDecompiler(Project project, String name, Class<? extends LoomDecompiler> decompilerClass, NamedDomainObjectProvider<Configuration> configuration) {
		LoomGradleExtension.get(project).getDecompilerOptions().register(name, options -> {
			options.getDecompilerClassName().set(decompilerClass.getName());
			options.getClasspath().from(configuration);
		});
	}

	// We need to wrap the internal API with the public API.
	// This is needed as the sourceset containing fabric's decompilers do not have access to loom classes.
	private abstract static sealed class BuiltinDecompiler implements LoomDecompiler permits BuiltinCfr, BuiltinVineflower {
		private final LoomInternalDecompiler internalDecompiler;

		BuiltinDecompiler(LoomInternalDecompiler internalDecompiler) {
			this.internalDecompiler = internalDecompiler;
		}

		@Override
		public void decompile(Path compiledJar, Path sourcesDestination, Path linemapDestination, DecompilationMetadata metaData) {
			final Logger slf4jLogger = LoggerFactory.getLogger(internalDecompiler.getClass());

			final var logger = new LoomInternalDecompiler.Logger() {
				@Override
				public void accept(String data) throws IOException {
					metaData.logger().accept(data);
				}

				@Override
				public void error(String msg) {
					slf4jLogger.error(msg);
				}
			};

			internalDecompiler.decompile(new LoomInternalDecompiler.Context() {
				@Override
				public Path compiledJar() {
					return compiledJar;
				}

				@Override
				public Path sourcesDestination() {
					return sourcesDestination;
				}

				@Override
				public Path linemapDestination() {
					return linemapDestination;
				}

				@Override
				public int numberOfThreads() {
					return metaData.numberOfThreads();
				}

				@Override
				public Path javaDocs() {
					return metaData.javaDocs();
				}

				@Override
				public Collection<Path> libraries() {
					return metaData.libraries();
				}

				@Override
				public LoomInternalDecompiler.Logger logger() {
					return logger;
				}

				@Override
				public Map<String, String> options() {
					return metaData.options();
				}

				@Override
				public byte[] unpackZip(Path zip, String path) throws IOException {
					return ZipUtils.unpack(zip, path);
				}

				@Override
				public String runtimeNamespace() {
					return metaData.runtimeNamespace();
				}

				@Override
				public JavadocStyle javadocStyle() {
					return metaData.javadocStyle();
				}
			});
		}
	}

	public static final class BuiltinCfr extends BuiltinDecompiler {
		public BuiltinCfr() {
			super(new LoomCFRDecompiler());
		}
	}

	public static final class BuiltinVineflower extends BuiltinDecompiler {
		public BuiltinVineflower() {
			super(new VineflowerDecompiler());
		}
	}
}
