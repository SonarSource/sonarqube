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
package org.sonarqube.tests.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.json.JSONException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.ItUtils.xooPlugin;

/**
 * Checks the feature of compressing plugins with pack200 and making them available for the scanners.
 */
public class CompressPluginsTest {
  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .setServerProperty("sonar.pluginsCompression.enable", "true")
    .build();

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void setUp() {
    orchestrator.resetData();
  }

  @Test
  public void dont_fail_analysis() {
    SonarScanner scanner = SonarScanner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(scanner);
  }

  @Test
  public void plugins_installed_ws_should_expose_compressed_plugin() {
    WsResponse response = tester.wsClient().wsConnector().call(new GetRequest("api/plugins/installed"));
    String content = response.content();
    JsonParser parser = new JsonParser();
    JsonObject root = parser.parse(content).getAsJsonObject();
    JsonArray plugins = root.getAsJsonArray("plugins");
    plugins.forEach(p -> {
      assertThat(p.getAsJsonObject().has("compressedHash")).isTrue();
      assertThat(p.getAsJsonObject().has("compressedFilename")).isTrue();
    });
  }
}
