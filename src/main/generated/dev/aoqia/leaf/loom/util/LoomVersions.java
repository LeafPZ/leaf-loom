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

package dev.aoqia.leaf.loom.util;

/**
 * Auto generated class, do not edit.
 */
public record LoomVersions(String group, String module, String version) {
	public static final LoomVersions CFR = new LoomVersions("net.fabricmc", "cfr", "0.2.2");
	public static final LoomVersions DEV_LAUNCH_INJECTOR = new LoomVersions("net.fabricmc", "dev-launch-injector", "0.2.1+build.8");
	public static final LoomVersions ENIGMA_SWING = new LoomVersions("cuchaz", "enigma-swing", "3.0.1");
	public static final LoomVersions FABRIC_LOG4J_UTIL = new LoomVersions("net.fabricmc", "fabric-log4j-util", "1.0.2");
	public static final LoomVersions JETBRAINS_ANNOTATIONS = new LoomVersions("org.jetbrains", "annotations", "26.0.2");
	public static final LoomVersions LEAF_INSTALLER = new LoomVersions("dev.aoqia.leaf", "installer", "1.2.0");
	public static final LoomVersions MIXIN_COMPILE_EXTENSIONS = new LoomVersions("net.fabricmc", "fabric-mixin-compile-extensions", "0.6.0");
	public static final LoomVersions NATIVE_SUPPORT = new LoomVersions("net.fabricmc", "fabric-loom-native-support", "1.0.1");
	public static final LoomVersions RENDERDOC = new LoomVersions("org.renderdoc", "renderdoc", "1.37");
	public static final LoomVersions VINEFLOWER = new LoomVersions("org.vineflower", "vineflower", "1.12.0");

	public String mavenNotation() {
		return "%s:%s:%s".formatted(group, module, version);
	}
}
