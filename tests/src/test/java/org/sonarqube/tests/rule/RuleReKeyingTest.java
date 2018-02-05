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
package org.sonarqube.tests.rule;

import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import org.junit.After;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.PostRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;

public class RuleReKeyingTest {

  private static Orchestrator orchestrator;
  private static Tester tester;

  @After
  public void tearDown() {
    if (tester != null) {
      tester.after();
    }
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void rules_are_re_keyed_when_upgrading_and_downgrading_plugin() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("foo-plugin-v1"))
      .build();
    orchestrator.start();

    tester = new Tester(orchestrator);
    tester.before();

    verifyRuleCount(16, 16);

    // uninstall plugin V1
    tester.wsClient().wsConnector().call(new PostRequest("api/plugins/uninstall").setParam("key", "foo")).failIfNotSuccessful();
    // install plugin V2
    File pluginsDir = new File(orchestrator.getServer().getHome() + "/extensions/plugins");
    orchestrator.getConfiguration().fileSystem().copyToDirectory(pluginArtifact("foo-plugin-v2"), pluginsDir);

    orchestrator.restartServer();

    // one rule deleted, one rule added, two rules re-keyed
    verifyRuleCount(16, 17);

    // uninstall plugin V2
    tester.wsClient().wsConnector().call(new PostRequest("api/plugins/uninstall").setParam("key", "foo")).failIfNotSuccessful();
    // install plugin V1
    orchestrator.getConfiguration().fileSystem().copyToDirectory(pluginArtifact("foo-plugin-v1"), pluginsDir);

    orchestrator.restartServer();

    // new rule removed, removed rule recreated, two rules re-keyed back
    verifyRuleCount(16, 17);
  }

  private void verifyRuleCount(int wsRuleCount, int dbRuleCount) {
    assertThat(tester.wsClient().rules().list().getRulesList()).hasSize(wsRuleCount);
    assertThat(orchestrator.getDatabase().countSql("select count(*) from rules")).isEqualTo(dbRuleCount);
  }
}
