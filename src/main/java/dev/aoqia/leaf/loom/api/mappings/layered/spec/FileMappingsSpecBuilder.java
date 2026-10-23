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
 * Copyright (c) 2021-2022 FabricMC
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

package dev.aoqia.leaf.loom.api.mappings.layered.spec;

import org.jetbrains.annotations.ApiStatus;

import dev.aoqia.leaf.loom.api.mappings.layered.MappingsNamespace;

/**
 * A builder for a file mappings layer.
 * This layer type supports any mapping type that mapping-io can read.
 */
@ApiStatus.Experimental
public interface FileMappingsSpecBuilder {
	/**
	 * Sets the mapping path inside a zip or jar.
	 * This will have no effect if the file of this mapping spec
	 * is not a zip.
	 *
	 * <p>Path components within the path should be separated with {@code /}.
	 *
	 * <p>The default mapping path is {@code mappings/mappings.tiny}, matching regular mapping dependency jars
	 * such as Yarn's.
	 *
	 * @param mappingPath the mapping path
	 * @return this builder
	 */
	FileMappingsSpecBuilder mappingPath(String mappingPath);

	/**
	 * Sets the fallback namespaces. They will be used
	 * if the mapping format itself doesn't provide namespaces with names
	 * (e.g. Enigma mappings).
	 *
	 * <p>The default fallback namespaces are {@code intermediary} as the source namespace
	 * and {@code named} as the target namespace as in Yarn.
	 *
	 * @param sourceNamespace the fallback source namespace
	 * @param targetNamespace the fallback target namespace
	 * @return this builder
	 */
	FileMappingsSpecBuilder fallbackNamespaces(String sourceNamespace, String targetNamespace);

	/**
	 * Marks that the file contains Enigma mappings.
	 * Because they are stored in a directory, the format cannot be auto-detected.
	 *
	 * @return this builder
	 */
	FileMappingsSpecBuilder enigmaMappings();

	/**
	 * Marks that the zip file contains annotation data.
	 *
	 * @return this builder
	 */
	FileMappingsSpecBuilder containsAnnotations();

	/**
	 * Marks that the zip file contains unpick data.
	 *
	 * @return this builder
	 */
	FileMappingsSpecBuilder containsUnpick();

	/**
	 * Sets the merge namespace of this mappings spec.
	 *
	 * <p>The merge namespace is the namespace that is used to match up this layer's
	 * names to the rest of the mappings. For example, Yarn mappings should be merged through
	 * the intermediary names.
	 *
	 * <p>The default merge namespace is {@link MappingsNamespace#INTERMEDIARY}.
	 *
	 * @param namespace the new merge namespace
	 * @return this builder
	 */
	FileMappingsSpecBuilder mergeNamespace(MappingsNamespace namespace);

	/**
	 * Sets the merge namespace of this mappings spec.
	 *
	 * <p>The merge namespace is the namespace that is used to match up this layer's
	 * names to the rest of the mappings. For example, Yarn mappings should be merged through
	 * the intermediary names.
	 *
	 * <p>The default merge namespace is {@code intermediary}.
	 *
	 * @param namespace the new merge namespace
	 * @return this builder
	 */
	FileMappingsSpecBuilder mergeNamespace(String namespace);
}
