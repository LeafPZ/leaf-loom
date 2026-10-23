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

package dev.aoqia.leaf.loom.configuration.providers.zomboid.library;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.gradle.api.artifacts.dsl.RepositoryHandler;

import dev.aoqia.leaf.loom.util.Platform;

public abstract class LibraryProcessor {
	protected static final Predicate<Library> ALLOW_ALL = library -> true;

	protected final Platform platform;
	protected final LibraryContext context;

	public LibraryProcessor(Platform platform, LibraryContext context) {
		this.platform = Objects.requireNonNull(platform);
		this.context = Objects.requireNonNull(context);
	}

	public abstract ApplicationResult getApplicationResult();

	public Predicate<Library> apply(Consumer<Library> dependencyConsumer) {
		return ALLOW_ALL;
	}

	public void applyRepositories(RepositoryHandler repositories) {
	}

	public enum ApplicationResult {
		/**
		 * This processor should be applied automatically to enable Minecraft to run on the current platform.
		 */
		MUST_APPLY,
		/**
		 * This processor can optionally be applied on the current platform.
		 */
		CAN_APPLY,
		/**
		 * This processor is incompatible with the current platform and should not be applied on the current platform.
		 */
		DONT_APPLY
	}
}
