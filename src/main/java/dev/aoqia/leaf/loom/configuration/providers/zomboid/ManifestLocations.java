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
 * Copyright (c) 2024 FabricMC
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

package dev.aoqia.leaf.loom.configuration.providers.zomboid;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import dev.aoqia.leaf.loom.api.manifest.VersionsManifestsAPI;
import dev.aoqia.leaf.loom.configuration.providers.zomboid.ManifestLocations.ManifestLocation;

public class ManifestLocations implements VersionsManifestsAPI, Iterable<ManifestLocation> {
	private final Queue<ManifestLocation> locations = new PriorityQueue<>();
	private final Set<String> manifestNames = new HashSet<>();

	@Override
	public void add(String name, String url, int priority) {
		if (manifestNames.add(name)) {
			locations.add(new ManifestLocation(name, url, priority));
		} else {
			throw new IllegalStateException("cannot add multiple versions manifests with the same name!");
		}
	}

	@Override
	public Iterator<ManifestLocation> iterator() {
		return locations.iterator();
	}

	public class ManifestLocation implements Comparable<ManifestLocation> {
		private final String name;
		private final String url;
		private final int priority;

		private ManifestLocation(String name, String url, int priority) {
			this.name = name;
			this.url = url;
			this.priority = priority;
		}

		public String name() {
			return name;
		}

		public String url() {
			return url;
		}

		public Path cacheFile(Path dir) {
			return dir.resolve(name + "_versions_manifest.json");
		}

		@Override
		public int compareTo(ManifestLocation o) {
			return Integer.compare(priority, o.priority);
		}
	}
}
