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

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDescriptionSectionContextDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.text.MacroInterpreter;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;
import org.sonarqube.ws.Rules.Rule;
import org.sonarqube.ws.Rules.ShowResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ASSESS_THE_PROBLEM_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.HOW_TO_FIX_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.RESOURCES_SECTION_KEY;
import static org.sonar.api.server.rule.RuleDescriptionSection.RuleDescriptionSectionKeys.ROOT_CAUSE_SECTION_KEY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.DEFAULT_KEY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.db.rule.RuleDto.Format.MARKDOWN;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.newRuleWithoutDescriptionSection;
import static org.sonar.db.rule.RuleTesting.newTemplateRule;
import static org.sonar.db.rule.RuleTesting.setTags;
import static org.sonar.server.language.LanguageTesting.newLanguage;
import static org.sonar.server.rule.ws.ShowAction.PARAM_ACTIVES;
import static org.sonar.server.rule.ws.ShowAction.PARAM_KEY;
import static org.sonarqube.ws.Common.RuleType.UNKNOWN;
import static org.sonarqube.ws.Common.RuleType.VULNERABILITY;

public class ShowActionIT {

  private static final String INTERPRETED = "interpreted";

  @org.junit.Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @org.junit.Rule
  public DbTester db = DbTester.create();

  private final UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private final MacroInterpreter macroInterpreter = mock(MacroInterpreter.class);
  private final Languages languages = new Languages(newLanguage("xoo", "Xoo"));
  private final RuleWsSupport ruleWsSupport = new RuleWsSupport(db.getDbClient(), userSession);
  private final RuleMapper ruleMapper = new RuleMapper(languages, macroInterpreter, new RuleDescriptionFormatter());
  private final WsActionTester ws = new WsActionTester(
    new ShowAction(db.getDbClient(), new RulesResponseFormatter(db.getDbClient(), ruleWsSupport, ruleMapper, languages)));
  private UserDto userDto;

  @Before
  public void before() {
    userDto = db.users().insertUser();
    doReturn(INTERPRETED).when(macroInterpreter).interpret(anyString());
  }

  @Test
  public void show_rule_key() {
    RuleDto rule = db.rules().insert(r -> r.setNoteUserUuid(userDto.getUuid()));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule()).extracting(Rule::getKey).isEqualTo(rule.getKey().toString());
  }

  @Test
  public void show_rule_with_basic_info() {
    RuleDto rule = db.rules().insert(r -> r.setNoteUserUuid(userDto.getUuid()));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getKey()).isEqualTo(rule.getKey().toString());
    assertThat(resultRule.getRepo()).isEqualTo(rule.getRepositoryKey());
    assertThat(resultRule.getName()).isEqualTo(rule.getName());
    assertThat(resultRule.getSeverity()).isEqualTo(rule.getSeverityString());
    assertThat(resultRule.getStatus()).hasToString(rule.getStatus().toString());
    assertThat(resultRule.getInternalKey()).isEqualTo(rule.getConfigKey());
    assertThat(resultRule.getIsTemplate()).isEqualTo(rule.isTemplate());
    assertThat(resultRule.getLang()).isEqualTo(rule.getLanguage());
    assertThat(resultRule.getParams().getParamsList())
      .extracting(Rule.Param::getKey, Rule.Param::getHtmlDesc, Rule.Param::getDefaultValue)
      .containsExactlyInAnyOrder(tuple(ruleParam.getName(), ruleParam.getDescription(), ruleParam.getDefaultValue()));
    assertThat(resultRule.getImpacts().getImpactsList())
      .extracting(Common.Impact::getSoftwareQuality, Common.Impact::getSeverity)
      .containsExactly(tuple(Common.SoftwareQuality.MAINTAINABILITY, Common.ImpactSeverity.HIGH));

    assertThat(resultRule.getEducationPrinciples().getEducationPrinciplesList()).containsExactlyElementsOf(rule.getEducationPrinciples());
  }

  @Test
  public void show_rule_tags() {
    RuleDto rule = db.rules().insert(setTags("tag1", "tag2"), r -> r.setNoteData(null).setNoteUserUuid(null));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule().getTags().getTagsList())
      .containsExactly(rule.getTags().toArray(new String[0]));
  }

  @Test
  public void returnRuleCleanCodeFields_whenEndpointIsCalled() {
    RuleDto rule = db.rules().insert(setTags("tag1", "tag2"), r -> r.setNoteData(null).setNoteUserUuid(null));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule().getCleanCodeAttribute()).isEqualTo(Common.CleanCodeAttribute.CLEAR);
    assertThat(result.getRule().getCleanCodeAttributeCategory()).isEqualTo(Common.CleanCodeAttributeCategory.INTENTIONAL);
  }

  @Test
  public void show_rule_with_note_login() {
    UserDto user = db.users().insertUser();
    RuleDto rule = db.rules().insert(r -> r.setNoteUserUuid(user.getUuid()));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule().getNoteLogin()).isEqualTo(user.getLogin());
  }

  @Test
  public void show_rule_with_default_debt_infos() {
    RuleDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("gap desc")
      .setNoteUserUuid(userDto.getUuid())
      .setRemediationFunction(null)
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort(null));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getDefaultRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getDefaultRemFnGapMultiplier()).isEqualTo("5d");
    assertThat(resultRule.getDefaultRemFnBaseEffort()).isEqualTo("10h");
    assertThat(resultRule.getGapDescription()).isEqualTo("gap desc");
    assertThat(resultRule.getRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getRemFnGapMultiplier()).isEqualTo("5d");
    assertThat(resultRule.getRemFnBaseEffort()).isEqualTo("10h");
    assertThat(resultRule.getRemFnOverloaded()).isFalse();
    assertThat(resultRule.hasDeprecatedKeys()).isFalse();
  }

  @Test
  public void show_rule_with_only_overridden_debt() {
    RuleDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction(null)
      .setDefRemediationGapMultiplier(null)
      .setDefRemediationBaseEffort(null)
      .setNoteData(null)
      .setNoteUserUuid(null)
      .setRemediationFunction("LINEAR_OFFSET")
      .setRemediationGapMultiplier("5d")
      .setRemediationBaseEffort("10h"));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.hasDefaultRemFnType()).isFalse();
    assertThat(resultRule.hasDefaultRemFnGapMultiplier()).isFalse();
    assertThat(resultRule.hasDefaultRemFnBaseEffort()).isFalse();
    assertThat(resultRule.getRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getRemFnGapMultiplier()).isEqualTo("5d");
    assertThat(resultRule.getRemFnBaseEffort()).isEqualTo("10h");
    assertThat(resultRule.getRemFnOverloaded()).isTrue();
    assertThat(resultRule.hasDeprecatedKeys()).isFalse();
  }

  @Test
  public void show_rule_with_default_and_overridden_debt_infos() {
    RuleDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setNoteData(null)
      .setNoteUserUuid(null)
      .setRemediationFunction("CONSTANT_ISSUE")
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort("15h"));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getDefaultRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getDefaultRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getDefaultRemFnGapMultiplier()).isEqualTo("5d");
    assertThat(resultRule.getDefaultRemFnBaseEffort()).isEqualTo("10h");
    assertThat(resultRule.getRemFnType()).isEqualTo("CONSTANT_ISSUE");
    assertThat(resultRule.hasRemFnGapMultiplier()).isFalse();
    assertThat(resultRule.getRemFnBaseEffort()).isEqualTo("15h");
    assertThat(resultRule.getRemFnOverloaded()).isTrue();
    assertThat(resultRule.hasDeprecatedKeys()).isFalse();
  }

  @Test
  public void show_rule_with_no_default_and_no_overridden_debt() {
    RuleDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction(null)
      .setDefRemediationGapMultiplier(null)
      .setDefRemediationBaseEffort(null)
      .setNoteData(null)
      .setNoteUserUuid(null)
      .setRemediationFunction(null)
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort(null));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.hasDefaultRemFnType()).isFalse();
    assertThat(resultRule.hasDefaultRemFnGapMultiplier()).isFalse();
    assertThat(resultRule.hasDefaultRemFnBaseEffort()).isFalse();
    assertThat(resultRule.hasRemFnType()).isFalse();
    assertThat(resultRule.hasRemFnGapMultiplier()).isFalse();
    assertThat(resultRule.hasRemFnBaseEffort()).isFalse();
    assertThat(resultRule.getRemFnOverloaded()).isFalse();
    assertThat(resultRule.hasDeprecatedKeys()).isFalse();
  }

  @Test
  public void show_deprecated_rule_debt_fields() {
    RuleDto rule = db.rules().insert(r -> r
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription("gap desc")
      .setNoteData(null)
      .setNoteUserUuid(null)
      .setRemediationFunction("CONSTANT_ISSUE")
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort("15h"));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getDefaultRemFnType()).isEqualTo("LINEAR_OFFSET");
    assertThat(resultRule.getDefaultRemFnGapMultiplier()).isEqualTo("5d");
    assertThat(resultRule.getDefaultRemFnBaseEffort()).isEqualTo("10h");
    assertThat(resultRule.getGapDescription()).isEqualTo("gap desc");
    assertThat(resultRule.getDebtRemFnType()).isEqualTo("CONSTANT_ISSUE");
    assertThat(resultRule.hasRemFnGapMultiplier()).isFalse();
    assertThat(resultRule.getRemFnBaseEffort()).isEqualTo("15h");
    assertThat(resultRule.getRemFnOverloaded()).isTrue();
    assertThat(resultRule.hasDeprecatedKeys()).isFalse();
  }

  @Test
  public void encode_html_description_of_custom_rule() {
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule);

    RuleDto customRule = newCustomRule(templateRule, "<div>line1\nline2</div>")
      .setDescriptionFormat(MARKDOWN)
      .setNoteUserUuid(userDto.getUuid());
    db.rules().insert(customRule);
    doReturn("&lt;div&gt;line1<br/>line2&lt;/div&gt;").when(macroInterpreter).interpret("<div>line1\nline2</div>");

    ShowResponse result = ws.newRequest()
      .setParam("key", customRule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getRule().getHtmlDesc()).isEqualTo(INTERPRETED);
    assertThat(result.getRule().getTemplateKey()).isEqualTo(templateRule.getKey().toString());
    verify(macroInterpreter, times(2)).interpret("&lt;div&gt;line1<br/>line2&lt;/div&gt;");
  }

  @Test
  public void show_external_rule() {
    RuleDto externalRule = db.rules().insert(r -> r
      .setIsExternal(true)
      .setName("ext rule name")
      .setNoteUserUuid(userDto.getUuid()));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, externalRule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getName()).isEqualTo("ext rule name");
  }

  @Test
  public void show_adhoc_rule() {
    // Ad-hoc description has no description sections defined
    RuleDto externalRule = db.rules().insert(newRuleWithoutDescriptionSection()
      .setIsExternal(true)
      .setIsAdHoc(true)
      .setAdHocName("adhoc name")
      .setAdHocDescription("<div>desc</div>")
      .setAdHocSeverity(Severity.BLOCKER)
      .setAdHocType(RuleType.VULNERABILITY)
      .setNoteData(null)
      .setNoteUserUuid(null));

    doReturn("&lt;div&gt;desc2&lt;/div&gt;").when(macroInterpreter).interpret(externalRule.getAdHocDescription());

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, externalRule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule)
      .extracting(Rule::getName, Rule::getHtmlDesc, Rule::getSeverity, Rule::getType)
      .containsExactlyInAnyOrder("adhoc name", "&lt;div&gt;desc2&lt;/div&gt;", Severity.BLOCKER, VULNERABILITY);

    assertThat(resultRule.getDescriptionSections().getDescriptionSectionsList())
      .extracting(Rule.DescriptionSection::getKey, Rule.DescriptionSection::getContent)
      .containsExactly(tuple(DEFAULT_KEY, "&lt;div&gt;desc2&lt;/div&gt;"));
  }

  @Test
  public void show_rule_desc_sections() {
    when(macroInterpreter.interpret(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

    RuleDescriptionSectionDto section1 = createRuleDescriptionSection(ROOT_CAUSE_SECTION_KEY, "<div>Root is Root</div>");
    RuleDescriptionSectionDto section2 = createRuleDescriptionSection(ASSESS_THE_PROBLEM_SECTION_KEY, "<div>This is not a problem</div>");
    RuleDescriptionSectionDto section3 = createRuleDescriptionSection(HOW_TO_FIX_SECTION_KEY, "<div>I don't want to fix</div>");
    RuleDescriptionSectionDto section4context1 = createRuleDescriptionSectionWithContext(RESOURCES_SECTION_KEY, "<div>I want to fix with Spring</div>", "ctx1");
    RuleDescriptionSectionDto section4context2 = createRuleDescriptionSectionWithContext(RESOURCES_SECTION_KEY, "<div>I want to fix with Servlet</div>", "ctx2");

    RuleDto rule = createRuleWithDescriptionSections(section1, section2, section3, section4context1, section4context2);
    rule.setType(RuleType.SECURITY_HOTSPOT);
    rule.setNoteUserUuid(userDto.getUuid());
    db.rules().insert(rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getHtmlDesc()).isEmpty();
    assertThat(resultRule.getMdDesc()).isEqualTo(resultRule.getHtmlDesc());

    assertThat(resultRule.getDescriptionSections().getDescriptionSectionsList())
      .extracting(Rule.DescriptionSection::getKey, Rule.DescriptionSection::getContent, section -> section.getContext().getKey(), section -> section.getContext().getDisplayName())
      .containsExactlyInAnyOrder(
        tuple(ROOT_CAUSE_SECTION_KEY, "<div>Root is Root</div>", "", ""),
        tuple(ASSESS_THE_PROBLEM_SECTION_KEY, "<div>This is not a problem</div>", "", ""),
        tuple(HOW_TO_FIX_SECTION_KEY, "<div>I don't want to fix</div>", "", ""),
        tuple(RESOURCES_SECTION_KEY, "<div>I want to fix with Spring</div>", section4context1.getContext().getKey(), section4context1.getContext().getDisplayName()),
        tuple(RESOURCES_SECTION_KEY, "<div>I want to fix with Servlet</div>", section4context2.getContext().getKey(), section4context2.getContext().getDisplayName()));
  }

  @Test
  public void show_rule_desc_sections_and_html_desc_with_macro() {
    RuleDescriptionSectionDto section = createRuleDescriptionSection(DEFAULT_KEY, "<div>Testing macro: {rule:java:S001}</div>");
    RuleDto rule = createRuleWithDescriptionSections(section);
    rule.setType(RuleType.SECURITY_HOTSPOT);
    rule.setNoteUserUuid(userDto.getUuid());
    db.rules().insert(rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);
    Rule resultRule = result.getRule();

    assertThat(resultRule.getHtmlDesc()).isEqualTo(INTERPRETED);
    assertThat(resultRule.getDescriptionSections().getDescriptionSectionsList())
      .extracting(Rule.DescriptionSection::getKey, Rule.DescriptionSection::getContent)
      .containsExactly(tuple(DEFAULT_KEY, INTERPRETED));
  }

  @Test
  public void show_rule_desc_sections_and_markdown_desc_with_macro() {
    RuleDescriptionSectionDto section = createRuleDescriptionSection(DEFAULT_KEY, "Testing macro: {rule:java:S001}");
    RuleDto rule = createRuleWithDescriptionSections(section);
    rule.setDescriptionFormat(MARKDOWN);
    rule.setType(RuleType.SECURITY_HOTSPOT);
    rule.setNoteUserUuid(userDto.getUuid());
    db.rules().insert(rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);
    Rule resultRule = result.getRule();

    assertThat(resultRule.getHtmlDesc()).isEqualTo(INTERPRETED);
    assertThat(resultRule.getDescriptionSections().getDescriptionSectionsList())
      .extracting(Rule.DescriptionSection::getKey, Rule.DescriptionSection::getContent)
      .containsExactly(tuple(DEFAULT_KEY, INTERPRETED));
  }

  @Test
  public void show_if_advanced_sections_and_default_filters_out_default() {
    when(macroInterpreter.interpret(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

    RuleDescriptionSectionDto section1 = createRuleDescriptionSection(ROOT_CAUSE_SECTION_KEY, "<div>Root is Root</div>");
    RuleDescriptionSectionDto defaultSection = createDefaultRuleDescriptionSection(uuidFactory.create(), "This is the default section");

    RuleDto rule = createRuleWithDescriptionSections(section1, defaultSection);
    rule.setType(RuleType.SECURITY_HOTSPOT);
    rule.setNoteUserUuid(userDto.getUuid());
    db.rules().insert(rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule.getHtmlDesc()).contains(defaultSection.getContent());

    assertThat(resultRule.getMdDesc()).isEqualTo(resultRule.getHtmlDesc());

    assertThat(resultRule.getDescriptionSections().getDescriptionSectionsList())
      .extracting(Rule.DescriptionSection::getKey, Rule.DescriptionSection::getContent)
      .containsExactlyInAnyOrder(tuple(ROOT_CAUSE_SECTION_KEY, "<div>Root is Root</div>"));
  }

  @Test
  public void show_rule_markdown_description() {
    when(macroInterpreter.interpret(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

    var section = createRuleDescriptionSection(DEFAULT_KEY, "*toto is toto*");

    RuleDto rule = createRuleWithDescriptionSections(section);
    rule.setDescriptionFormat(MARKDOWN);
    rule.setNoteUserUuid(userDto.getUuid());
    db.rules().insert(rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();

    assertThat(resultRule.getHtmlDesc()).contains("<strong>toto is toto</strong>");
    assertThat(resultRule.getMdDesc()).contains("*toto is toto*");

    assertThat(resultRule.getDescriptionSections().getDescriptionSectionsList())
      .extracting(Rule.DescriptionSection::getKey, Rule.DescriptionSection::getContent)
      .contains(tuple(DEFAULT_KEY, "<strong>toto is toto</strong>"));
  }

  @Test
  public void ignore_predefined_info_on_adhoc_rule() {
    RuleDto externalRule = newRule(createDefaultRuleDescriptionSection(uuidFactory.create(), "<div>predefined desc</div>"))
      .setIsExternal(true)
      .setIsAdHoc(true)
      .setName("predefined name")
      .setSeverity(Severity.BLOCKER)
      .setType(RuleType.VULNERABILITY)
      .setAdHocName("adhoc name")
      .setAdHocDescription("<div>adhoc desc</div>")
      .setAdHocSeverity(Severity.MAJOR)
      .setAdHocType(RuleType.CODE_SMELL)
      .setNoteData(null)
      .setNoteUserUuid(null);
    externalRule = db.rules().insert(externalRule);
    doReturn("&lt;div&gt;adhoc desc&lt;/div&gt;").when(macroInterpreter).interpret(externalRule.getAdHocDescription());

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, externalRule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule)
      .extracting(Rule::getName, Rule::getHtmlDesc, Rule::getSeverity, Rule::getType)
      .containsExactlyInAnyOrder("adhoc name", "&lt;div&gt;adhoc desc&lt;/div&gt;", Severity.MAJOR, Common.RuleType.CODE_SMELL);
  }

  @Test
  public void adhoc_info_are_empty_when_no_metadata() {
    RuleDto externalRule = db.rules().insert(r -> r
      .setIsExternal(true)
      .setIsAdHoc(true)
      .setName(null)
      .setDescriptionFormat(null)
      .setSeverity((String) null)
      .setNoteData(null)
      .setNoteUserUuid(null)
      .setAdHocDescription(null)
      .setType(0)
      .setAdHocSeverity(null)
      .setAdHocName(null)
      .setAdHocType(0));

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, externalRule.getKey().toString())
      .executeProtobuf(ShowResponse.class);

    Rule resultRule = result.getRule();
    assertThat(resultRule)
      .extracting(Rule::hasName, Rule::hasHtmlDesc, Rule::hasSeverity, Rule::getType)
      .containsExactlyInAnyOrder(false, false, false, UNKNOWN);
  }

  @Test
  public void show_rule_with_activation() {
    RuleDto rule = db.rules().insert(r -> r.setNoteData(null).setNoteUserUuid(null));
    RuleParamDto ruleParam = db.rules().insertRuleParam(rule, p -> p.setType("STRING").setDescription("Reg *exp*").setDefaultValue(".*"));
    QProfileDto qProfile = db.qualityProfiles().insert();
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(qProfile, rule);
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule, new ActiveRuleParamDto()
      .setRulesParameterUuid(ruleParam.getUuid())
      .setKey(ruleParam.getName())
      .setValue(".*?"));
    db.commit();

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_ACTIVES, "true")
      .executeProtobuf(ShowResponse.class);

    List<Rules.Active> actives = result.getActivesList();
    assertThat(actives).extracting(Rules.Active::getQProfile).containsExactly(qProfile.getKee());
    assertThat(actives).extracting(Rules.Active::getSeverity).containsExactly(activeRule.getSeverityString());
    assertThat(actives).extracting(Rules.Active::getInherit).containsExactly("NONE");
    assertThat(actives.get(0).getParamsList())
      .extracting(Rules.Active.Param::getKey, Rules.Active.Param::getValue)
      .containsExactlyInAnyOrder(tuple(ruleParam.getName(), ".*?"));
  }

  @Test
  public void show_rule_without_activation() {
    RuleDto rule = db.rules().insert(r -> r.setNoteData(null).setNoteUserUuid(null));
    QProfileDto qProfile = db.qualityProfiles().insert();
    db.qualityProfiles().activateRule(qProfile, rule);

    ShowResponse result = ws.newRequest()
      .setParam(PARAM_KEY, rule.getKey().toString())
      .setParam(PARAM_ACTIVES, "false")
      .executeProtobuf(ShowResponse.class);

    assertThat(result.getActivesList()).isEmpty();
  }

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.isPost()).isFalse();
    assertThat(def.since()).isEqualTo("4.2");
    assertThat(def.isInternal()).isFalse();
    assertThat(def.responseExampleAsString()).isNotEmpty();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("key", true),
        tuple("actives", false));
  }

  private RuleDescriptionSectionDto createRuleDescriptionSection(String key, String content) {
    return createRuleDescriptionSectionWithContext(key, content, null);
  }

  private RuleDescriptionSectionDto createRuleDescriptionSectionWithContext(String key, String content, @Nullable String contextKey) {
    RuleDescriptionSectionContextDto contextDto = Optional.ofNullable(contextKey)
      .map(c -> RuleDescriptionSectionContextDto.of(contextKey, contextKey + " display name"))
      .orElse(null);
    return RuleDescriptionSectionDto.builder()
      .uuid(uuidFactory.create())
      .key(key)
      .content(content)
      .context(contextDto)
      .build();
  }

  private RuleDto createRuleWithDescriptionSections(RuleDescriptionSectionDto... sections) {
    var rule = newRuleWithoutDescriptionSection();
    for (var section : sections) {
      rule.addRuleDescriptionSectionDto(section);
    }
    return rule;
  }
}
