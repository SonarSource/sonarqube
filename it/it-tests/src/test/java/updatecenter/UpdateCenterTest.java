/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package updatecenter;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Plugin;
import org.sonar.wsclient.services.UpdateCenterQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;

public class UpdateCenterTest {

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setServerProperty("sonar.updatecenter.url", UpdateCenterTest.class.getResource("UpdateCenterTest/update-center.properties").toString())
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
      "/updatecenter/installed-plugins.html")
      .build();
    orchestrator.executeSelenese(selenese);
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
