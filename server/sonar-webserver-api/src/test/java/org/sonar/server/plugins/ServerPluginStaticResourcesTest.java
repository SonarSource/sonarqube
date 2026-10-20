/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.server.plugins.PluginFilesAndMd5.FileAndMd5;

import static org.assertj.core.api.Assertions.assertThat;

class ServerPluginStaticResourcesTest {

  private static final String HELLO_SHA256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

  @TempDir
  private Path temp;

  private final ServerPluginStaticResources underTest = new ServerPluginStaticResources();

  @Test
  void getStaticResources_returnsOnlyEntriesUnderStaticFolder_withPathRelativeToIt() throws IOException {
    ServerPlugin plugin = newPlugin("foo", Map.of(
      "static/a.txt", "a",
      "static/sub/b.txt", "b",
      "META-INF/MANIFEST.MF", "Manifest-Version: 1.0",
      "org/foo/Bar.class", "not really a class"));

    assertThat(underTest.getStaticResources(plugin))
      .extracting(PluginStaticResource::path)
      .containsExactly("a.txt", "sub/b.txt");
  }

  @Test
  void getStaticResources_computesSha256AndSizeOfEachResource() throws IOException {
    ServerPlugin plugin = newPlugin("foo", Map.of("static/a.txt", "hello"));

    assertThat(underTest.getStaticResources(plugin))
      .containsExactly(new PluginStaticResource("a.txt", HELLO_SHA256, 5L));
  }

  @Test
  void getStaticResources_returnsEmpty_whenPluginShipsNoStaticResource() throws IOException {
    ServerPlugin plugin = newPlugin("foo", Map.of("org/foo/Bar.class", "not really a class"));

    assertThat(underTest.getStaticResources(plugin)).isEmpty();
  }

  @Test
  void getStaticResources_isSortedByPath_whateverTheOrderOfTheJarEntries() throws IOException {
    Map<String, String> entries = new LinkedHashMap<>();
    entries.put("static/z.txt", "z");
    entries.put("static/a.txt", "a");
    entries.put("static/m/b.txt", "b");
    ServerPlugin plugin = newPlugin("foo", entries);

    assertThat(underTest.getStaticResources(plugin))
      .extracting(PluginStaticResource::path)
      .containsExactly("a.txt", "m/b.txt", "z.txt");
  }

  @Test
  void getStaticResources_ignoresDirectoryEntries() throws IOException {
    ServerPlugin plugin = newPlugin("foo", Map.of(
      "static/", "",
      "static/sub/", "",
      "static/sub/b.txt", "b"));

    assertThat(underTest.getStaticResources(plugin))
      .extracting(PluginStaticResource::path)
      .containsExactly("sub/b.txt");
  }

  @Test
  void getStaticResources_includesTheDocumentation() throws IOException {
    ServerPlugin plugin = newPlugin("foo", Map.of("static/documentation.md", "# Foo"));

    assertThat(underTest.getStaticResources(plugin))
      .extracting(PluginStaticResource::path)
      .containsExactly("documentation.md");
  }

  @Test
  void getStaticResources_ignoresEntriesNavigatingOutOfTheStaticFolder() throws IOException {
    ServerPlugin plugin = newPlugin("foo", Map.of("static/../evil.txt", "evil"));

    assertThat(underTest.getStaticResources(plugin)).isEmpty();
  }

  @Test
  void getStaticResources_returnsEmpty_whenJarCannotBeRead() throws IOException {
    File notAnArchive = Files.createFile(temp.resolve("empty.jar")).toFile();
    ServerPlugin plugin = newPlugin("foo", notAnArchive);

    assertThat(underTest.getStaticResources(plugin)).isEmpty();
  }

  @Test
  void getStaticResources_scansTheJarAgain_whenItCouldNotBeReadOnTheFirstCall() throws IOException {
    File jar = Files.createFile(temp.resolve("foo.jar")).toFile();
    ServerPlugin plugin = newPlugin("foo", jar);
    assertThat(underTest.getStaticResources(plugin)).isEmpty();

    writeJar(jar, Map.of("static/a.txt", "hello"));

    assertThat(underTest.getStaticResources(plugin))
      .containsExactly(new PluginStaticResource("a.txt", HELLO_SHA256, 5L));
  }

  @Test
  void getStaticResources_readsTheJarOnlyOnce() throws IOException {
    ServerPlugin plugin = newPlugin("foo", Map.of("static/a.txt", "hello"));
    List<PluginStaticResource> firstCall = underTest.getStaticResources(plugin);
    Files.delete(plugin.getJar().getFile().toPath());

    assertThat(underTest.getStaticResources(plugin)).isEqualTo(firstCall);
  }

  @Test
  void getStaticResources_returnsEntriesUnderResourcesFolder_forZipPackagedPlugin() throws IOException {
    File zip = temp.resolve("foo.zip").toFile();
    writeJar(zip, Map.of(
      "resources/a.txt", "a",
      "resources/sub/b.txt", "b",
      "plugin/foo-1.0.jar", "x"));
    ServerPlugin plugin = newPlugin("foo", zip);

    assertThat(underTest.getStaticResources(plugin))
      .extracting(PluginStaticResource::path)
      .containsExactly("a.txt", "sub/b.txt");
  }

  @Test
  void getStaticResources_zipPackagedPlugin_readsFromResourcesArchive_evenIfInstalledFileWasRemoved() throws IOException {
    File installedZip = temp.resolve("installed.zip").toFile();
    writeJar(installedZip, Map.of("resources/a.txt", "a", "plugin/foo-1.0.jar", "x"));
    File deployedResourcesArchive = temp.resolve("deployed.zip").toFile();
    Files.copy(installedZip.toPath(), deployedResourcesArchive.toPath());
    PluginInfo pluginInfo = new PluginInfo("foo").setJarFile(installedZip);
    ServerPlugin plugin = new ServerPlugin(pluginInfo, PluginType.BUNDLED, null, new FileAndMd5(installedZip), null, deployedResourcesArchive);

    // simulates PluginUninstaller moving the installed archive away while the server is still up
    Files.delete(installedZip.toPath());

    assertThat(underTest.getStaticResources(plugin))
      .extracting(PluginStaticResource::path)
      .containsExactly("a.txt");
  }

  @Test
  void getStaticResources_returnsEmpty_whenZipPackagedPluginHasNoResourcesArchive() throws IOException {
    File installedZip = temp.resolve("installed.zip").toFile();
    writeJar(installedZip, Map.of("plugin/foo-1.0.jar", "x"));
    PluginInfo pluginInfo = new PluginInfo("foo").setJarFile(installedZip);
    ServerPlugin plugin = new ServerPlugin(pluginInfo, PluginType.BUNDLED, null, new FileAndMd5(installedZip), null, null);

    assertThat(underTest.getStaticResources(plugin)).isEmpty();
  }

  private ServerPlugin newPlugin(String key, Map<String, String> entriesByName) throws IOException {
    File jar = temp.resolve(key + ".jar").toFile();
    writeJar(jar, entriesByName);
    return newPlugin(key, jar);
  }

  private static void writeJar(File jar, Map<String, String> entriesByName) throws IOException {
    try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar))) {
      for (Map.Entry<String, String> entry : entriesByName.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
    }
  }

  private static ServerPlugin newPlugin(String key, File jar) {
    PluginInfo pluginInfo = new PluginInfo(key).setJarFile(jar);
    File resourcesArchive = pluginInfo.isZipPackaging() ? jar : null;
    return new ServerPlugin(pluginInfo, PluginType.BUNDLED, null, new FileAndMd5(jar), null, resourcesArchive);
  }
}
