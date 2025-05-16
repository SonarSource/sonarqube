/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.common.text.MacroInterpreter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class ListActionIT {

  private static final String RULE_KEY_1 = "java:S001";
  private static final String RULE_KEY_2 = "java:S002";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @org.junit.Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final Languages languages = LanguageTesting.newLanguages("java", "js");
  private final MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  private final RuleMapper ruleMapper = new RuleMapper(languages, macroInterpreter, new RuleDescriptionFormatter());
  private final RuleWsSupport ruleWsSupport = new RuleWsSupport(db.getDbClient(), userSession);
  private final RulesResponseFormatter rulesResponseFormatter = new RulesResponseFormatter(db.getDbClient(), ruleWsSupport, ruleMapper, languages);
  private final WsActionTester ws = new WsActionTester(new ListAction(db.getDbClient(), rulesResponseFormatter, ruleWsSupport));

  @Test
  public void define_shouldDefineParameters() {
    WebService.Action def = ws.getDef();
    assertThat(def.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder("asc", "p", "s", "ps", "available_since", "qprofile");
  }

  @Test
  public void execute_shouldReturnRules() {
    List<RuleDto> rules = List.of(
      db.rules().insert(RuleTesting.newRule(RuleKey.parse(RULE_KEY_1)).setConfigKey(null).setName(null)),
      db.rules().insert(RuleTesting.newRule(RuleKey.parse(RULE_KEY_2)).setConfigKey("I002").setName("Rule Two")));
    db.getSession().commit();

    Rules.ListResponse listResponse = ws.newRequest()
      .executeProtobuf(Rules.ListResponse.class);

    assertThat(listResponse.getRulesCount()).isEqualTo(2);

    Rules.Rule ruleS001 = getRule(listResponse, RULE_KEY_1);
    assertThat(ruleS001.getKey()).isEqualTo(RULE_KEY_1);
    assertThat(ruleS001.getInternalKey()).isEmpty();
    assertThat(ruleS001.getName()).isEmpty();

    Rules.Rule ruleS002 = getRule(listResponse, RULE_KEY_2);
    assertThat(ruleS002.getKey()).isEqualTo(RULE_KEY_2);
    assertThat(ruleS002.getInternalKey()).isEqualTo("I002");
    assertThat(ruleS002.getName()).isEqualTo("Rule Two");

    assertThat(listResponse.getRulesList()).extracting(Rules.Rule::getKey).containsExactly(
      rules.stream().sorted(Comparator.comparing(RuleDto::getUuid)).map(rule -> rule.getKey().toString()).toArray(String[]::new));
  }

  @Test
  public void execute_whenSortingDefined_shouldReturnSortedRules() {
    db.rules().insert(RuleTesting.newRule(RuleKey.parse(RULE_KEY_1)).setCreatedAt(2_000_000L));
    db.rules().insert(RuleTesting.newRule(RuleKey.parse(RULE_KEY_2)).setCreatedAt(1_000_000L));
    db.getSession().commit();

    Rules.ListResponse listResponse = ws.newRequest()
      .setParam(WebService.Param.SORT, "createdAt")
      .setParam(WebService.Param.ASCENDING, "true")
      .executeProtobuf(Rules.ListResponse.class);

    assertThat(listResponse.getRulesCount()).isEqualTo(2);
    assertThat(listResponse.getRulesList()).extracting(Rules.Rule::getKey).containsExactly(RULE_KEY_2, RULE_KEY_1);
  }

  @Test
  public void execute_shouldReturnFilteredRules_whenQProfileDefined() {
    QProfileDto profile = db.qualityProfiles().insert(p -> p.setLanguage("java"));
    RuleDto rule = db.rules().insert(r -> r.setRuleKey(RuleKey.parse(RULE_KEY_1)));

    db.qualityProfiles().activateRule(profile, rule);

    Rules.ListResponse result = ws.newRequest()
      .setParam("qprofile", profile.getKee())
      .executeProtobuf(Rules.ListResponse.class);

    assertThat(result.getPaging().getTotal()).isOne();
    assertThat(result.getPaging().getPageIndex()).isOne();
    assertThat(result.getRulesCount()).isOne();
    assertThat(result.getActives()).isNotNull();

    Map<String, Rules.ActiveList> activeRules = result.getActives().getActivesMap();
    assertThat(activeRules.get(rule.getKey().toString())).isNotNull();
    assertThat(activeRules.get(rule.getKey().toString()).getActiveListList()).hasSize(1);
  }

  @Test
  public void execute_shouldReturnFilteredRules_whenAvailableSinceDefined() throws ParseException {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    long recentDay = dateFormat.parse("2023-02-20").getTime();
    long oldDay = dateFormat.parse("2022-09-17").getTime();
    db.rules().insert(r -> r.setRuleKey(RuleKey.parse(RULE_KEY_1)).setCreatedAt(recentDay));
    db.rules().insert(r -> r.setRuleKey(RuleKey.parse(RULE_KEY_2)).setCreatedAt(oldDay));

    Rules.ListResponse result = ws.newRequest()
      .setParam("available_since", "2022-11-23")
      .executeProtobuf(Rules.ListResponse.class);

    assertThat(result.getRulesCount()).isOne();
    assertThat(result.getRulesList().stream().map(Rules.Rule::getKey)).containsOnly(RULE_KEY_1);
  }

  @Test
  public void execute_shouldFailWithNotFoundException_whenQProfileDoesNotExist() {
    String unknownProfile = "unknown_profile";
    TestRequest request = ws.newRequest()
      .setParam("qprofile", unknownProfile);

    assertThatThrownBy(() -> request.executeProtobuf(Rules.SearchResponse.class))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("The specified qualityProfile '" + unknownProfile + "' does not exist");
  }

  private Rules.Rule getRule(Rules.ListResponse listResponse, String ruleKey) {
    Optional<Rules.Rule> rule = listResponse.getRulesList().stream()
      .filter(r -> ruleKey.equals(r.getKey()))
      .findFirst();
    assertThat(rule).isPresent();
    return rule.get();
  }
}
