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

import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.platform.PluginMetadata;

import java.io.File;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class DefaultPluginMetadataTest {

  @Test
  public void testGettersAndSetters() {
    DefaultPluginMetadata metadata = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar"));
    metadata.setKey("checkstyle")
        .setLicense("LGPL")
        .setDescription("description")
        .setHomepage("http://home")
        .setMainClass("org.Main")
        .setOrganization("SonarSource")
        .setOrganizationUrl("http://sonarsource.org")
        .setVersion("1.1");

    assertThat(metadata.getKey(), Is.is("checkstyle"));
    assertThat(metadata.getLicense(), Is.is("LGPL"));
    assertThat(metadata.getDescription(), Is.is("description"));
    assertThat(metadata.getHomepage(), Is.is("http://home"));
    assertThat(metadata.getMainClass(), Is.is("org.Main"));
    assertThat(metadata.getOrganization(), Is.is("SonarSource"));
    assertThat(metadata.getOrganizationUrl(), Is.is("http://sonarsource.org"));
    assertThat(metadata.getVersion(), Is.is("1.1"));
    assertThat(metadata.getBasePlugin(), nullValue());
    assertThat(metadata.getFile(), not(nullValue()));
    assertThat(metadata.getDeployedFiles().size(), is(0));
  }

  @Test
  public void testDeployedFiles() {
    DefaultPluginMetadata metadata = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar"))
        .addDeployedFile(new File("foo.jar"))
        .addDeployedFile(new File("bar.jar"));
    assertThat(metadata.getDeployedFiles().size(), is(2));
  }

  @Test
  public void testInternalPathToDependencies() {
    DefaultPluginMetadata metadata = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar"))
        .setPathsToInternalDeps(new String[]{"META-INF/lib/commons-lang.jar", "META-INF/lib/commons-io.jar"});
    assertThat(metadata.getPathsToInternalDeps().length, is(2));
    assertThat(metadata.getPathsToInternalDeps()[0], is("META-INF/lib/commons-lang.jar"));
    assertThat(metadata.getPathsToInternalDeps()[1], is("META-INF/lib/commons-io.jar"));
  }

  @Test
  public void shouldEquals() {
    DefaultPluginMetadata checkstyle = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar")).setKey("checkstyle");
    PluginMetadata pmd = DefaultPluginMetadata.create(new File("sonar-pmd-plugin.jar")).setKey("pmd");

    assertThat(checkstyle.equals(pmd), is(false));
    assertThat(checkstyle.equals(checkstyle), is(true));
    assertThat(checkstyle.equals(DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar")).setKey("checkstyle")), is(true));
  }

  @Test
  public void shouldCompare() {
    PluginMetadata checkstyle = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar"))
        .setKey("checkstyle")
        .setName("Checkstyle");
    PluginMetadata pmd = DefaultPluginMetadata.create(new File("sonar-pmd-plugin.jar"))
        .setKey("pmd")
        .setName("PMD");

    PluginMetadata[] array = {pmd, checkstyle};
    Arrays.sort(array);
    assertThat(array[0].getKey(), is("checkstyle"));
    assertThat(array[1].getKey(), is("pmd"));
  }
}
