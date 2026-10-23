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

package dev.aoqia.leaf.loom.configuration.providers.mappings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gradle.api.Action;

import dev.aoqia.leaf.loom.api.mappings.layered.spec.FileMappingsSpecBuilder;
import dev.aoqia.leaf.loom.api.mappings.layered.spec.FileSpec;
import dev.aoqia.leaf.loom.api.mappings.layered.spec.LayeredMappingSpecBuilder;
import dev.aoqia.leaf.loom.api.mappings.layered.spec.MappingsSpec;
import dev.aoqia.leaf.loom.configuration.providers.mappings.extras.signatures.SignatureFixesSpec;
import dev.aoqia.leaf.loom.configuration.providers.mappings.file.FileMappingsSpecBuilderImpl;
import dev.aoqia.leaf.loom.configuration.providers.mappings.intermediary.IntermediaryMappingsSpec;

public class LayeredMappingSpecBuilderImpl implements LayeredMappingSpecBuilder {
	private final List<MappingsSpec<?>> layers = new ArrayList<>();

	@Override
	public LayeredMappingSpecBuilder addLayer(MappingsSpec<?> mappingSpec) {
		layers.add(mappingSpec);
		return this;
	}

	@Override
	public LayeredMappingSpecBuilder signatureFix(Object object) {
		return addLayer(new SignatureFixesSpec(FileSpec.create(object)));
	}

	@Override
	public LayeredMappingSpecBuilder mappings(Object file, Action<? super FileMappingsSpecBuilder> action) {
		FileMappingsSpecBuilderImpl builder = FileMappingsSpecBuilderImpl.builder(FileSpec.create(file));
		action.execute(builder);
		return addLayer(builder.build());
	}

	public LayeredMappingSpec build() {
		List<MappingsSpec<?>> builtLayers = new ArrayList<>(layers.size() + 1);
		// Intermediary is always the base layer
		builtLayers.add(new IntermediaryMappingsSpec());
		builtLayers.addAll(layers);

		return new LayeredMappingSpec(Collections.unmodifiableList(builtLayers));
	}
}
