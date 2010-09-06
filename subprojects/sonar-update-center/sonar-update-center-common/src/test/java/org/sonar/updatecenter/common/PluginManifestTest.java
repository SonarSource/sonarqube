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
package org.sonar.updatecenter.common;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;

public class PluginManifestTest {

  @Test
  public void testCreateManifest() throws URISyntaxException, IOException {
    URL jar = getClass().getResource("/org/sonar/updatecenter/common/PluginManifestTest/checkstyle-plugin.jar");
    PluginManifest manifest = new PluginManifest(new File(jar.toURI()));

    assertThat(manifest.getKey(), is("checkstyle"));
    assertThat(manifest.getName(), is("Checkstyle"));
    assertThat(manifest.getMainClass(), is("org.sonar.plugins.checkstyle.CheckstylePlugin"));
    assertThat(manifest.getVersion().length(), greaterThan(1));
    assertThat(manifest.getDependencies().length, is(4));
    assertThat(manifest.getDependencies()[0], is("META-INF/lib/antlr-2.7.6.jar"));
  }

  @Test
  public void doNotFailWhenNoOldPluginManifest() throws URISyntaxException, IOException {
    URL jar = getClass().getResource("/org/sonar/updatecenter/common/PluginManifestTest/old-plugin.jar");
    PluginManifest manifest = new PluginManifest(new File(jar.toURI()));

    assertThat(manifest.getKey(), nullValue());
    assertThat(manifest.getName(), nullValue());
    assertThat(manifest.getMainClass(), is("org.sonar.plugins.checkstyle.CheckstylePlugin"));
    assertThat(manifest.getDependencies().length, is(0));
  }
}
