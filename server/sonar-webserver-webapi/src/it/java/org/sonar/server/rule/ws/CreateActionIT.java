/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.common.rule.service.RuleService;
import org.sonar.server.common.text.MacroInterpreter;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.common.rule.RuleCreator;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newTemplateRule;
import static org.sonar.server.util.TypeValidationsTesting.newFullTypeValidations;
import static org.sonar.test.JsonAssert.assertJson;

public class CreateActionIT {

  private final System2 system2 = mock(System2.class);

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = EsTester.create();

  private final UuidFactory uuidFactory = new SequenceUuidFactory();

  private final WsActionTester ws = new WsActionTester(new CreateAction(db.getDbClient(),
    new RuleService(db.getDbClient(),
      new RuleCreator(system2, new RuleIndexer(es.client(), db.getDbClient()), db.getDbClient(), newFullTypeValidations(), uuidFactory)),
    new RuleMapper(new Languages(), createMacroInterpreter(), new RuleDescriptionFormatter()),
    new RuleWsSupport(db.getDbClient(), userSession)));

  @Test
  public void check_definition() {
    assertThat(ws.getDef().isPost()).isTrue();
    assertThat(ws.getDef().isInternal()).isFalse();
    assertThat(ws.getDef().responseExampleAsString()).isNotNull();
    assertThat(ws.getDef().description()).isNotNull();
  }

  @Test
  public void create_custom_rule() {
    logInAsQProfileAdministrator();
    // Template rule
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"))
      .setType(BUG)
      .setTags(Set.of())
      .setLanguage("js")
      .setSystemTags(Set.of("systag1", "systag2"));
    db.rules().insert(templateRule);
    db.rules().insertRuleParam(templateRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*"));

    String result = ws.newRequest()
      .setParam("customKey", "MY_CUSTOM")
      .setParam("templateKey", templateRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdownDescription", "Description")
      .setParam("severity", "MAJOR")
      .setParam("status", "BETA")
      .setParam("type", BUG.name())
      .setParam("params", "regex=a.*")
      .execute().getInput();

    String expectedResult = """
      {
        "rule": {
          "key": "java:MY_CUSTOM",
          "repo": "java",
          "name": "My custom rule",
          "htmlDesc": "Description",
          "severity": "MAJOR",
          "status": "BETA",
          "type": "BUG",
          "internalKey": "configKey_S001",
          "isTemplate": false,
          "templateKey": "java:S001",
          "sysTags": [
            "systag1",
            "systag2"
          ],
          "lang": "js",
          "params": [
            {
              "key": "regex",
              "htmlDesc": "Reg ex",
              "defaultValue": "a.*",
              "type": "STRING"
            }
          ],
          "cleanCodeAttribute": "CONVENTIONAL",
          "cleanCodeAttributeCategory": "CONSISTENT",
          "impacts": [
            {
              "softwareQuality": "RELIABILITY",
              "severity": "MEDIUM"
            }
          ]
        }
      }
      """;

    assertJson(result).isSimilarTo(expectedResult);
  }

  @Test
  public void create_shouldSetCleanCodeAttributeAndImpacts() {
    logInAsQProfileAdministrator();
    // Template rule
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"))
      .setType(BUG)
      .setLanguage("js");
    db.rules().insert(templateRule);

    String result = ws.newRequest()
      .setParam("customKey", "MY_CUSTOM")
      .setParam("templateKey", templateRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdownDescription", "Description")
      .setParam("status", "BETA")
      .setParam("cleanCodeAttribute", "MODULAR")
      .setParam("impacts", "RELIABILITY=HIGH;SECURITY=LOW")
      .execute().getInput();

    String expectedResult = """
      {
        "rule": {
          "key": "java:MY_CUSTOM",
          "repo": "java",
          "name": "My custom rule",
          "htmlDesc": "Description",
          "severity": "MINOR",
          "status": "BETA",
          "type": "VULNERABILITY",
          "internalKey": "configKey_S001",
          "isTemplate": false,
          "templateKey": "java:S001",
          "lang": "js",
          "cleanCodeAttribute": "MODULAR",
          "cleanCodeAttributeCategory": "ADAPTABLE",
          "impacts": [
            {
              "softwareQuality": "RELIABILITY",
              "severity": "HIGH"
            },
            {
              "softwareQuality": "SECURITY",
              "severity": "LOW"
            }
          ]
        }
      }
      """;

    assertJson(result).isSimilarTo(expectedResult);
  }

  @Test
  public void create_custom_rule_with_preventReactivation_param_to_true() {
    logInAsQProfileAdministrator();
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule);
    // insert a removed rule
    RuleDto customRule = newCustomRule(templateRule, "Description")
      .setRuleKey("MY_CUSTOM")
      .setStatus(RuleStatus.REMOVED)
      .setName("My custom rule")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .setSeverity(Severity.MAJOR);
    db.rules().insert(customRule);

    TestResponse response = ws.newRequest()
      .setParam("customKey", "MY_CUSTOM")
      .setParam("templateKey", templateRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdownDescription", "Description")
      .setParam("severity", "MAJOR")
      .setParam("preventReactivation", "true")
      .execute();

    assertThat(response.getStatus()).isEqualTo(409);
    assertJson(response.getInput()).isSimilarTo("{\n" +
      "  \"rule\": {\n" +
      "    \"key\": \"java:MY_CUSTOM\",\n" +
      "    \"repo\": \"java\",\n" +
      "    \"name\": \"My custom rule\",\n" +
      "    \"htmlDesc\": \"Description\",\n" +
      "    \"severity\": \"MAJOR\",\n" +
      "    \"status\": \"REMOVED\",\n" +
      "    \"isTemplate\": false\n" +
      "  }\n" +
      "}\n");
  }

  @Test
  public void create_custom_rule_of_non_existing_template_should_fail() {
    logInAsQProfileAdministrator();

    TestRequest request = ws.newRequest()
      .setParam("customKey", "MY_CUSTOM")
      .setParam("templateKey", "non:existing")
      .setParam("name", "My custom rule")
      .setParam("markdownDescription", "Description")
      .setParam("severity", "MAJOR")
      .setParam("preventReactivation", "true");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The template key doesn't exist: non:existing");
  }

  @Test
  public void create_custom_rule_of_removed_template_should_fail() {
    logInAsQProfileAdministrator();

    RuleDto templateRule = db.rules().insert(r -> r.setIsTemplate(true).setStatus(RuleStatus.REMOVED));

    TestRequest request = ws.newRequest()
      .setParam("customKey", "MY_CUSTOM")
      .setParam("templateKey", templateRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdownDescription", "Description")
      .setParam("severity", "MAJOR")
      .setParam("preventReactivation", "true");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The template key doesn't exist: " + templateRule.getKey());
  }

  @Test
  public void throw_IllegalArgumentException_if_status_is_removed() {
    logInAsQProfileAdministrator();

    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"));

    TestRequest request = ws.newRequest()
      .setParam("customKey", "MY_CUSTOM")
      .setParam("templateKey", templateRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdownDescription", "Description")
      .setParam("severity", "MAJOR")
      .setParam("status", "REMOVED")
      .setParam("preventReactivation", "true");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of parameter 'status' (REMOVED) must be one of: [BETA, DEPRECATED, READY]");
  }

  @Test
  public void severity_set_to_default() {
    logInAsQProfileAdministrator();

    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule);

    String result = ws.newRequest()
      .setParam("customKey", "MY_CUSTOM")
      .setParam("templateKey", templateRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdownDescription", "Description")
      .setParam("status", "BETA")
      .setParam("type", BUG.name())
      .execute().getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"rule\": {\n" +
      "    \"severity\": \"MAJOR\"" +
      "  }\n" +
      "}\n");
  }

  @Test
  public void throw_ForbiddenException_if_not_profile_administrator() {
    userSession.logIn();

    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(UnauthorizedException.class);
  }

  private static MacroInterpreter createMacroInterpreter() {
    MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
    doAnswer(returnsFirstArg()).when(macroInterpreter).interpret(anyString());
    return macroInterpreter;
  }

  private void logInAsQProfileAdministrator() {
    userSession
      .logIn()
      .addPermission(ADMINISTER_QUALITY_PROFILES);
  }

}
