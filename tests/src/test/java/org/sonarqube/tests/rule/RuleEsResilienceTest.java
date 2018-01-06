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
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Byteman;
import org.sonarqube.ws.client.rules.CreateRequest;
import org.sonarqube.ws.client.rules.SearchRequest;
import util.ItUtils;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonarqube.tests.Byteman.Process.WEB;

public class RuleEsResilienceTest {

  @ClassRule
  public static final Orchestrator orchestrator;
  private static final Byteman byteman;

  static {
    byteman = new Byteman(Orchestrator.builderEnv(), WEB);
    orchestrator = byteman
      .getOrchestratorBuilder()
      .setServerProperty("sonar.search.recovery.delayInMs", "1000")
      .setServerProperty("sonar.search.recovery.minAgeInMs", "3000")
      .addPlugin(ItUtils.xooPlugin())
      .build();
  }

  @Before
  public void before() throws Exception {
    byteman.activateScript("resilience/rule_indexer.btm");
  }

  @After
  public void after() throws Exception {
    byteman.deactivateAllRules();
  }

  @Rule
  public TestRule timeout = new DisableOnDebug(Timeout.builder()
    .withLookingForStuckThread(true)
    .withTimeout(60L, TimeUnit.SECONDS)
    .build());

  @Rule
  public Tester tester = new Tester(orchestrator)
    // custom rules are not supported when organizations are enabled
    .disableOrganizations();

  @Test
  public void creation_of_custom_rule_is_resilient_to_elasticsearch_errors() throws Exception {
    CreateRequest request = new CreateRequest()
      .setCustomKey("my_custom_rule")
      .setName("My custom rule")
      .setTemplateKey("xoo:xoo-template")
      .setMarkdownDescription("The *initial* rule")
      .setSeverity("MAJOR");
    tester.wsClient().rules().create(request);

    // rule exists in db but is not indexed. Search returns no results.
    assertThat(nameFoundInSearch("initial rule")).isFalse();

    // rule already exists in db, can't be created twice
    ItUtils.expectHttpError(400, () -> tester.wsClient().rules().create(request));

    while (!nameFoundInSearch("initial rule")) {
      // rule is indexed by the recovery daemon, which runs every 3 seconds
      Thread.sleep(500L);
    }
  }

  private boolean nameFoundInSearch(String query) {
    SearchRequest request = new SearchRequest()
      .setQ(query)
      .setRepositories(singletonList("xoo"));
    return tester.wsClient().rules().search(request).getRulesCount() > 0;
  }
}
