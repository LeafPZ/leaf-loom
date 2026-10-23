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
 * Copyright (c) 2026 FabricMC
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.util.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.jspecify.annotations.Nullable;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.LoomGradlePlugin;
import dev.aoqia.leaf.loom.api.decompilers.JavadocStyle;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.configuration.DependencyInfo;
import dev.aoqia.leaf.loom.configuration.providers.mappings.tiny.MappingsMerger;
import dev.aoqia.leaf.loom.configuration.providers.mappings.tiny.TinyJarInfo;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidProvider;
import dev.aoqia.leaf.loom.util.Checksum;
import dev.aoqia.leaf.loom.util.Constants;
import dev.aoqia.leaf.loom.util.DeletingFileVisitor;
import dev.aoqia.leaf.loom.util.ZipUtils;
import dev.aoqia.leaf.loom.util.service.ServiceFactory;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.stitch.Command;
import net.fabricmc.stitch.commands.CommandProposeFieldNames;

public final class RemapMappingConfiguration extends MappingConfiguration {
	private final ServiceFactory serviceFactory;
	private final Path baseTinyMappings;
	public final Path tinyMappings;
	public final Path tinyMappingsJar;
	@Nullable
	private Map<String, String> signatureFixes;

	private RemapMappingConfiguration(String mappingsIdentifier, Path mappingsWorkingDir, Path inputJar, ServiceFactory serviceFactory) {
		super(mappingsIdentifier, inputJar);
		this.serviceFactory = serviceFactory;
		this.baseTinyMappings = mappingsWorkingDir.resolve("mappings-base.tiny");
		this.tinyMappings = mappingsWorkingDir.resolve("mappings.tiny");
		this.tinyMappingsJar = mappingsWorkingDir.resolve("mappings.jar");
	}

	public static RemapMappingConfiguration create(Project project, ServiceFactory serviceFactory, DependencyInfo dependency, ZomboidProvider gameProvider) {
		final String version = dependency.getResolvedVersion();
		final Path inputJar = resolveInputJar(dependency, "mappings");
		final String mappingsName = StringUtils.removeSuffix(dependency.getDependency().getGroup() + "." + dependency.getDependency().getName(), "-unmerged");
		final TinyJarInfo jarInfo = readJarInfo(inputJar, dependency, gameProvider, "mappings");
		final String mappingsIdentifier = createMappingsIdentifier(mappingsName, version, getMappingsClassifier(dependency, jarInfo.v2()), gameProvider.gameVersion());
		final Path workingDir = gameProvider.dir(mappingsIdentifier).toPath();
		var mappingConfiguration = new RemapMappingConfiguration(mappingsIdentifier, workingDir, inputJar, serviceFactory);

		mappingConfiguration.setup(project, gameProvider, dependency, "mappings");
		return mappingConfiguration;
	}

	@Override
	protected void prepare(ZomboidProvider gameProvider) throws IOException {
		if (gameProvider.refreshDeps()) {
			cleanup();
		}

		Files.createDirectories(tinyMappings.getParent());
	}

	@Override
	protected void cleanup() throws IOException {
		if (Files.exists(tinyMappings.getParent())) {
			Files.walkFileTree(tinyMappings.getParent(), new DeletingFileVisitor());
		}
	}

	@Override
	protected void setupMappings(Project project, ZomboidProvider gameProvider, FileSystem inputJar) throws IOException {
		extractSignatureFixes(inputJar);

		if (Files.notExists(tinyMappings) || gameProvider.refreshDeps()) {
			TinyJarInfo.extractMappings(inputJar, baseTinyMappings);
			storeMappings(project, gameProvider);
		}

		if (Files.notExists(tinyMappingsJar) || gameProvider.refreshDeps()) {
			Files.deleteIfExists(tinyMappingsJar);
			ZipUtils.add(tinyMappingsJar, TinyJarInfo.MAPPINGS_PATH, Files.readAllBytes(tinyMappings));
		}
	}

	private void storeMappings(Project project, ZomboidProvider gameProvider) throws IOException {
		if (areMappingsV2(baseTinyMappings)) {
			final LoomGradleExtension extension = LoomGradleExtension.get(project);

			if (extension.getUseIntermediateMappings().get()) {
				IntermediateMappingsService intermediateMappingsService = serviceFactory.get(IntermediateMappingsService.createOptions(project, gameProvider));
				MappingsMerger.mergeAndSaveMappings(baseTinyMappings, tinyMappings, gameProvider, intermediateMappingsService);
			} else {
				Files.copy(baseTinyMappings, tinyMappings, StandardCopyOption.REPLACE_EXISTING);
			}
		} else {
			final List<Path> gameJars = gameProvider.getGameJars();

			if (gameJars.size() != 1) {
				throw new UnsupportedOperationException("V1 mappings only support single jar zomboid providers");
			}

			Files.deleteIfExists(tinyMappings);
			LOGGER.info(":populating field names");
			suggestFieldNames(gameJars.getFirst(), baseTinyMappings, tinyMappings);
		}
	}

	private static void validateMappings(MemoryMappingTree mappingTree) throws IOException {
		if (!mappingTree.getMetadata(MARKDOWN_METADATA_KEY).isEmpty()) {
			LOGGER.warn("Markdown comments are currently not supported for remapped Zomboid. Reinterpreting comments as HTML.");
		}
	}

	@Override
	public TinyMappingsService getMappingsService(Project project, ServiceFactory serviceFactory) {
		TinyMappingsService mappingsService = super.getMappingsService(project, serviceFactory);

		try {
			validateMappings(mappingsService.getMappingTree());
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to validate mappings", e);
		}

		return mappingsService;
	}

	@Override
	public Provider<TinyMappingsService.Options> getMappingsServiceOptions(Project project) {
		return TinyMappingsService.createOptions(project, tinyMappings);
	}

	@Override
	public String getMappingsHash() {
		return Checksum.of(tinyMappings).sha256().hex();
	}

	@Override
	public MappingsNamespace getRuntimeNamespace() {
		return MappingsNamespace.NAMED;
	}

	@Override
	public JavadocStyle getJavadocStyle() {
		return JavadocStyle.HTML;
	}

	@Override
	public void applyToProject(Project project, DependencyInfo dependency) {
		super.applyToProject(project, dependency);
		project.getDependencies().add(Constants.Configurations.MAPPINGS_FINAL, project.files(tinyMappingsJar.toFile()));
	}

	private void extractSignatureFixes(FileSystem inputJar) throws IOException {
		Path recordSignaturesJsonPath = inputJar.getPath("extras/record_signatures.json");

		if (!Files.exists(recordSignaturesJsonPath)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(recordSignaturesJsonPath, StandardCharsets.UTF_8)) {
			//noinspection unchecked
			signatureFixes = LoomGradlePlugin.GSON.fromJson(reader, Map.class);
		}
	}

	@Nullable
	public Map<String, String> getSignatureFixes() {
		return signatureFixes;
	}

	private static boolean areMappingsV2(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return MappingReader.detectFormat(reader) == MappingFormat.TINY_2_FILE;
		}
	}

	private static void suggestFieldNames(Path inputJar, Path oldMappings, Path newMappings) {
		Command command = new CommandProposeFieldNames();

		try {
			command.run(new String[] {
					inputJar.toFile().getAbsolutePath(),
					oldMappings.toAbsolutePath().toString(),
					newMappings.toAbsolutePath().toString()
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
