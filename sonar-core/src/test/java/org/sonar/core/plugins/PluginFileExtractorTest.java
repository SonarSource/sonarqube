/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class PluginFileExtractorTest {

  private PluginFileExtractor extractor= new PluginFileExtractor();

  @Test
  public void shouldExtractMetadata() {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFile("sonar-checkstyle-plugin-2.8.jar"), true);
    assertThat(metadata.getKey(), is("checkstyle"));
    assertThat(metadata.getBasePlugin(), nullValue());
    assertThat(metadata.getName(), is("Checkstyle"));
    assertThat(metadata.isCore(), is(true));
    assertThat(metadata.getFile().getName(), is("sonar-checkstyle-plugin-2.8.jar"));
  }

  @Test
  public void shouldExtractDeprecatedMetadata() {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFile("sonar-emma-plugin-0.3.jar"), false);
    assertThat(metadata.getKey(), is("emma"));
    assertThat(metadata.getBasePlugin(), nullValue());
    assertThat(metadata.getName(), is("Emma"));
  }

  @Test
  public void shouldExtractExtensionMetadata() {
    DefaultPluginMetadata metadata = extractor.extractMetadata(getFile("sonar-checkstyle-extensions-plugin-0.1-SNAPSHOT.jar"), true);
    assertThat(metadata.getKey(), is("checkstyleextensions"));
    assertThat(metadata.getBasePlugin(), is("checkstyle"));
  }

  @Test
  public void shouldCopyAndExtractDependencies() throws IOException {
    File toDir = new File("target/test-tmp/PluginFileExtractorTest/shouldCopyAndExtractDependencies");
    FileUtils.forceMkdir(toDir);
    FileUtils.cleanDirectory(toDir);

    DefaultPluginMetadata metadata = extractor.install(getFile("sonar-checkstyle-plugin-2.8.jar"), true, toDir);

    assertThat(metadata.getKey(), is("checkstyle"));
    assertThat(new File(toDir, "sonar-checkstyle-plugin-2.8.jar").exists(), is(true));
    assertThat(new File(toDir, "META-INF/lib/checkstyle-5.1.jar").exists(), is(true));
  }

  @Test
  public void shouldExtractOnlyDependencies() throws IOException {
    File toDir = new File("target/test-tmp/PluginFileExtractorTest/shouldExtractOnlyDependencies");
    FileUtils.forceMkdir(toDir);
    FileUtils.cleanDirectory(toDir);

    extractor.install(getFile("sonar-checkstyle-plugin-2.8.jar"), true, toDir);

    assertThat(new File(toDir, "sonar-checkstyle-plugin-2.8.jar").exists(), is(true));
    assertThat(new File(toDir, "META-INF/MANIFEST.MF").exists(), is(false));
    assertThat(new File(toDir, "org/sonar/plugins/checkstyle/CheckstyleVersion.class").exists(), is(false));
  }

  @Test
  public void shouldCopyRuleExtensionsOnServerSide() throws IOException {
    File toDir = new File("target/test-tmp/PluginFileExtractorTest/shouldCopyRuleExtensionsOnServerSide");
    FileUtils.forceMkdir(toDir);
    FileUtils.cleanDirectory(toDir);

    DefaultPluginMetadata metadata = DefaultPluginMetadata.create(getFile("sonar-checkstyle-plugin-2.8.jar"))
        .setKey("checkstyle")
        .addDeprecatedExtension(getFile("PluginFileExtractorTest/shouldCopyRuleExtensionsOnServerSide/checkstyle-extension.xml"));
    extractor.install(metadata, toDir);

    assertThat(new File(toDir, "sonar-checkstyle-plugin-2.8.jar").exists(), is(true));
    assertThat(new File(toDir, "checkstyle-extension.xml").exists(), is(true));
  }

  private File getFile(String filename) {
    return FileUtils.toFile(getClass().getResource("/org/sonar/core/plugins/" + filename));
  }
}
