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
 * Copyright (c) 2016-2021 FabricMC
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import dev.aoqia.leaf.loom.api.mappings.layered.MappingContext;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingLayer;
import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;
import dev.aoqia.leaf.loom.api.mappings.layered.spec.MappingsSpec;
import dev.aoqia.leaf.loom.configuration.providers.mappings.extras.annotations.AnnotationsData;
import dev.aoqia.leaf.loom.configuration.providers.mappings.extras.annotations.AnnotationsLayer;
import dev.aoqia.leaf.loom.configuration.providers.mappings.extras.signatures.SignatureFixesLayer;
import dev.aoqia.leaf.loom.configuration.providers.mappings.extras.unpick.UnpickLayer;
import dev.aoqia.leaf.loom.configuration.providers.mappings.unpick.UnpickMetadata;

public class LayeredMappingsProcessor {
	private final LayeredMappingSpec layeredMappingSpec;
	private final boolean noIntermediateMappings;

	public LayeredMappingsProcessor(LayeredMappingSpec spec, boolean noIntermediateMappings) {
		this.layeredMappingSpec = spec;
		this.noIntermediateMappings = noIntermediateMappings;
	}

	public List<MappingLayer> resolveLayers(MappingContext context) {
		List<MappingLayer> layers = new ArrayList<>();
		List<Class<? extends MappingLayer>> visitedLayers = new ArrayList<>();

		for (MappingsSpec<?> spec : layeredMappingSpec.layers()) {
			MappingLayer layer = spec.createLayer(context);

			for (Class<? extends MappingLayer> dependentLayer : layer.dependsOn()) {
				if (!visitedLayers.contains(dependentLayer)) {
					throw new RuntimeException("Layer %s depends on %s".formatted(layer.getClass().getName(), dependentLayer.getName()));
				}
			}

			layers.add(layer);
			visitedLayers.add(layer.getClass());
		}

		return Collections.unmodifiableList(layers);
	}

	public MemoryMappingTree getMappings(List<MappingLayer> layers) throws IOException {
		MemoryMappingTree mappingTree = new MemoryMappingTree();

		for (MappingLayer layer : layers) {
			// We have to rebuild a new tree to work on when a layer doesnt merge into layered
			boolean rebuild = layer.getSourceNamespace() != MappingsNamespace.NAMED;
			MemoryMappingTree workingTree;

			if (rebuild) {
				var tempTree = new MemoryMappingTree();

				// This can be null on the first layer
				if (mappingTree.getSrcNamespace() != null) {
					var sourceNsSwitch = new MappingSourceNsSwitch(tempTree, layer.getSourceNamespace().toString());
					mappingTree.accept(sourceNsSwitch);
				}

				workingTree = tempTree;
			} else {
				workingTree = mappingTree;
			}

			try {
				layer.visit(workingTree);
			} catch (IOException e) {
				throw new IOException("Failed to visit: " + layer.getClass(), e);
			}

			if (rebuild) {
				mappingTree = new MemoryMappingTree();
				workingTree.accept(new MappingSourceNsSwitch(mappingTree, MappingsNamespace.NAMED.toString()));
			}
		}

		return mappingTree;
	}

	public List<AnnotationsData> getAnnotationsData(List<MappingLayer> layers) throws IOException {
		List<AnnotationsData> result = new ArrayList<>();

		for (MappingLayer layer : layers) {
			if (layer instanceof AnnotationsLayer annotationsLayer) {
				AnnotationsData annotationsData = annotationsLayer.getAnnotationsData();

				if (annotationsData != null) {
					result.add(annotationsData);
				}
			}
		}

		return result;
	}

	@Nullable public Map<String, String> getSignatureFixes(List<MappingLayer> layers) {
		Map<String, String> signatureFixes = new HashMap<>();

		for (MappingLayer layer : layers) {
			if (layer instanceof SignatureFixesLayer signatureFixesLayer) {
				signatureFixes.putAll(signatureFixesLayer.getSignatureFixes());
			}
		}

		if (signatureFixes.isEmpty()) {
			return null;
		}

		return Collections.unmodifiableMap(signatureFixes);
	}

	public UnpickLayer.@Nullable UnpickData getUnpickData(List<MappingLayer> layers) throws IOException {
		List<UnpickLayer.UnpickData> unpickDataList = new ArrayList<>();

		for (MappingLayer layer : layers) {
			if (layer instanceof UnpickLayer unpickLayer) {
				UnpickLayer.UnpickData data = unpickLayer.getUnpickData();
				if (data == null) continue;

				if (!data.metadata().hasConstantsLocation()) {
					// if the constants location is not provided explicitly, Loom
					// cannot trick its way into finding them, since the mappings
					// dependency points to the layered mappings instead of the
					// individual layer that provided the unpick data
					String fallbackConstants = unpickLayer.getFallbackConstants();
					UnpickMetadata metadata = fallbackConstants == null
								? data.metadata().withoutConstants()
								: data.metadata().withConstants(fallbackConstants);

					data = new UnpickLayer.UnpickData(metadata, data.definitions());
				}

				unpickDataList.add(data);
			}
		}

		if (unpickDataList.isEmpty()) {
			return null;
		}

		if (unpickDataList.size() != 1) {
			// TODO merge
			throw new UnsupportedOperationException("Only one unpick layer is currently supported.");
		}

		return unpickDataList.get(0);
	}
}
