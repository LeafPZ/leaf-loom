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

package dev.aoqia.leaf.loom.configuration;

import javax.inject.Inject;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaPlugin;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.util.Constants;
import dev.aoqia.leaf.loom.util.LoomVersions;
import dev.aoqia.leaf.loom.util.gradle.SourceSetHelper;

public abstract class LoomConfigurations implements Runnable {
	@Inject
	protected abstract Project getProject();

	@Inject
	protected abstract ConfigurationContainer getConfigurations();

	@Inject
	protected abstract DependencyHandler getDependencies();

	@Override
	public void run() {
		final LoomGradleExtension extension = LoomGradleExtension.get(getProject());

		register(Constants.Configurations.MOD_COMPILE_CLASSPATH, Role.RESOLVABLE);
		registerNonTransitive(Constants.Configurations.MOD_COMPILE_CLASSPATH_MAPPED, Role.RESOLVABLE);

		// Set up the compile configurations.
		var clientCompile = registerNonTransitive(Constants.Configurations.ZOMBOID_CLIENT_COMPILE_LIBRARIES, Role.RESOLVABLE);
		var serverCompile = registerNonTransitive(Constants.Configurations.ZOMBOID_SERVER_COMPILE_LIBRARIES, Role.RESOLVABLE);
		var zomboidCompile = registerNonTransitive(Constants.Configurations.ZOMBOID_COMPILE_LIBRARIES, Role.RESOLVABLE);
		zomboidCompile.configure(configuration -> {
			configuration.extendsFrom(clientCompile);
			configuration.extendsFrom(serverCompile);
		});

		// Set up the runtime configurations, this extends from the compile configurations.
		var zomboidClientRuntime = registerNonTransitive(Constants.Configurations.ZOMBOID_CLIENT_RUNTIME_LIBRARIES, Role.RESOLVABLE);
		var zomboidServerRuntime = registerNonTransitive(Constants.Configurations.ZOMBOID_SERVER_RUNTIME_LIBRARIES, Role.RESOLVABLE);

		// Runtime extends from compile
		zomboidClientRuntime.configure(configuration -> configuration.extendsFrom(clientCompile));
		zomboidServerRuntime.configure(configuration -> configuration.extendsFrom(serverCompile));

		registerNonTransitive(Constants.Configurations.ZOMBOID_RUNTIME_LIBRARIES, Role.RESOLVABLE).configure(runtime -> {
			runtime.extendsFrom(zomboidClientRuntime);
			runtime.extendsFrom(zomboidServerRuntime);
		});

		registerNonTransitive(Constants.Configurations.ZOMBOID_NATIVES, Role.RESOLVABLE);
		registerNonTransitive(Constants.Configurations.LOADER_DEPENDENCIES, Role.RESOLVABLE);

		registerNonTransitive(Constants.Configurations.ZOMBOID, Role.NONE);

		register(Constants.Configurations.INCLUDE, Role.NONE);
		registerNonTransitive(Constants.Configurations.MAPPING_CONSTANTS, Role.RESOLVABLE);
		extendsFrom(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, Constants.Configurations.MAPPING_CONSTANTS);

		if (!extension.disableObfuscation()) {
			register(Constants.Configurations.NAMED_ELEMENTS, Role.CONSUMABLE).configure(configuration -> {
				configuration.extendsFrom(getConfigurations().named(JavaPlugin.API_CONFIGURATION_NAME));
			});

			register(Constants.Configurations.MAPPINGS, Role.RESOLVABLE);
			register(Constants.Configurations.MAPPINGS_FINAL, Role.RESOLVABLE);
			extendsFrom(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.MAPPINGS_FINAL);
			extendsFrom(JavaPlugin.TEST_RUNTIME_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.MAPPINGS_FINAL);

			extension.createRemapConfigurations(SourceSetHelper.getMainSourceSet(getProject()));
		} else {
			registerNonTransitive(Constants.Configurations.ANNOTATIONS, Role.RESOLVABLE);
		}

		register(Constants.Configurations.LOOM_DEVELOPMENT_DEPENDENCIES, Role.RESOLVABLE);
		register(Constants.Configurations.LOCAL_RUNTIME, Role.RESOLVABLE);
		extendsFrom(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.LOCAL_RUNTIME);

		extendsFrom(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME, Constants.Configurations.ZOMBOID_RUNTIME_LIBRARIES);

		// Add the dev time dependencies
		getDependencies().add(Constants.Configurations.LOOM_DEVELOPMENT_DEPENDENCIES, LoomVersions.DEV_LAUNCH_INJECTOR.mavenNotation());
		getDependencies().add(Constants.Configurations.LOOM_DEVELOPMENT_DEPENDENCIES, LoomVersions.FABRIC_LOG4J_UTIL.mavenNotation());
		getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, LoomVersions.JETBRAINS_ANNOTATIONS.mavenNotation());
		getDependencies().add(JavaPlugin.TEST_COMPILE_ONLY_CONFIGURATION_NAME, LoomVersions.JETBRAINS_ANNOTATIONS.mavenNotation());

		register(Constants.Configurations.ZOMBOID_TEST_CLIENT_RUNTIME_LIBRARIES, Role.RESOLVABLE);
		extendsFrom(Constants.Configurations.ZOMBOID_TEST_CLIENT_RUNTIME_LIBRARIES, Constants.Configurations.ZOMBOID_NATIVES);
		extendsFrom(Constants.Configurations.ZOMBOID_TEST_CLIENT_RUNTIME_LIBRARIES, Constants.Configurations.ZOMBOID_CLIENT_RUNTIME_LIBRARIES);
		extendsFrom(Constants.Configurations.ZOMBOID_TEST_CLIENT_RUNTIME_LIBRARIES, Constants.Configurations.LOADER_DEPENDENCIES);

		register(Constants.Configurations.PRODUCTION_RUNTIME_MODS, Role.RESOLVABLE);
	}

	private NamedDomainObjectProvider<Configuration> register(String name, Role role) {
		return getConfigurations().register(name, role::apply);
	}

	private NamedDomainObjectProvider<Configuration> registerNonTransitive(String name, Role role) {
		final NamedDomainObjectProvider<Configuration> provider = register(name, role);
		provider.configure(configuration -> configuration.setTransitive(false));
		return provider;
	}

	public void extendsFrom(String a, String b) {
		getConfigurations().named(a, configuration -> configuration.extendsFrom(getConfigurations().named(b)));
	}

	public enum Role {
		NONE(false, false),
		CONSUMABLE(true, false),
		RESOLVABLE(false, true);

		private final boolean canBeConsumed;
		private final boolean canBeResolved;

		Role(boolean canBeConsumed, boolean canBeResolved) {
			this.canBeConsumed = canBeConsumed;
			this.canBeResolved = canBeResolved;
		}

		public void apply(Configuration configuration) {
			configuration.setCanBeConsumed(canBeConsumed);
			configuration.setCanBeResolved(canBeResolved);
		}
	}
}
