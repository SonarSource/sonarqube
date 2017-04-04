/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.rule.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsAction;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.rule.RuleStatus.DEPRECATED;
import static org.sonar.api.rule.RuleStatus.READY;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.api.rule.Severity.MAJOR;
import static org.sonar.api.rule.Severity.MINOR;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.server.ws.WebService.Param.ASCENDING;
import static org.sonar.api.server.ws.WebService.Param.SORT;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.qualityprofile.ActiveRuleDto.INHERITED;
import static org.sonar.db.qualityprofile.ActiveRuleDto.OVERRIDES;
import static org.sonar.server.rule.ws.SearchAction.defineRuleSearchParameters;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TYPES;

public class RuleQueryFactoryTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  DbSession dbSession = dbTester.getSession();

  RuleQueryFactory underTest = new RuleQueryFactory(dbClient, new RuleWsSupport(dbClient, null, TestDefaultOrganizationProvider.from(dbTester)));

  FakeAction fakeAction = new FakeAction(underTest);
  OrganizationDto organization = OrganizationTesting.newOrganizationDto();

  @Test
  public void create_empty_query() throws Exception {
    RuleQuery result = execute();

    assertThat(result.getKey()).isNull();

    assertThat(result.getActivation()).isNull();
    assertThat(result.getActiveSeverities()).isNull();
    assertThat(result.isAscendingSort()).isTrue();
    assertThat(result.getAvailableSinceLong()).isNull();
    assertThat(result.getInheritance()).isNull();
    assertThat(result.isTemplate()).isNull();
    assertThat(result.getLanguages()).isNull();
    assertThat(result.getQueryText()).isNull();
    assertThat(result.getQProfileKey()).isNull();
    assertThat(result.getRepositories()).isNull();
    assertThat(result.getRuleKey()).isNull();
    assertThat(result.getSeverities()).isNull();
    assertThat(result.getStatuses()).isEmpty();
    assertThat(result.getTags()).isNull();
    assertThat(result.templateKey()).isNull();
    assertThat(result.getTypes()).isEmpty();

    assertThat(result.getSortField()).isNull();
  }

  @Test
  public void create_query() throws Exception {
    RuleQuery result = execute(
      PARAM_RULE_KEY, "ruleKey",

      PARAM_ACTIVATION, "true",
      PARAM_ACTIVE_SEVERITIES, "MINOR,MAJOR",
      PARAM_AVAILABLE_SINCE, "2016-01-01",
      PARAM_INHERITANCE, "INHERITED,OVERRIDES",
      PARAM_IS_TEMPLATE, "true",
      PARAM_LANGUAGES, "java,js",
      TEXT_QUERY, "S001",
      PARAM_QPROFILE, "sonar-way",
      PARAM_REPOSITORIES, "pmd,checkstyle",
      PARAM_SEVERITIES, "MINOR,CRITICAL",
      PARAM_STATUSES, "DEPRECATED,READY",
      PARAM_TAGS, "tag1,tag2",
      PARAM_TEMPLATE_KEY, "architectural",
      PARAM_TYPES, "CODE_SMELL,BUG",

      SORT, "updatedAt",
      ASCENDING, "false");

    assertThat(result.getKey()).isEqualTo("ruleKey");

    assertThat(result.getActivation()).isTrue();
    assertThat(result.getActiveSeverities()).containsOnly(MINOR, MAJOR);
    assertThat(result.isAscendingSort()).isFalse();
    assertThat(result.getAvailableSinceLong()).isNotNull();
    assertThat(result.getInheritance()).containsOnly(INHERITED, OVERRIDES);
    assertThat(result.isTemplate()).isTrue();
    assertThat(result.getLanguages()).containsOnly("java", "js");
    assertThat(result.getQueryText()).isEqualTo("S001");
    assertThat(result.getQProfileKey()).isEqualTo("sonar-way");
    assertThat(result.getRepositories()).containsOnly("pmd", "checkstyle");
    assertThat(result.getRuleKey()).isNull();
    assertThat(result.getSeverities()).containsOnly(MINOR, CRITICAL);
    assertThat(result.getStatuses()).containsOnly(DEPRECATED, READY);
    assertThat(result.getTags()).containsOnly("tag1", "tag2");
    assertThat(result.templateKey()).isEqualTo("architectural");
    assertThat(result.getTypes()).containsOnly(BUG, CODE_SMELL);

    assertThat(result.getSortField()).isEqualTo("updatedAt");
  }

  @Test
  public void create_query_add_language_from_profile() throws Exception {
    String profileKey = "sonar-way";
    dbClient.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(profileKey)
      .setOrganizationUuid(organization.getUuid())
      .setName("Sonar Way")
      .setLanguage("xoo"));
    dbSession.commit();

    RuleQuery result = execute(
      PARAM_QPROFILE, profileKey,
      PARAM_LANGUAGES, "java,js");

    assertThat(result.getQProfileKey()).isEqualTo(profileKey);
    assertThat(result.getLanguages()).containsOnly("xoo");
  }

  private RuleQuery execute(String... paramsKeyAndValue) {
    WsActionTester ws = new WsActionTester(fakeAction);
    TestRequest request = ws.newRequest();
    for (int i = 0; i < paramsKeyAndValue.length; i += 2) {
      request.setParam(paramsKeyAndValue[i], paramsKeyAndValue[i + 1]);
    }
    request.execute();
    return fakeAction.getRuleQuery();
  }

  private class FakeAction implements WsAction {

    private final RuleQueryFactory ruleQueryFactory;
    private RuleQuery ruleQuery;

    private FakeAction(RuleQueryFactory ruleQueryFactory) {
      this.ruleQueryFactory = ruleQueryFactory;
    }

    @Override
    public void define(WebService.NewController controller) {
      WebService.NewAction action = controller.createAction("fake")
        .setHandler(this);
      defineRuleSearchParameters(action);
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
      ruleQuery = ruleQueryFactory.createRuleQuery(dbTester.getSession(), request);
    }

    RuleQuery getRuleQuery() {
      return ruleQuery;
    }
  }
}
