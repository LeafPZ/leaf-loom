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

package dev.aoqia.leaf.loom.configuration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.aoqia.leaf.loom.configuration.processors.speccontext.DebofConfiguration;
import dev.aoqia.leaf.loom.util.ZipUtils;
import dev.aoqia.leaf.loom.util.fmj.LeafModJson;
import dev.aoqia.leaf.loom.util.fmj.LeafModJsonFactory;

public class DebofInstallerData {
	private static final Logger LOGGER = LoggerFactory.getLogger(DebofInstallerData.class);

	public static void findAndApply(Project project) {
		for (DebofConfiguration debofConfiguration : DebofConfiguration.ALL) {
			for (Configuration configuration : debofConfiguration.getConfigurations(project)) {
				Optional<InstallerData> installerData = configuration.getFiles().parallelStream()
						.filter(File::isFile)
						.map(DebofInstallerData::getInstaller)
						.filter(Objects::nonNull)
						.findFirst();

				if (installerData.isPresent()) {
					LOGGER.info("Applying installer data from configuration '{}'", configuration.getName());
					installerData.get().applyToProject(project);
					return;
				}
			}
		}

		LOGGER.info("No installer data found in any configuration.");
	}

	@Nullable
	private static InstallerData getInstaller(File file) {
		try {
			byte[] installerData = ZipUtils.unpackNullable(file.toPath(), InstallerData.INSTALLER_PATH);

			if (installerData == null) {
				return null;
			}

			LeafModJson LeafModJson = LeafModJsonFactory.createFromZip(file.toPath());
			LOGGER.info("Found installer in mod {} version {}", LeafModJson.getId(), LeafModJson.getModVersion());
			return InstallerData.fromBytes(installerData, LeafModJson.getModVersion());
		} catch (IOException e) {
			LOGGER.debug("Failed to read installer data from file '{}'", file, e);
			return null;
		}
	}
}
