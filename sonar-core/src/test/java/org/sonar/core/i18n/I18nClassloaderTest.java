/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.i18n;

import com.google.common.collect.Lists;
import java.net.URL;
import java.net.URLClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.platform.PluginRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class I18nClassloaderTest {
  private I18nClassloader i18nClassloader;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void init() {
    i18nClassloader = new I18nClassloader(mock(PluginRepository.class));
  }

  @Test
  public void aggregate_plugin_classloaders() {
    URLClassLoader checkstyle = newCheckstyleClassloader();

    I18nClassloader i18nClassloader = new I18nClassloader(Lists.newArrayList(checkstyle));
    assertThat(i18nClassloader.getResource("org/sonar/l10n/checkstyle.properties")).isNotNull();
    assertThat(i18nClassloader.getResource("org/sonar/l10n/checkstyle.properties").getFile()).endsWith("checkstyle.properties");
  }

  @Test
  public void contain_its_own_classloader() {
    assertThat(i18nClassloader.getResource("org/sonar/l10n/core.properties")).isNotNull();
  }

  @Test
  public void return_null_if_resource_not_found() {
    assertThat(i18nClassloader.getResource("org/unknown.properties")).isNull();
  }

  @Test
  public void not_support_lookup_of_java_classes() throws ClassNotFoundException {
    thrown.expect(UnsupportedOperationException.class);
    i18nClassloader.loadClass("java.lang.String");
  }

  @Test
  public void override_toString() {
    assertThat(i18nClassloader.toString()).isEqualTo("i18n-classloader");
  }

  private static URLClassLoader newCheckstyleClassloader() {
    return newClassLoader("/org/sonar/core/i18n/I18nClassloaderTest/");
  }

  private static URLClassLoader newClassLoader(String... resourcePaths) {
    URL[] urls = new URL[resourcePaths.length];
    for (int index = 0; index < resourcePaths.length; index++) {
      urls[index] = I18nClassloaderTest.class.getResource(resourcePaths[index]);
    }
    return new URLClassLoader(urls);
  }
}
