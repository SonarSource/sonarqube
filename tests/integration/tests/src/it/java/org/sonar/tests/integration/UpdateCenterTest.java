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
package org.sonar.tests.integration;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Plugin;
import org.sonar.wsclient.services.UpdateCenterQuery;

import java.util.List;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;

public class UpdateCenterTest {

  @Test
  public void shouldGetInstalledPlugins() {
    Sonar sonar = Sonar.create("http://localhost:9000");
    List<Plugin> plugins = sonar.findAll(UpdateCenterQuery.createForInstalledPlugins());
    assertThat(plugins.size(), greaterThan(0));

    Plugin referencePlugin = findReferencePlugin(plugins, "itreference");
    assertNotNull(referencePlugin);
    assertThat(referencePlugin.getName(), is("Sonar :: Integration Tests :: Reference Plugin"));
    assertThat(referencePlugin.getVersion(), startsWith("2."));
  }

  private Plugin findReferencePlugin(List<Plugin> plugins, String pluginKey) {
    for (Plugin plugin : plugins) {
      if (StringUtils.equals(pluginKey, plugin.getKey())) {
        return plugin;
      }
    }
    return null;
  }

}
