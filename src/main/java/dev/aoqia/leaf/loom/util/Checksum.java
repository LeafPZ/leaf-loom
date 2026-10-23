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

package dev.aoqia.leaf.loom.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

public final class Checksum {
	public static Checksum of(byte[] data) {
		return new Checksum(digest -> digest.write(data));
	}

	public static Checksum of(String str) {
		return new Checksum(digest -> digest.write(str));
	}

	public static Checksum of(File file) {
		return of(file.toPath());
	}

	public static Checksum of(Path file) {
		return new Checksum(digest -> {
			try (InputStream is = Files.newInputStream(file)) {
				is.transferTo(digest);
			}
		});
	}

	public static Checksum of(Project project) {
		return of(project.getProjectDir().getAbsolutePath() + ":" + project.getPath());
	}

	public static Checksum of(FileCollection files) {
		return new Checksum(os -> {
			for (File file : files) {
				try (InputStream is = Files.newInputStream(file.toPath())) {
					is.transferTo(os);
				}
			}
		});
	}

	public static Checksum of(List<Checksum> others) {
		return new Checksum(os -> {
			for (Checksum other : others) {
				other.consumer.accept(os);
			}
		});
	}

	private final DataConsumer consumer;

	private Checksum(DataConsumer consumer) {
		this.consumer = consumer;
	}

	public Result sha1() {
		return computeResult("SHA-1");
	}

	public Result sha256() {
		return computeResult("SHA-256");
	}

	public Result md5() {
		return computeResult("MD5");
	}

	private Result computeResult(String algorithm) {
		MessageDigest digest;

		try {
			digest = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		try (MessageDigestOutputStream os = new MessageDigestOutputStream(digest)) {
			consumer.accept(os);
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to compute checksum", e);
		}

		return new Result(digest.digest());
	}

	public record Result(byte[] digest) {
		public String hex() {
			return HexFormat.of().formatHex(digest());
		}

		public String hex(int length) {
			return hex().substring(0, length);
		}

		public boolean matchesStr(String other) {
			return hex().equalsIgnoreCase(other);
		}
	}

	@FunctionalInterface
	private interface DataConsumer {
		void accept(MessageDigestOutputStream os) throws IOException;
	}

	private static class MessageDigestOutputStream extends OutputStream {
		private final MessageDigest digest;

		private MessageDigestOutputStream(MessageDigest digest) {
			this.digest = digest;
		}

		@Override
		public void write(int b) {
			digest.update((byte) b);
		}

		@Override
		public void write(byte[] b, int off, int len) {
			digest.update(b, off, len);
		}

		public void write(String string) throws IOException {
			write(string.getBytes(StandardCharsets.UTF_8));
		}
	}
}
