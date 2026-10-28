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

package dev.aoqia.leaf.loom.configuration.providers.mappings;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.mappingio.adapter.MappingDstNsReorder;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import dev.aoqia.leaf.loom.LoomGradleExtension;
import dev.aoqia.leaf.loom.LoomGradlePlugin;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingContext;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingLayer;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.configuration.ConfigContext;
import dev.aoqia.leaf.loom.configuration.mods.dependency.LocalMavenHelper;
import dev.aoqia.leaf.loom.configuration.providers.mappings.extras.annotations.AnnotationsData;
import dev.aoqia.leaf.loom.configuration.providers.mappings.extras.annotations.AnnotationsLayer;
import dev.aoqia.leaf.loom.configuration.providers.mappings.extras.unpick.UnpickLayer;
import dev.aoqia.leaf.loom.configuration.providers.mappings.unpick.UnpickMetadata;
import dev.aoqia.leaf.loom.configuration.providers.mappings.utils.AddConstructorMappingVisitor;
import dev.aoqia.leaf.loom.util.ZipUtils;

public record LayeredMappingsFactory(LayeredMappingSpec spec) {
    private static final String GROUP = "loom";
    private static final String MODULE = "mappings";
    private static final Logger LOGGER = LoggerFactory.getLogger(LayeredMappingsFactory.class);

    /*
     * As we no longer have SelfResolvingDependency we now always create the mappings file after evaluation.
     * This works in a similar way to how remapped mods are handled.
     */
    public static void afterEvaluate(ConfigContext configContext) {
        for (LayeredMappingsFactory layeredMappingFactory : configContext.extension().getLayeredMappingFactories()) {
            try {
                layeredMappingFactory.evaluate(configContext);
            } catch (IOException e) {
                throw new UncheckedIOException(
                    "Failed to setup layered mappings: %s".formatted(layeredMappingFactory.mavenNotation()), e);
            }
        }
    }

    private void evaluate(ConfigContext configContext) throws IOException {
        LOGGER.info("Evaluating layer mapping: {}", mavenNotation());

        final Path mavenRepoDir = configContext.extension().getFiles().getGlobalZomboidRepo().toPath();
        final LocalMavenHelper maven = new LocalMavenHelper(GROUP, MODULE, spec().getVersion(), null, mavenRepoDir);
        final Path jar = resolve(configContext.project());
        maven.copyToMaven(jar, null);
    }

    public Path resolve(Project project) throws IOException {
        final LoomGradleExtension extension = LoomGradleExtension.get(project);
        final MappingContext mappingContext = new GradleMappingContext(project,
            spec.getVersion().replace("+", "_").replace(".", "_"));
        final Path mappingsDir = mappingContext.zomboidProvider().dir("layered").toPath();
        final Path mappingsZip = mappingsDir.resolve(String.format("%s.%s-%s.jar", GROUP, MODULE, spec.getVersion()));

        if (Files.exists(mappingsZip) && !mappingContext.refreshDeps()) {
            return mappingsZip;
        }

        boolean useIntermediateMappings = extension.getUseIntermediateMappings().get();
        var processor = new LayeredMappingsProcessor(spec, !useIntermediateMappings);
        List<MappingLayer> layers = processor.resolveLayers(mappingContext);

        Files.deleteIfExists(mappingsZip);

        writeMapping(processor, layers, mappingsZip, useIntermediateMappings);
        writeAnnotationData(processor, layers, mappingsZip);
        writeSignatureFixes(processor, layers, mappingsZip);
        writeUnpickData(processor, layers, mappingsZip);

        return mappingsZip;
    }

    public Dependency createDependency(Project project) {
        return project.getDependencies().create(mavenNotation());
    }

    public String mavenNotation() {
        return String.format("%s:%s:%s", GROUP, MODULE, spec.getVersion());
    }

    private void writeMapping(LayeredMappingsProcessor processor, List<MappingLayer> layers, Path mappingsFile,
        boolean useIntermediateMappings) throws IOException
    {
        MemoryMappingTree mappings = processor.getMappings(layers);

        try (Writer writer = new StringWriter()) {
            var tiny2Writer = new Tiny2FileWriter(writer, false);

            MappingDstNsReorder nsReorder = new MappingDstNsReorder(tiny2Writer,
                useIntermediateMappings
                    ? List.of(MappingsNamespace.NAMED.toString(), MappingsNamespace.OFFICIAL.toString())
                    : List.of(MappingsNamespace.NAMED.toString()));
            MappingSourceNsSwitch nsSwitch = new MappingSourceNsSwitch(nsReorder, useIntermediateMappings
                ? MappingsNamespace.INTERMEDIARY.toString() : MappingsNamespace.OFFICIAL.toString(), true);
            AddConstructorMappingVisitor addConstructor = new AddConstructorMappingVisitor(nsSwitch);
            mappings.accept(addConstructor);

            Files.deleteIfExists(mappingsFile);
            ZipUtils.add(mappingsFile, "mappings/mappings.tiny", writer.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private void writeAnnotationData(LayeredMappingsProcessor processor, List<MappingLayer> layers, Path mappingsFile)
        throws IOException
    {
        List<AnnotationsData> annotationsData = processor.getAnnotationsData(layers);

        if (annotationsData.isEmpty()) {
            return;
        }

        byte[] data = AnnotationsData.GSON.toJson(AnnotationsData.listToJson(annotationsData))
            .getBytes(StandardCharsets.UTF_8);

        ZipUtils.add(mappingsFile, AnnotationsLayer.ANNOTATIONS_PATH, data);
    }

    private void writeSignatureFixes(LayeredMappingsProcessor processor, List<MappingLayer> layers, Path mappingsFile)
        throws IOException
    {
        Map<String, String> signatureFixes = processor.getSignatureFixes(layers);

        if (signatureFixes == null) {
            return;
        }

        byte[] data = LoomGradlePlugin.GSON.toJson(signatureFixes).getBytes(StandardCharsets.UTF_8);

        ZipUtils.add(mappingsFile, "extras/record_signatures.json", data);
    }

    private void writeUnpickData(LayeredMappingsProcessor processor, List<MappingLayer> layers, Path mappingsFile)
        throws IOException
    {
        UnpickLayer.UnpickData unpickData = processor.getUnpickData(layers);

        if (unpickData == null) {
            return;
        }

        byte[] data = UnpickMetadata.toJson(unpickData.metadata()).getBytes(StandardCharsets.UTF_8);

        ZipUtils.add(mappingsFile, UnpickMetadata.UNPICK_DEFINITIONS_PATH, unpickData.definitions());
        ZipUtils.add(mappingsFile, UnpickMetadata.UNPICK_METADATA_PATH, data);
    }
}
