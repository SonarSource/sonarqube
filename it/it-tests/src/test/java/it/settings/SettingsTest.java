/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package it.settings;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category1Suite;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Test;
import util.selenium.SeleneseTest;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  // SONAR-4404
  @Test
  public void should_get_settings_default_value() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("settings-default-value",
      "/settings/SettingsTest/settings-default-value.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  /**
   * SONAR-3320
   */
  @Test
  public void global_property_change_extension_point() throws IOException {
    orchestrator.getServer().adminWsClient().post("api/properties/create?id=globalPropertyChange.received&value=NEWVALUE");
    assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs()).contains("Received change: NEWVALUE"));
  }

}
