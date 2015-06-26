/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package updatecenter;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;

public class UpdateCenterSystemUpdateTest {

  Orchestrator orchestrator;

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  /**
   * SONAR-4279
   */
  @Test
  public void should_not_display_already_compatible_plugins_on_system_update() {
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.updatecenter.url",
        UpdateCenterSystemUpdateTest.class.getResource("UpdateCenterTest/update-center-system-update-with-already-compatible-plugins.properties").toString())
      .addPlugin(pluginArtifact("sonar-fake-plugin"))
      .build();

    orchestrator.start();
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("system-updates-without-plugin-updates",
      "/updatecenter/system-updates-without-plugin-updates.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-4585
   */
  @Test
  public void should_system_update_page_not_fail_when_installed_plugin_version_not_found_in_update_center_definitions() throws IOException {
    orchestrator = Orchestrator.builderEnv()
      .setServerProperty("sonar.updatecenter.url",
        UpdateCenterSystemUpdateTest.class.getResource("UpdateCenterTest/update-center-with-missing-plugin-version.properties").toString())
      .addPlugin(pluginArtifact("sonar-fake-plugin"))
      .build();

    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("system-updates-with-missing-installed-plugin-version",
      "/updatecenter/system-updates-with-missing-installed-plugin-version.html"
      ).build();
    orchestrator.executeSelenese(selenese);

    // Exception stacktrace should not be in logs
    File logs = orchestrator.getServer().getLogs();
    assertThat(FileUtils.readFileToString(logs)).doesNotContain("NoSuchElementException");
  }

}
