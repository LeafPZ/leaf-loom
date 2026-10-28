/*
 * Copyright (C) 2026 aoqia
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <https://www.gnu.org/licenses/>.
 *
 * This license and file are additionally subjected to the exceptions described in LICENSE_EXCEPTION.md.
 *
 * The exceptions permit independent software to use this project as a dependency under terms of its own choosing, but
 * does not permit the copying or incorporation of covered source code into another work outside the terms of the AGPL.
 */

package dev.aoqia.leaf.loom.test;

/**
 * Auto generated class, do not edit.
 */
public record LoomTestVersions(String group, String module, String version) {
	public static final LoomTestVersions BCPKIX = new LoomTestVersions("org.bouncycastle", "bcpkix-jdk18on", "1.84");
	public static final LoomTestVersions BCPROV = new LoomTestVersions("org.bouncycastle", "bcprov-jdk18on", "1.84");
	public static final LoomTestVersions BCUTIL = new LoomTestVersions("org.bouncycastle", "bcutil-jdk18on", "1.84");
	public static final LoomTestVersions GRADLE_NIGHTLY = new LoomTestVersions("org.gradle", "dummy", "9.9.0-20260915012842+0000");
	public static final LoomTestVersions JAVA_DEBUG = new LoomTestVersions("com.microsoft.java", "com.microsoft.java.debug.core", "0.53.1");
	public static final LoomTestVersions JAVALIN = new LoomTestVersions("io.javalin", "javalin", "6.7.0");
	public static final LoomTestVersions JUNIT_JUPITER_ENGINE = new LoomTestVersions("org.junit.jupiter", "junit-jupiter-engine", "6.1.1");
	public static final LoomTestVersions LEAF_LOADER = new LoomTestVersions("dev.aoqia.leaf", "loader", "1.7.2");
	public static final LoomTestVersions MIXIN = new LoomTestVersions("net.fabricmc", "sponge-mixin", "0.17.2+mixin.0.8.7");
	public static final LoomTestVersions MOCKITO = new LoomTestVersions("org.mockito", "mockito-core", "5.23.0");
	public static final LoomTestVersions SPOCK = new LoomTestVersions("org.spockframework", "spock-core", "2.4-groovy-4.0");

	public String mavenNotation() {
		return "%s:%s:%s".formatted(group, module, version);
	}
}
