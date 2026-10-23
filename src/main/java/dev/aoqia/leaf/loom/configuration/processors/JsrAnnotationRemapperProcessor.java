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

package dev.aoqia.leaf.loom.configuration.processors;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import javax.inject.Inject;

import org.jspecify.annotations.Nullable;

import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

import dev.aoqia.leaf.loom.api.processor.ProcessorContext;
import dev.aoqia.leaf.loom.api.processor.SpecContext;
import dev.aoqia.leaf.loom.api.processor.ZomboidJarProcessor;
import dev.aoqia.leaf.loom.util.TinyRemapperLoggerAdapter;

public class JsrAnnotationRemapperProcessor implements ZomboidJarProcessor<JsrAnnotationRemapperProcessor.Spec> {
	private static final Map<String, String> JETBRAINS_TO_JSR = Map.of(
			"org/jetbrains/annotations/Nullable", "javax/annotation/Nullable",
			"org/jetbrains/annotations/NotNull", "javax/annotation/Nonnull",
			"org/jetbrains/annotations/Unmodifiable", "javax/annotation/concurrent/Immutable"
	);

	private final String name;

	@Inject
	public JsrAnnotationRemapperProcessor(String name) {
		this.name = name;
	}

	@Override
	public @Nullable Spec buildSpec(SpecContext context) {
		return new Spec(JETBRAINS_TO_JSR);
	}

	@Override
	public void processJar(Path jar, Spec spec, ProcessorContext context) throws IOException {
		TinyRemapper tinyRemapper = TinyRemapper.newRemapper(TinyRemapperLoggerAdapter.INSTANCE)
				.withMappings(spec.getMappings())
				.build();

		try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(jar).build()) {
			tinyRemapper.readInputs(jar);
			tinyRemapper.apply(outputConsumer);
		} catch (Exception e) {
			throw new RuntimeException("Failed to remap JAR " + jar + " with mapping " + spec.annotationMapping(), e);
		} finally {
			tinyRemapper.finish();
		}
	}

	@Override
	public String getName() {
		return name;
	}

	public record Spec(Map<String, String> annotationMapping) implements ZomboidJarProcessor.Spec {
		public IMappingProvider getMappings() {
			return out -> annotationMapping.forEach(out::acceptClass);
		}
	}
}
