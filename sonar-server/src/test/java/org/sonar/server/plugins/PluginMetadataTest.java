/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.plugins;

import org.junit.Test;
import org.sonar.test.TestUtils;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PluginMetadataTest {

  @Test
  public void testCreateFromJar() throws IOException {
    PluginMetadata metadata = PluginMetadata.createFromJar(TestUtils.getResource(getClass(), "foo-plugin.jar"), false);
    assertThat(metadata.getKey(), is("foo"));
    assertThat(metadata.getFilename(), is("foo-plugin.jar"));
    assertThat(metadata.getMainClass(), is("foo.Main"));
    assertThat(metadata.getVersion(), is("2.2-SNAPSHOT"));
    assertThat(metadata.getOrganization(), is("SonarSource"));
    assertThat(metadata.getDependencyPaths().length, is(0));
    assertThat(metadata.isCore(), is(false));
  }

  @Test
  public void testOldPlugin() {
    PluginMetadata metadata = new PluginMetadata();
    metadata.setMainClass("foo.Main");
    assertThat(metadata.isOldManifest(), is(true));

    metadata.setKey("foo");
    assertThat(metadata.isOldManifest(), is(false));
  }
}
