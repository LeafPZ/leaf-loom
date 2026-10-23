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

package dev.aoqia.leaf.loom.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.StandardProtocolFamily;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class CurrentPlatform implements Platform {
	static final Platform INSTANCE = new CurrentPlatform();

	private final OperatingSystem operatingSystem;
	private final Architecture architecture;
	private final boolean supportsUnixDomainSockets;
	private final boolean isRaspberryPi;

	private CurrentPlatform() {
		this.operatingSystem = getCurrentOperatingSystem();
		this.architecture = getCurrentArchitecture();
		this.supportsUnixDomainSockets = isUnixDomainSocketsSupported();
		this.isRaspberryPi = getIsRaspberryPi(operatingSystem, architecture);
	}

	private static OperatingSystem getCurrentOperatingSystem() {
		final String osName = System.getProperty("os.name").toLowerCase();

		if (osName.contains("win")) {
			return OperatingSystem.WINDOWS;
		} else if (osName.contains("mac")) {
			return OperatingSystem.MAC_OS;
		} else {
			// Or unknown
			return OperatingSystem.LINUX;
		}
	}

	private static Architecture getCurrentArchitecture() {
		final String arch = System.getProperty("os.arch");
		final boolean is64Bit = arch.contains("64") || arch.startsWith("armv8");
		final boolean isArm = arch.startsWith("arm") || arch.startsWith("aarch64");
		final boolean isRiscV = arch.startsWith("riscv");

		return new Architecture() {
			@Override
			public boolean is64Bit() {
				return is64Bit;
			}

			@Override
			public boolean isArm() {
				return isArm;
			}

			@Override
			public boolean isRiscV() {
				return isRiscV;
			}
		};
	}

	// Requires Unix, or Windows 10 17063 or later. See: https://devblogs.microsoft.com/commandline/af_unix-comes-to-windows/
	private static boolean isUnixDomainSocketsSupported() {
		try (ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
			return true;
		} catch (UnsupportedOperationException e) {
			return false;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Returns true if the current system is a Raspberry Pi running Debian 12 (Bookworm).
	 */
	private static boolean getIsRaspberryPi(OperatingSystem operatingSystem, Architecture architecture) {
		if (operatingSystem != OperatingSystem.LINUX || !architecture.isArm()) {
			return false;
		}

		try {
			final Path releasePath = Paths.get("/etc/os-release");
			final String release = Files.readString(releasePath, StandardCharsets.UTF_8);
			final boolean isDebianBookworm = release.contains("VERSION_CODENAME=bookworm");

			final Path modelPath = Paths.get("/sys/firmware/devicetree/base/model");
			final String model = Files.readString(modelPath, StandardCharsets.UTF_8);
			final boolean isRaspberryPi = model.startsWith("Raspberry Pi");

			return isDebianBookworm && isRaspberryPi;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public OperatingSystem getOperatingSystem() {
		return operatingSystem;
	}

	@Override
	public Architecture getArchitecture() {
		return architecture;
	}

	@Override
	public boolean supportsUnixDomainSockets() {
		return supportsUnixDomainSockets;
	}

	@Override
	public boolean isRaspberryPi() {
		return isRaspberryPi;
	}
}
