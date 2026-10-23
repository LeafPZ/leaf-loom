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

package dev.aoqia.leaf.loom.configuration.providers.zomboid.library.processors;

import java.util.function.Consumer;
import java.util.function.Predicate;

import dev.aoqia.leaf.loom.configuration.providers.zomboid.library.Library;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.library.LibraryContext;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.library.LibraryProcessor;
import dev.aoqia.leaf.loom.util.Platform;

public class ObjcBridgeUpgradeLibraryProcessor extends LibraryProcessor {
	private static final String OBJC_BRIDGE_PREFIX = "ca.weblite:java-objc-bridge";
	private static final String OBJC_BRIDGE_VERSION = "1.1";
	private static final String OBJC_BRIDGE_NAME = "%s:%s".formatted(OBJC_BRIDGE_PREFIX, OBJC_BRIDGE_VERSION);

	public ObjcBridgeUpgradeLibraryProcessor(Platform platform, LibraryContext context) {
		super(platform, context);
	}

	@Override
	public ApplicationResult getApplicationResult() {
		if (!context.usesLWJGL3()) {
			// Only supports LWJGL 3
			return ApplicationResult.DONT_APPLY;
		}

		if (!(platform.getOperatingSystem().isMacOS() && platform.getArchitecture().isArm())) {
			// Only supported on arm64 macOS
			return ApplicationResult.DONT_APPLY;
		}

		// Apply when Arm64 macOS is unsupported by Minecraft
		return context.supportsArm64(Platform.OperatingSystem.MAC_OS) ? ApplicationResult.DONT_APPLY : ApplicationResult.MUST_APPLY;
	}

	@Override
	public Predicate<Library> apply(Consumer<Library> dependencyConsumer) {
		// Add the updated library on the runtime classpath.
		dependencyConsumer.accept(Library.fromMaven(OBJC_BRIDGE_NAME, Library.Target.RUNTIME));

		return library -> {
			if (library.is(OBJC_BRIDGE_PREFIX)) {
				// Remove the natives, as they are included in the dep library we added to the runtime classpath above.
				return library.target() != Library.Target.NATIVES;
			}

			return true;
		};
	}
}
