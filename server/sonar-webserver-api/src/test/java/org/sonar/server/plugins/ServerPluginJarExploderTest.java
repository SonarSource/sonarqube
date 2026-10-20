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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.platform.ExplodedPlugin;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.platform.ServerFileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerPluginJarExploderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ServerFileSystem fs = mock(ServerFileSystem.class);
  private ServerPluginJarExploder underTest = new ServerPluginJarExploder(fs);

  @Test
  public void copy_all_classloader_files_to_dedicated_directory() throws Exception {
    File deployDir = temp.newFolder();
    when(fs.getDeployedPluginsDir()).thenReturn(deployDir);
    File sourceJar = TestProjectUtils.jarOf("test-libs-plugin");
    PluginInfo info = PluginInfo.create(sourceJar);

    ExplodedPlugin exploded = underTest.explode(info);

    // all the files loaded by classloaders (JAR + META-INF/libs/*.jar) are copied to the dedicated directory
    // web/deploy/{pluginKey}
    File pluginDeployDir = new File(deployDir, "testlibs");

    assertThat(exploded.getKey()).isEqualTo("testlibs");
    assertThat(exploded.getMain()).isFile().exists().hasParent(pluginDeployDir);
    assertThat(exploded.getLibs()).extracting("name").containsOnly("commons-daemon-1.0.15.jar", "commons-email-20030310.165926.jar");
    for (File lib : exploded.getLibs()) {
      assertThat(lib).exists().isFile();
      assertThat(lib.getCanonicalPath()).startsWith(pluginDeployDir.getCanonicalPath());
    }
    File targetJar = new File(fs.getDeployedPluginsDir(), "testlibs/test-libs-plugin-0.1-SNAPSHOT.jar");
  }

  @Test
  public void explode_zip_packaged_plugin_extracts_nested_jar_and_its_libs() throws Exception {
    File deployDir = temp.newFolder();
    when(fs.getDeployedPluginsDir()).thenReturn(deployDir);
    File zip = createZipPlugin("zipplugin");
    PluginInfo info = PluginInfo.create(zip);

    ExplodedPlugin exploded = underTest.explode(info);

    File pluginDeployDir = new File(deployDir, "zipplugin");
    assertThat(exploded.getKey()).isEqualTo("zipplugin");
    assertThat(exploded.getMain()).isFile().exists().hasParent(pluginDeployDir).hasName("zipplugin-1.0.jar");
    assertThat(exploded.getLibs()).extracting("name").containsOnly("somelib.jar");

    // the outer zip is also copied to the deploy dir, so resources/** stays servable even after
    // PluginUninstaller moves the installed zip away while the server is still up
    assertThat(exploded.getResourcesArchive()).isFile().exists().hasParent(pluginDeployDir).hasName(zip.getName());
    assertThat(exploded.getResourcesArchive()).hasSameBinaryContentAs(zip);
  }

  @Test
  public void explode_zip_packaged_plugin_never_writes_outside_the_deploy_dir_for_a_hostile_nested_jar_name() throws Exception {
    File deployDir = temp.newFolder();
    when(fs.getDeployedPluginsDir()).thenReturn(deployDir);
    File zip = createZipPlugin("zipplugin", "plugin/../../evil.jar");
    PluginInfo info = PluginInfo.create(zip);

    ExplodedPlugin exploded = underTest.explode(info);

    // only the last path segment of the entry name is kept, so the traversal prefix is dropped
    File pluginDeployDir = new File(deployDir, "zipplugin");
    assertThat(exploded.getMain()).isFile().hasParent(pluginDeployDir).hasName("evil.jar");
    assertThat(new File(deployDir, "evil.jar")).doesNotExist();
    assertThat(new File(deployDir.getParentFile(), "evil.jar")).doesNotExist();
  }

  private File createZipPlugin(String key) throws IOException {
    return createZipPlugin(key, "plugin/" + key + "-1.0.jar");
  }

  private File createZipPlugin(String key, String nestedJarEntryName) throws IOException {
    File zip = temp.newFile(key + "-1.0.zip");
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip))) {
      zos.putNextEntry(new ZipEntry(nestedJarEntryName));
      zos.write(createInnerJarBytes(key));
      zos.closeEntry();
      zos.putNextEntry(new ZipEntry("resources/foo.txt"));
      zos.write("hello".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
    return zip;
  }

  private static byte[] createInnerJarBytes(String key) throws IOException {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().putValue("Plugin-Key", key);
    manifest.getMainAttributes().putValue("Plugin-Version", "1.0");
    manifest.getMainAttributes().putValue("Plugin-Name", key);
    manifest.getMainAttributes().putValue("Plugin-Class", "org.foo.Plugin");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (JarOutputStream jos = new JarOutputStream(baos, manifest)) {
      jos.putNextEntry(new ZipEntry("META-INF/lib/somelib.jar"));
      jos.write("fake-lib-bytes".getBytes(StandardCharsets.UTF_8));
      jos.closeEntry();
    }
    return baos.toByteArray();
  }
}
