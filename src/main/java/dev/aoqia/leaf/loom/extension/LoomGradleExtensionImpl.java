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
 * Copyright (c) 2021-2025 FabricMC
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

package dev.aoqia.leaf.loom.extension;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.configuration.BuildFeatures;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.jspecify.annotations.Nullable;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.LoomRemapGradlePlugin;
import dev.aoqia.leaf.loom.api.mappings.intermediate.IntermediateMappingsProvider;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.configuration.InstallerData;
import dev.aoqia.leaf.loom.configuration.LoomDependencyManager;
import dev.aoqia.leaf.loom.configuration.accesswidener.AccessWidenerFile;
import dev.aoqia.leaf.loom.configuration.mods.ArtifactMetadata;
import dev.aoqia.leaf.loom.configuration.providers.mappings.IntermediaryMappingsProvider;
import dev.aoqia.leaf.loom.configuration.providers.mappings.LayeredMappingsFactory;
import dev.aoqia.leaf.loom.configuration.providers.mappings.MappingConfiguration;
import dev.aoqia.leaf.loom.configuration.providers.mappings.RemapMappingConfiguration;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidMetadataProvider;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidProvider;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.library.LibraryProcessorManager;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.mapped.IntermediaryZomboidProvider;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.mapped.NamedZomboidProvider;
import dev.aoqia.leaf.loom.util.Constants;
import dev.aoqia.leaf.loom.util.copygamefile.CopyGameFile;
import dev.aoqia.leaf.loom.util.copygamefile.CopyGameFileBuilder;
import dev.aoqia.leaf.loom.util.download.Download;
import dev.aoqia.leaf.loom.util.download.DownloadBuilder;
import dev.aoqia.leaf.loom.util.gradle.GradleUtils;

public abstract class LoomGradleExtensionImpl extends LoomGradleExtensionApiImpl implements LoomGradleExtension {
    private final boolean configurationCacheActive;
    private LoomDependencyManager dependencyManager;
    private final Property<Boolean> disableObfuscation;
    private final Property<Boolean> dontRemap;

    private InstallerData installerData;

    private IntermediaryZomboidProvider<?> intermediaryZomboidProvider;
    private final boolean isCollectingDependencyVerificationMetadata;
    private final boolean isolatedProjectsActive;
    private final ListProperty<LibraryProcessorManager.LibraryProcessorFactory> libraryProcessorFactories;
    private final LoomFiles loomFiles;
    private MappingConfiguration mappingConfiguration;
    private ZomboidMetadataProvider metadataProvider;
    private final MixinExtension mixinApExtension;
    private NamedZomboidProvider<?> namedZomboidProvider;
    private final Project project;
    private boolean refreshDeps;
    private final List<AccessWidenerFile> transitiveAccessWideners = new ArrayList<>();
    private final ConfigurableFileCollection unmappedMods;
    private ZomboidProvider zomboidProvider;

    @Inject
    public LoomGradleExtensionImpl(Project project, LoomFiles files) {
        super(project, files);
        this.project = project;
        // Initiate with newInstance to allow gradle to decorate our extension
        this.mixinApExtension = project.getObjects().newInstance(MixinExtensionImpl.class, project);
        this.loomFiles = files;
        this.unmappedMods = project.files();

        // Setup the default intermediate mappings provider.
        setIntermediateMappingsProvider(IntermediaryMappingsProvider.class, provider -> {
            provider.getIntermediaryUrl().convention(getIntermediaryUrl()).finalizeValueOnRead();

            provider.getRefreshDeps().set(project.provider(() -> LoomGradleExtension.get(project).refreshDeps()));
        });

        refreshDeps = manualRefreshDeps();
        libraryProcessorFactories = project.getObjects()
            .listProperty(LibraryProcessorManager.LibraryProcessorFactory.class);
        libraryProcessorFactories.addAll(LibraryProcessorManager.DEFAULT_LIBRARY_PROCESSORS);
        libraryProcessorFactories.finalizeValueOnRead();

        configurationCacheActive = getBuildFeatures().getConfigurationCache().getActive().get();
        isolatedProjectsActive = getBuildFeatures().getIsolatedProjects().getActive().get();
        isCollectingDependencyVerificationMetadata = !project.getGradle()
            .getStartParameter()
            .getWriteDependencyVerifications()
            .isEmpty();
        disableObfuscation = project.getObjects().property(Boolean.class);
        dontRemap = project.getObjects().property(Boolean.class);

        // TODO:
        if (!project.getPluginManager().hasPlugin(LoomRemapGradlePlugin.NAME)) {
            disableObfuscation.set(true);
            disableObfuscation.finalizeValue();
        } else {
            disableObfuscation.set(project.provider(
                () -> GradleUtils.getBooleanProperty(getProject(), Constants.Properties.DISABLE_OBFUSCATION)));
            disableObfuscation.finalizeValueOnRead();
        }

        dontRemap.set(disableObfuscation.map(notObfuscated -> notObfuscated
            || GradleUtils.getBooleanProperty(getProject(), Constants.Properties.DONT_REMAP)));
        dontRemap.finalizeValueOnRead();

        if (refreshDeps) {
            project.getLogger().lifecycle("Refresh dependencies is in use, loom will be significantly slower.");
        }
    }

    @Override
    public void addTransitiveAccessWideners(List<AccessWidenerFile> accessWidenerFiles) {
        transitiveAccessWideners.addAll(accessWidenerFiles);
    }

    @Override
    public CopyGameFileBuilder copyGameFile(String url) {
        CopyGameFileBuilder builder = CopyGameFile.create(Path.of(url));

        if (manualRefreshDeps()) {
            builder.forced();
        }

        return builder;
    }

    @Override
    public boolean disableObfuscation() {
        return disableObfuscation.get();
    }

    @Override
    public boolean dontRemapOutputs() {
        return dontRemap.get();
    }

    @Override
    public DownloadBuilder download(String url) {
        DownloadBuilder builder;

        try {
            builder = Download.create(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to create downloader for: " + e);
        }

        if (project.getGradle().getStartParameter().isOffline()) {
            builder.offline();
        }

        if (manualRefreshDeps()) {
            builder.forceDownload();
        }

        return builder;
    }

    @Override
    public Provider<ArtifactMetadata.MixinRemapType> getDefaultMixinRemapTypeEnum() {
        return getDefaultMixinRemapType().map(ArtifactMetadata.MixinRemapType::valueOf);
    }

    @Override
    public LoomFiles getFiles() {
        return loomFiles;
    }

    @Override
    public InstallerData getInstallerData() {
        return installerData;
    }

    @Override
    public IntermediaryZomboidProvider<?> getIntermediaryZomboidProvider() {
        return Objects.requireNonNull(intermediaryZomboidProvider,
            "Cannot get IntermediaryZomboidProvider before it has been setup");
    }

    @Override
    public Collection<LayeredMappingsFactory> getLayeredMappingFactories() {
        if (disableObfuscation()) {
            throw new UnsupportedOperationException(
                "Cannot get layered mapping factories in a non-obfuscated environment");
        }

        hasEvaluatedLayeredMappings = true;
        return Collections.unmodifiableCollection(layeredMappingsDependencyMap.values());
    }

    @Override
    public ListProperty<LibraryProcessorManager.LibraryProcessorFactory> getLibraryProcessors() {
        return libraryProcessorFactories;
    }

    @Override
    public RemapMappingConfiguration getMappingConfiguration() {
        if (disableObfuscation()) {
            project.getLogger().lifecycle("help", new RuntimeException());
            throw new UnsupportedOperationException(
                "Cannot get mappings configuration in a non-obfuscated environment");
        }

        return (RemapMappingConfiguration) Objects.requireNonNull(mappingConfiguration,
            "Cannot get MappingsProvider before it has been setup");
    }

    @Override
    public @Nullable MappingConfiguration getMappingConfigurationOrNull() {
        return mappingConfiguration;
    }

    @Override
    public ZomboidMetadataProvider getMetadataProvider() {
        return Objects.requireNonNull(metadataProvider, "Cannot get ZomboidMetadataProvider before it has been setup");
    }

    @Override
    public MixinExtension getMixin() {
        return this.mixinApExtension;
    }

    @Override
    public NamedZomboidProvider<?> getNamedZomboidProvider() {
        return Objects.requireNonNull(namedZomboidProvider, "Cannot get NamedZomboidProvider before it has been setup");
    }

    @Override
    public Provider<MappingsNamespace> getProductionNamespaceEnum() {
        return getProductionNamespace()
            .map(s -> Objects.requireNonNull(MappingsNamespace.of(s), "Invalid production namespace"));
    }

    @Override
    public ListProperty<RemapperExtensionHolder> getRemapperExtensions() {
        return remapperExtensions;
    }

    @Override
    public List<AccessWidenerFile> getTransitiveAccessWideners() {
        return transitiveAccessWideners;
    }

    @Override
    public ConfigurableFileCollection getUnmappedModCollection() {
        return unmappedMods;
    }

    @Override
    public FileCollection getZomboidJarsCollection(MappingsNamespace mappingsNamespace) {
        return getProject().files(getProject()
            .provider(() -> getProject().files(getZomboidJars(mappingsNamespace).stream().map(Path::toFile).toList())));
    }

    @Override
    public ZomboidProvider getZomboidProvider() {
        return Objects.requireNonNull(zomboidProvider, "Cannot get ZomboidProvider before it has been setup");
    }

    @Override
    public boolean isCollectingDependencyVerificationMetadata() {
        return isCollectingDependencyVerificationMetadata;
    }

    @Override
    public boolean isConfigurationCacheActive() {
        return configurationCacheActive;
    }

    @Override
    public boolean isProjectIsolationActive() {
        return isolatedProjectsActive;
    }

    @Override
    public void noIntermediateMappings() {
        getUseIntermediateMappings().set(false);
    }

    @Override
    public boolean refreshDeps() {
        return refreshDeps;
    }

    public void setInstallerData(InstallerData object) {
        this.installerData = object;
    }

    @Override
    public void setIntermediaryZomboidProvider(IntermediaryZomboidProvider<?> provider) {
        this.intermediaryZomboidProvider = provider;
    }

    @Override
    public void setMappingConfiguration(MappingConfiguration mappingConfiguration) {
        this.mappingConfiguration = mappingConfiguration;
    }

    @Override
    public void setMetadataProvider(ZomboidMetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    @Override
    public void setNamedZomboidProvider(NamedZomboidProvider<?> provider) {
        this.namedZomboidProvider = provider;
    }

    @Override
    public void setRefreshDeps(boolean refreshDeps) {
        this.refreshDeps = refreshDeps;
    }

    @Override
    public void setZomboidProvider(ZomboidProvider provider) {
        this.zomboidProvider = provider;
    }

    @Override
    protected <T extends IntermediateMappingsProvider> void configureIntermediateMappingsProviderInternal(T provider) {
        provider.getGameVersion().set(getProject().provider(() -> getZomboidProvider().gameVersion()));
        provider.getGameVersion().disallowChanges();

        provider.getDownloader().set(this::download);
        provider.getDownloader().disallowChanges();

        provider.getUseSplitOfficialNamespaces().set(false);
        provider.getUseSplitOfficialNamespaces().disallowChanges();
    }

    @Inject
    protected abstract BuildFeatures getBuildFeatures();

    @Override
    protected Project getProject() {
        return project;
    }

    private boolean manualRefreshDeps() {
        return project.getGradle().getStartParameter().isRefreshDependencies() || Boolean.getBoolean("loom.refresh");
    }
}
