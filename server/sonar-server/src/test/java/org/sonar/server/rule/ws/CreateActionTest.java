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
import org.sonar.api.config.MapSettings;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.organization.TestDefaultOrganizationProvider;
import org.sonar.server.organization.TestOrganizationFlags;
import org.sonar.server.rule.RuleCreator;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newTemplateRule;
import static org.sonar.server.util.TypeValidationsTesting.newFullTypeValidations;
import static org.sonar.test.JsonAssert.assertJson;

public class CreateActionTest {

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = new EsTester(new RuleIndexDefinition(new MapSettings()));

  private TestOrganizationFlags organizationFlags = TestOrganizationFlags.standalone();

  private WsActionTester wsTester = new WsActionTester(new CreateAction(db.getDbClient(),
    new RuleCreator(system2, new RuleIndexer(es.client(), db.getDbClient()), db.getDbClient(), newFullTypeValidations(),
      TestDefaultOrganizationProvider.from(db)),
    new RuleMapper(new Languages(), createMacroInterpreter()), organizationFlags));

  @Test
  public void create_custom_rule() {
    // Template rule
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"), db.getDefaultOrganization());
    db.rules().insert(templateRule.getDefinition());
    db.rules().insertOrUpdateMetadata(templateRule.getMetadata().setRuleId(templateRule.getId()));
    db.rules().insertRuleParam(templateRule.getDefinition(), param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*"));

    String result = wsTester.newRequest()
      .setParam("custom_key", "MY_CUSTOM")
      .setParam("template_key", templateRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "Description")
      .setParam("severity", "MAJOR")
      .setParam("status", "BETA")
      .setParam("params", "regex=a.*")
      .execute().getInput();

    assertJson(result).isSimilarTo("{\n" +
      "  \"rule\": {\n" +
      "    \"key\": \"java:MY_CUSTOM\",\n" +
      "    \"repo\": \"java\",\n" +
      "    \"name\": \"My custom rule\",\n" +
      "    \"htmlDesc\": \"Description\",\n" +
      "    \"severity\": \"MAJOR\",\n" +
      "    \"status\": \"BETA\",\n" +
      "    \"internalKey\": \"InternalKeyS001\",\n" +
      "    \"isTemplate\": false,\n" +
      "    \"templateKey\": \"java:S001\",\n" +
      "    \"sysTags\": [\"systag1\", \"systag2\"],\n" +
      "    \"lang\": \"js\",\n" +
      "    \"params\": [\n" +
      "      {\n" +
      "        \"key\": \"regex\",\n" +
      "        \"htmlDesc\": \"Reg ex\",\n" +
      "        \"defaultValue\": \"a.*\",\n" +
      "        \"type\": \"STRING\"\n" +
      "      }\n" +
      "    ]\n" +
      "  }\n" +
      "}\n");
  }

  @Test
  public void create_custom_rule_with_prevent_reactivation_param_to_true() {
    RuleDefinitionDto templateRule = newTemplateRule(RuleKey.of("java", "S001")).getDefinition();
    db.rules().insert(templateRule);
    // insert a removed rule
    RuleDefinitionDto customRule = newCustomRule(templateRule)
      .setRuleKey("MY_CUSTOM")
      .setStatus(RuleStatus.REMOVED)
      .setName("My custom rule")
      .setDescription("Description")
      .setDescriptionFormat(RuleDto.Format.MARKDOWN)
      .setSeverity(Severity.MAJOR);
    db.rules().insert(customRule);

    TestResponse response = wsTester.newRequest()
      .setParam("custom_key", "MY_CUSTOM")
      .setParam("template_key", templateRule.getKey().toString())
      .setParam("name", "My custom rule")
      .setParam("markdown_description", "Description")
      .setParam("severity", "MAJOR")
      .setParam("prevent_reactivation", "true")
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

  private static MacroInterpreter createMacroInterpreter() {
    MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
    doAnswer(returnsFirstArg()).when(macroInterpreter).interpret(anyString());
    return macroInterpreter;
  }

}
