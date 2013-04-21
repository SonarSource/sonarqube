/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.i18n;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.URLClassLoader;

import static org.fest.assertions.Assertions.assertThat;

public class I18nClassloaderTest {
  private I18nClassloader i18nClassloader;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void init() {
    URLClassLoader sqale = I18nManagerTest.newSqaleClassloader();
    URLClassLoader checkstyle = I18nManagerTest.newCheckstyleClassloader();

    i18nClassloader = new I18nClassloader(new ClassLoader[]{sqale, checkstyle});
  }

  @Test
  public void should_aggregate_plugin_classloaders() {
    assertThat(i18nClassloader.getResource("org/sonar/l10n/checkstyle.properties")).isNotNull();
    assertThat(i18nClassloader.getResource("org/sonar/l10n/checkstyle.properties").getFile()).endsWith("checkstyle.properties");
    assertThat(i18nClassloader.getResource("org/sonar/l10n/checkstyle/ArchitectureRule.html").getFile()).endsWith("ArchitectureRule.html");
  }

  @Test
  public void should_return_null_if_resource_not_found() {
    assertThat(i18nClassloader.getResource("org/unknown.properties")).isNull();
  }

  @Test
  public void should_not_support_lookup_of_java_classes() throws ClassNotFoundException {
    thrown.expect(UnsupportedOperationException.class);
    i18nClassloader.loadClass("java.lang.String");
  }

  @Test
  public void should_override_toString() throws ClassNotFoundException {
    assertThat(i18nClassloader.toString()).isEqualTo("i18n-classloader");
  }
}
