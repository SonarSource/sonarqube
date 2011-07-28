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
package org.sonar.core.i18n;

import com.google.common.collect.Maps;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertThat;

public class I18nManagerTest {
  @Test
  public void shouldExtractBundleKey() {
    Map<String,ClassLoader> bundleToClassLoaders = Maps.newHashMap();
    bundleToClassLoaders.put("core", getClass().getClassLoader());
    bundleToClassLoaders.put("checkstyle", getClass().getClassLoader());
    bundleToClassLoaders.put("sqale", getClass().getClassLoader());
    I18nManager i18n = new I18nManager(bundleToClassLoaders);

    assertThat(i18n.keyToBundle("by"), Is.is("core"));
    assertThat(i18n.keyToBundle("violations_drilldown.page"), Is.is("core"));
    assertThat(i18n.keyToBundle("checkstyle.rule1.name"), Is.is("checkstyle"));
    assertThat(i18n.keyToBundle("sqale.console.page"), Is.is("sqale"));
  }
}
