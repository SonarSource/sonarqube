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
package org.sonar.server.plugins.ws;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

public class PluginsWsMediumTest {
  @ClassRule
  public static ServerTester serverTester = new ServerTester()
    .addPluginJar(getFile("sonar-decoy-plugin-1.0.jar"))
    .setUpdateCenterUrl(getFileUrl("update-center.properties"));
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(serverTester);

  @Test
  public void test_update_existing_and_install_new_scenario() throws Exception {
    WsTester wsTester = new WsTester(serverTester.get(PluginsWs.class));

    // 1 - check what's installed, available and pending
    wsTester.newGetRequest("api/plugins", "installed").execute().assertJson("{" +
      "  \"plugins\": [" +
      "    {" +
      "      \"key\": \"decoy\"," +
      "      \"version\": \"1.0\"" +
      "    }" +
      "  ]" +
      "}"
      );

    wsTester.newGetRequest("api/plugins", "available").execute().assertJson("{" +
        "  \"plugins\": [" +
        "    {" +
        "      \"key\": \"foo\"," +
        "      \"release\": {" +
        "        \"version\": \"1.0\"," +
        "      }," +
        "      \"update\": {" +
        "        \"status\": \"COMPATIBLE\"," +
        "        \"requires\": []" +
        "      }" +
        "    }" +
        "  ]" +
        "}");

    wsTester.newGetRequest("api/plugins", "pending").execute().assertJson("{\"installing\":[],\"removing\":[]}");

    // 2 - login as admin and install one plugin, update another, verify pending status in the process
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    wsTester.newPostRequest("api/plugins", "update").setParam("key", "decoy").execute().assertNoContent();

    wsTester.newGetRequest("api/plugins", "pending").execute().assertJson("{" +
        "  \"installing\": [" +
        "    {" +
        "      \"key\": \"decoy\"," +
        "      \"version\": \"1.1\"" +
        "    }" +
        "  ]," +
        "  \"removing\": []" +
        "}");

    wsTester.newPostRequest("api/plugins", "install").setParam("key", "foo").execute().assertNoContent();

    wsTester.newGetRequest("api/plugins", "pending").execute().assertJson("{" +
        "  \"installing\": [" +
        "    {" +
        "      \"key\": \"decoy\"," +
        "      \"version\": \"1.1\"" +
        "    }," +
        "    {" +
        "      \"key\": \"foo\"," +
        "      \"version\": \"1.0\"" +
        "    }" +
        "  ]," +
        "  \"removing\": []" +
        "}");

    // 3 - simulate SQ restart
    wsTester = restartServerTester();

    // 4 - make sure plugin is installed
    wsTester.newGetRequest("api/plugins", "installed").execute().assertJson("{" +
      "  \"plugins\": [" +
      "    {" +
      "      \"key\": \"decoy\"," +
      "      \"version\": \"1.1\"" +
      "    }," +
      "    {" +
      "      \"key\": \"foo\"," +
      "      \"version\": \"1.0\"" +
      "    }" +
      "  ]" +
      "}"
      );

    // 5 - login as admin again and uninstall a plugin, verify pending status in the process
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    wsTester.newPostRequest("api/plugins", "uninstall").setParam("key", "foo").execute().assertNoContent();

    wsTester.newGetRequest("api/plugins", "pending").execute().assertJson("{" +
        "  \"installing\": []," +
        "  \"removing\": [" +
        "    {" +
        "      \"key\": \"foo\"," +
        "      \"version\": \"1.0\"" +
        "    }" +
        "  ]," +
        "}");

    // 6 - simulate SQ restart again
    wsTester = restartServerTester();

    // 7 - make sure plugin has been uninstalled
    wsTester.newGetRequest("api/plugins", "installed").execute().assertJson("{" +
            "  \"plugins\": [" +
            "    {" +
            "      \"key\": \"decoy\"," +
            "      \"version\": \"1.1\"" +
            "    }" +
            "  ]" +
            "}"
    );
  }

  private WsTester restartServerTester() {
    serverTester.restart();
    // correctly simulate a server restart (ie. user is disconnected)
    userSessionRule.anonymous();
    // must use a new WsTester to reference the right PluginWs instance
    return new WsTester(serverTester.get(PluginsWs.class));
  }

  private static File getFile(String jarFileName) {
    try {
      return new File(getFileUrl(jarFileName).toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static URL getFileUrl(String fileName) {
    return PluginsWsMediumTest.class.getResource(PluginsWsMediumTest.class.getSimpleName() + "/" + fileName);
  }
}
