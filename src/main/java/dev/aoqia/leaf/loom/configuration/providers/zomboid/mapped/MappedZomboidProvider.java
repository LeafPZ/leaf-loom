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

package dev.aoqia.leaf.loom.configuration.providers.zomboid.mapped;

import java.nio.file.Path;
import java.util.List;

import dev.aoqia.leaf.loom.configuration.providers.zomboid.SingleJarEnvType;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ZomboidJar;

public interface MappedZomboidProvider {
	default List<Path> getZomboidJarPaths() {
		return getZomboidJars().stream().map(ZomboidJar::getPath).toList();
	}

	List<ZomboidJar> getZomboidJars();

	interface ProviderImpl extends MappedZomboidProvider {
		Path getJar(ZomboidJar.Type type);
	}

	interface Merged extends ProviderImpl {
		default ZomboidJar getMergedJar() {
			return new ZomboidJar.Merged(getJar(ZomboidJar.Type.MERGED));
		}

		@Override
		default List<ZomboidJar> getZomboidJars() {
			return List.of(getMergedJar());
		}
	}

	interface Split extends ProviderImpl {
		default ZomboidJar getCommonJar() {
			return new ZomboidJar.Common(getJar(ZomboidJar.Type.COMMON));
		}

		default ZomboidJar getClientOnlyJar() {
			return new ZomboidJar.ClientOnly(getJar(ZomboidJar.Type.CLIENT_ONLY));
		}

		@Override
		default List<ZomboidJar> getZomboidJars() {
			return List.of(getCommonJar(), getClientOnlyJar());
		}
	}

	interface SingleJar extends ProviderImpl {
		SingleJarEnvType env();

		default ZomboidJar.Type envType() {
			return env().getType();
		}

		default ZomboidJar getEnvOnlyJar() {
			return env().getJar().apply(getJar(env().getType()));
		}

		@Override
		default List<ZomboidJar> getZomboidJars() {
			return List.of(getEnvOnlyJar());
		}
	}

    interface CompleteJar extends ProviderImpl {
        default ZomboidJar getGameJar() {
            return new ZomboidJar.Complete(getJar(ZomboidJar.Type.COMPLETE));
        }

        @Override
        default List<ZomboidJar> getZomboidJars() {
            return List.of(getGameJar());
        }
    }
}
