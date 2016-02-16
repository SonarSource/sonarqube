/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.updateCenter;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.wsclient.services.Plugin;
import org.sonar.wsclient.services.UpdateCenterQuery;
import util.QaOnly;
import util.selenium.SeleneseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

/**
 * This class start its own orchestrator
 */
@Category(QaOnly.class)
public class UpdateCenterTest {

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .setServerProperty("sonar.updatecenter.url", UpdateCenterTest.class.getResource("/updateCenter/UpdateCenterTest/update-center.properties").toString())
    .addPlugin(pluginArtifact("sonar-fake-plugin"))
    .build();

  @Test
  public void web_service_should_return_installed_plugins() {
    List<Plugin> plugins = orchestrator.getServer().getAdminWsClient().findAll(UpdateCenterQuery.createForInstalledPlugins());
    assertThat(plugins.size()).isGreaterThan(0);

    Plugin installedPlugin = findPlugin(plugins, "fake");

    assertThat(installedPlugin).isNotNull();
    assertThat(installedPlugin.getName()).isEqualTo("Plugins :: Fake");
    assertThat(installedPlugin.getVersion()).isEqualTo("1.0-SNAPSHOT");
  }

  @Test
  public void test_console() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("server-update-center",
      "/updateCenter/installed-plugins.html")
      .build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  private Plugin findPlugin(List<Plugin> plugins, String pluginKey) {
    for (Plugin plugin : plugins) {
      if (StringUtils.equals(pluginKey, plugin.getKey())) {
        return plugin;
      }
    }
    return null;
  }

}
