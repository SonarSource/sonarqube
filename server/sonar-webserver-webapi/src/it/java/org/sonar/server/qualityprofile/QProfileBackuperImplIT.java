/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.qualityprofile;

import com.google.common.io.Resources;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.common.rule.RuleCreator;
import org.sonar.server.common.rule.service.NewCustomRule;
import org.sonar.server.qualityprofile.builtin.QProfileName;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.newRuleWithoutDescriptionSection;

class QProfileBackuperImplIT {

  private static final String EMPTY_BACKUP = "<?xml version='1.0' encoding='UTF-8'?>" +
    "<profile><name>foo</name>" +
    "<language>js</language>" +
    "<rules/>" +
    "</profile>";

  private final System2 system2 = new AlwaysIncreasingSystem2();

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  private final DummyReset reset = new DummyReset();
  private final QProfileFactory profileFactory = new DummyProfileFactory();
  private final RuleCreator ruleCreator = mock(RuleCreator.class);

  private final QProfileBackuper underTest = new QProfileBackuperImpl(db.getDbClient(), reset, profileFactory, ruleCreator, new QProfileParser());

  @Test
  void backup_generates_xml_file() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule.getLanguage());
    ActiveRuleDto activeRule = activate(profile, rule, ar -> ar.setPrioritizedRule(false));

    StringWriter writer = new StringWriter();
    underTest.backup(db.getSession(), profile, writer);

    assertThat(writer).hasToString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<profile><name>" + profile.getName() + "</name>" +
      "<language>" + profile.getLanguage() + "</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>" + rule.getRepositoryKey() + "</repositoryKey>" +
      "<key>" + rule.getRuleKey() + "</key>" +
      "<type>" + RuleType.fromDbConstant(rule.getType()).name() + "</type>" +
      "<priority>" + activeRule.getSeverityString() + "</priority>" +
      "<impacts>" +
      "<impact>" +
      "<softwareQuality>MAINTAINABILITY</softwareQuality>" +
      "<severity>HIGH</severity>" +
      "</impact>" +
      "</impacts>" +
      "<parameters></parameters>" +
      "</rule>" +
      "</rules>" +
      "</profile>");
  }

  @Test
  void backup_prioritized_rule() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule.getLanguage());
    ActiveRuleDto activeRule = activate(profile, rule, ar -> ar.setPrioritizedRule(true));

    StringWriter writer = new StringWriter();
    underTest.backup(db.getSession(), profile, writer);

    assertThat(writer).hasToString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<profile><name>" + profile.getName() + "</name>" +
      "<language>" + profile.getLanguage() + "</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>" + rule.getRepositoryKey() + "</repositoryKey>" +
      "<key>" + rule.getRuleKey() + "</key>" +
      "<type>" + RuleType.fromDbConstant(rule.getType()).name() + "</type>" +
      "<priority>" + activeRule.getSeverityString() + "</priority>" +
      "<impacts>" +
      "<impact>" +
      "<softwareQuality>MAINTAINABILITY</softwareQuality>" +
      "<severity>HIGH</severity>" +
      "</impact>" +
      "</impacts>" +
      "<prioritizedRule>true</prioritizedRule>" +
      "<parameters></parameters>" +
      "</rule>" +
      "</rules>" +
      "</profile>");
  }

  @Test
  void backup_rules_having_parameters() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto profile = createProfile(rule.getLanguage());
    ActiveRuleDto activeRule = activate(profile, rule, param);

    StringWriter writer = new StringWriter();
    underTest.backup(db.getSession(), profile, writer);

    assertThat(writer.toString()).contains(
      "<rule>" +
        "<repositoryKey>" + rule.getRepositoryKey() + "</repositoryKey>" +
        "<key>" + rule.getRuleKey() + "</key>" +
        "<type>" + RuleType.fromDbConstant(rule.getType()).name() + "</type>" +
        "<priority>" + activeRule.getSeverityString() + "</priority>" +
        "<impacts>" +
        "<impact>" +
        "<softwareQuality>MAINTAINABILITY</softwareQuality>" +
        "<severity>HIGH</severity>" +
        "</impact>" +
        "</impacts>" +
        "<parameters><parameter>" +
        "<key>" + param.getName() + "</key>" +
        "<value>20</value>" +
        "</parameter></parameters>" +
        "</rule>");
  }

  @Test
  void backup_empty_profile() {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule.getLanguage());

    StringWriter writer = new StringWriter();
    underTest.backup(db.getSession(), profile, writer);

    assertThat(writer).hasToString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<profile><name>" + profile.getName() + "</name>" +
      "<language>" + profile.getLanguage() + "</language>" +
      "<rules></rules>" +
      "</profile>");
  }

  @Test
  void backup_custom_rules_with_params() {
    RuleDto templateRule = db.rules().insert(ruleDefinitionDto -> ruleDefinitionDto
      .setIsTemplate(true));
    RuleDto rule = db.rules().insert(
      newRule(createDefaultRuleDescriptionSection(UuidFactoryFast.getInstance().create(), "custom rule description"))
        .setName("custom rule name")
        .setStatus(RuleStatus.READY)
        .setTemplateUuid(templateRule.getUuid()));
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto profile = createProfile(rule.getLanguage());
    ActiveRuleDto activeRule = activate(profile, rule, param);

    StringWriter writer = new StringWriter();
    underTest.backup(db.getSession(), profile, writer);

    assertThat(writer).hasToString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<profile>" +
      "<name>" + profile.getName() + "</name>" +
      "<language>" + profile.getLanguage() + "</language>" +
      "<rules><rule>" +
      "<repositoryKey>" + rule.getRepositoryKey() + "</repositoryKey>" +
      "<key>" + rule.getKey().rule() + "</key>" +
      "<type>" + RuleType.fromDbConstant(rule.getType()) + "</type>" +
      "<priority>" + activeRule.getSeverityString() + "</priority>" +
      "<impacts>" +
      "<impact>" +
      "<softwareQuality>MAINTAINABILITY</softwareQuality>" +
      "<severity>HIGH</severity>" +
      "</impact>" +
      "</impacts>" +
      "<name>" + rule.getName() + "</name>" +
      "<templateKey>" + templateRule.getKey().rule() + "</templateKey>" +
      "<description>" + rule.getDefaultRuleDescriptionSection().getContent() + "</description>" +
      "<cleanCodeAttribute>" + rule.getCleanCodeAttribute() + "</cleanCodeAttribute>" +
      "<parameters><parameter>" +
      "<key>" + param.getName() + "</key>" +
      "<value>20</value>" +
      "</parameter></parameters>" +
      "</rule></rules></profile>");
  }

  @Test
  void backup_custom_rules_without_description_section() {
    var rule = newRuleWithoutDescriptionSection();
    db.rules().insert(rule);
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto profile = createProfile(rule.getLanguage());
    ActiveRuleDto activeRule = activate(profile, rule, param);

    StringWriter writer = new StringWriter();
    underTest.backup(db.getSession(), profile, writer);

    assertThat(writer).hasToString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
      "<profile>" +
      "<name>" + profile.getName() + "</name>" +
      "<language>" + profile.getLanguage() + "</language>" +
      "<rules><rule>" +
      "<repositoryKey>" + rule.getRepositoryKey() + "</repositoryKey>" +
      "<key>" + rule.getKey().rule() + "</key>" +
      "<type>" + RuleType.fromDbConstant(rule.getType()) + "</type>" +
      "<priority>" + activeRule.getSeverityString() + "</priority>" +
      "<impacts>" +
      "<impact>" +
      "<softwareQuality>MAINTAINABILITY</softwareQuality>" +
      "<severity>HIGH</severity>" +
      "</impact>" +
      "</impacts>" +
      "<parameters><parameter>" +
      "<key>" + param.getName() + "</key>" +
      "<value>20</value>" +
      "</parameter></parameters>" +
      "</rule></rules></profile>");
  }

  @Test
  void restore_backup_on_the_profile_specified_in_backup() {
    Reader backup = new StringReader(EMPTY_BACKUP);

    QProfileRestoreSummary summary = underTest.restore(db.getSession(), backup, (String) null);

    assertThat(summary.profile().getName()).isEqualTo("foo");
    assertThat(summary.profile().getLanguage()).isEqualTo("js");

    assertThat(reset.calledProfile.getKee()).isEqualTo(summary.profile().getKee());
    assertThat(reset.calledActivations).isEmpty();
  }

  @Test
  void restore_detects_deprecated_rule_keys() {
    String ruleUuid = db.rules().insert(RuleKey.of("sonarjs", "s001")).getUuid();
    db.rules().insertDeprecatedKey(c -> c.setRuleUuid(ruleUuid).setOldRuleKey("oldkey").setOldRepositoryKey("oldrepo"));

    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile><name>foo</name>" +
      "<language>js</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>oldrepo</repositoryKey>" +
      "<key>oldkey</key>" +
      "<priority>BLOCKER</priority>" +
      "<parameters>" +
      "<parameter><key>bar</key><value>baz</value></parameter>" +
      "</parameters>" +
      "</rule>" +
      "</rules>" +
      "</profile>");

    underTest.restore(db.getSession(), backup, (String) null);

    assertThat(reset.calledActivations).hasSize(1);
    RuleActivation activation = reset.calledActivations.get(0);
    assertThat(activation.getSeverity()).isEqualTo("BLOCKER");
    assertThat(activation.getRuleUuid()).isEqualTo(ruleUuid);
    assertThat(activation.getParameter("bar")).isEqualTo("baz");
  }

  @Test
  void restore_ignores_deprecated_rule_keys_if_new_key_is_already_present() {
    String ruleUuid = db.rules().insert(RuleKey.of("sonarjs", "s001")).getUuid();
    db.rules().insertDeprecatedKey(c -> c.setRuleUuid(ruleUuid).setOldRuleKey("oldkey").setOldRepositoryKey("oldrepo"));

    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile><name>foo</name>" +
      "<language>js</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>oldrepo</repositoryKey>" +
      "<key>oldkey</key>" +
      "<priority>MAJOR</priority>" +
      "<parameters>" +
      "<parameter><key>bar</key><value>baz</value></parameter>" +
      "</parameters>" +
      "</rule>" +
      "<rule>" +
      "<repositoryKey>sonarjs</repositoryKey>" +
      "<key>s001</key>" +
      "<priority>BLOCKER</priority>" +
      "<parameters>" +
      "<parameter><key>bar2</key><value>baz2</value></parameter>" +
      "</parameters>" +
      "</rule>" +
      "</rules>" +
      "</profile>");

    underTest.restore(db.getSession(), backup, (String) null);

    assertThat(reset.calledActivations).hasSize(1);
    RuleActivation activation = reset.calledActivations.get(0);
    assertThat(activation.getSeverity()).isEqualTo("BLOCKER");
    assertThat(activation.getRuleUuid()).isEqualTo(ruleUuid);
    assertThat(activation.getParameter("bar2")).isEqualTo("baz2");
  }

  @Test
  void restore_backup_on_profile_having_different_name() {
    Reader backup = new StringReader(EMPTY_BACKUP);

    QProfileRestoreSummary summary = underTest.restore(db.getSession(), backup, "bar");

    assertThat(summary.profile().getName()).isEqualTo("bar");
    assertThat(summary.profile().getLanguage()).isEqualTo("js");

    assertThat(reset.calledProfile.getKee()).isEqualTo(summary.profile().getKee());
    assertThat(reset.calledActivations).isEmpty();
  }

  @Test
  void restore_resets_the_activated_rules() {
    String ruleUuid = db.rules().insert(RuleKey.of("sonarjs", "s001")).getUuid();
    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile><name>foo</name>" +
      "<language>js</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>sonarjs</repositoryKey>" +
      "<key>s001</key>" +
      "<priority>BLOCKER</priority>" +
      "<prioritizedRule>true</prioritizedRule>" +
      "<parameters>" +
      "<parameter><key>bar</key><value>baz</value></parameter>" +
      "</parameters>" +
      "</rule>" +
      "</rules>" +
      "</profile>");

    underTest.restore(db.getSession(), backup, (String) null);

    assertThat(reset.calledActivations).hasSize(1);
    RuleActivation activation = reset.calledActivations.get(0);
    assertThat(activation.getSeverity()).isEqualTo("BLOCKER");
    assertThat(activation.isPrioritizedRule()).isTrue();
    assertThat(activation.getRuleUuid()).isEqualTo(ruleUuid);
    assertThat(activation.getParameter("bar")).isEqualTo("baz");
  }

  @ParameterizedTest
  @MethodSource("prioritizedRules")
  void restore_sets_correctly_the_prioritizedRule_flag(boolean prioritizedInProfile, Boolean prioritizedInBackup, boolean expected) {
    RuleDto rule = createRule();
    QProfileDto profile = createProfile(rule.getLanguage());
    ActiveRuleDto activeRule = activate(profile, rule, ar -> ar.setPrioritizedRule(prioritizedInProfile));

    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile><name>" + profile.getName() + "</name>" +
      "<language>" + profile.getLanguage() + "</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>" + rule.getRepositoryKey() + "</repositoryKey>" +
      "<key>" + rule.getRuleKey() + "</key>" +
      "<type>" + RuleType.fromDbConstant(rule.getType()).name() + "</type>" +
      "<priority>" + activeRule.getSeverityString() + "</priority>" +
      (prioritizedInBackup == null ? "" : "<prioritizedRule>" + prioritizedInBackup + "</prioritizedRule>") +
      "</rule>" +
      "</rules>" +
      "</profile>");

    underTest.restore(db.getSession(), backup, (String) null);

    assertThat(reset.calledActivations).hasSize(1);
    RuleActivation activation = reset.calledActivations.get(0);
    assertThat(activation.getSeverity()).isEqualTo(activeRule.getSeverityString());
    assertThat(activation.isPrioritizedRule()).isEqualTo(expected);
    assertThat(activation.getRuleUuid()).isEqualTo(rule.getUuid());
  }

  private static Stream<Arguments> prioritizedRules() {
    return Stream.of(
      Arguments.of(true, false, false),
      Arguments.of(true, null, false),
      Arguments.of(true, true, true),
      Arguments.of(false, false, false),
      Arguments.of(false, null, false),
      Arguments.of(false, true, true));
  }

  @Test
  void restore_custom_rule() {
    when(ruleCreator.restore(any(), anyList())).then(invocation -> {
      List<NewCustomRule> newRuleList = invocation.getArgument(1);
      return newRuleList.stream().map(newRule -> {
        RuleDto rule = db.rules().insert(newRule.ruleKey())
            .setCleanCodeAttribute(newRule.getCleanCodeAttribute())
            .setName(newRule.name());
        return db.rules().update(rule);
      }).toList();
    });

    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile>" +
      "<name>custom rule</name>" +
      "<language>js</language>" +
      "<rules><rule>" +
      "<repositoryKey>sonarjs</repositoryKey>" +
      "<key>s001</key>" +
      "<type>CODE_SMELL</type>" +
      "<priority>CRITICAL</priority>" +
      "<name>custom rule name</name>" +
      "<templateKey>rule_mc8</templateKey>" +
      "<description>custom rule description</description>" +
      "<cleanCodeAttribute>MODULAR</cleanCodeAttribute>" +
      "<parameters><parameter>" +
      "<key>bar</key>" +
      "<value>baz</value>" +
      "</parameter>" +
      "</parameters>" +
      "</rule></rules></profile>");

    underTest.restore(db.getSession(), backup, (String) null);

    assertThat(reset.calledActivations).hasSize(1);
    RuleActivation activation = reset.calledActivations.get(0);
    assertThat(activation.getSeverity()).isEqualTo("CRITICAL");
    assertThat(activation.getParameter("bar")).isEqualTo("baz");
    RuleDto rule = db.getDbClient().ruleDao().selectByKey( db.getSession(), RuleKey.of("sonarjs", "s001")).get();
    assertThat(rule.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.MODULAR);
    assertThat(rule.getName()).isEqualTo("custom rule name");
  }

  @Test
  void restore_custom_rule_without_clean_code_attribute() {
    when(ruleCreator.restore(any(), anyList())).then(invocation -> {
      List<NewCustomRule> newRuleList = invocation.getArgument(1);
      return newRuleList.stream().map(newRule -> {
        RuleDto rule = db.rules().insert(newRule.ruleKey())
                .setCleanCodeAttribute(newRule.getCleanCodeAttribute())
                .setName(newRule.name());
        return db.rules().update(rule);
      }).toList();
    });

    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
            "<profile>" +
            "<name>custom rule</name>" +
            "<language>js</language>" +
            "<rules><rule>" +
            "<repositoryKey>sonarjs</repositoryKey>" +
            "<key>s001</key>" +
            "<type>CODE_SMELL</type>" +
            "<priority>CRITICAL</priority>" +
            "<name>custom rule name</name>" +
            "<templateKey>rule_mc8</templateKey>" +
            "<description>custom rule description</description>" +
            "<parameters><parameter>" +
            "<key>bar</key>" +
            "<value>baz</value>" +
            "</parameter>" +
            "</parameters>" +
            "</rule></rules></profile>");

    underTest.restore(db.getSession(), backup, (String) null);

    assertThat(reset.calledActivations).hasSize(1);
    RuleDto rule = db.getDbClient().ruleDao().selectByKey( db.getSession(), RuleKey.of("sonarjs", "s001")).get();
    assertThat(rule.getCleanCodeAttribute()).isNull();
  }

  @Test
  void restore_skips_rule_without_template_key_and_db_definition() {
    String ruleUuid = db.rules().insert(RuleKey.of("sonarjs", "s001")).getUuid();
    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile><name>foo</name>" +
      "<language>js</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>sonarjs</repositoryKey>" +
      "<key>s001</key>" +
      "<priority>BLOCKER</priority>" +
      "<parameters>" +
      "<parameter><key>bar</key><value>baz</value></parameter>" +
      "</parameters>" +
      "</rule>" +
      "<rule>" +
      "<repositoryKey>sonarjs</repositoryKey>" +
      "<key>s002</key>" +
      "<priority>MAJOR</priority>" +
      "</rule>" +
      "</rules>" +
      "</profile>");

    underTest.restore(db.getSession(), backup, (String) null);

    assertThat(reset.calledActivations).hasSize(1);
    RuleActivation activation = reset.calledActivations.get(0);
    assertThat(activation.getRuleUuid()).isEqualTo(ruleUuid);
    assertThat(activation.getSeverity()).isEqualTo("BLOCKER");
    assertThat(activation.getParameter("bar")).isEqualTo("baz");
  }

  @Test
  void restore_should_override_impacts() {
    String ruleUuid = db.rules().insert(RuleKey.of("sonarjs", "s001")).getUuid();

    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile><name>foo</name>" +
      "<language>js</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>sonarjs</repositoryKey>" +
      "<key>s001</key>" +
      "<priority>BLOCKER</priority>" +
      "<impacts>" +
      "<impact>" +
      "<softwareQuality>MAINTAINABILITY</softwareQuality>" +
      "<severity>BLOCKER</severity>" +
      "</impact>" +
      "<impact>" +
      "<softwareQuality>SECURITY</softwareQuality>" +
      "<severity>INFO</severity>" +
      "</impact>" +
      "</impacts>" +
      "<parameters>" +
      "<parameter><key>bar</key><value>baz</value></parameter>" +
      "</parameters>" +
      "</rule>" +
      "</rules>" +
      "</profile>");

    underTest.restore(db.getSession(), backup, (String) null);

    assertThat(reset.calledActivations).hasSize(1);
    RuleActivation activation = reset.calledActivations.get(0);
    assertThat(activation.getSeverity()).isEqualTo("BLOCKER");
    assertThat(activation.getRuleUuid()).isEqualTo(ruleUuid);
    assertThat(activation.getImpactSeverities()).isEqualTo(Map.of(MAINTAINABILITY, BLOCKER, SECURITY, INFO));
    db.getDbClient().activeRuleDao().selectByRuleUuid(db.getSession(), ruleUuid).forEach(ar -> {
      assertThat(ar.getSeverityString()).isEqualTo("BLOCKER");
      assertThat(ar.getImpactsString()).isEqualTo("{\"MAINTAINABILITY\":\"BLOCKER\",\"SECURITY\":\"INFO\"}");
    });
  }

  @Test
  void copy_profile() {
    RuleDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto from = createProfile(rule.getLanguage());
    ActiveRuleDto activeRule = activate(from, rule, param);

    QProfileDto to = createProfile(rule.getLanguage());
    underTest.copy(db.getSession(), from, to);

    assertThat(reset.calledActivations).extracting(RuleActivation::getRuleUuid).containsOnly(activeRule.getRuleUuid());
    assertThat(reset.calledActivations.get(0).getParameter(param.getName())).isEqualTo("20");
    assertThat(reset.calledProfile).isEqualTo(to);
  }

  @Test
  void copy_profile_with_custom_rule() {
    RuleDto templateRule = db.rules().insert(ruleDefinitionDto -> ruleDefinitionDto
      .setIsTemplate(true));
    RuleDto rule = db.rules().insert(
      newRule(createDefaultRuleDescriptionSection(UuidFactoryFast.getInstance().create(), "custom rule description"))
        .setName("custom rule name")
        .setStatus(RuleStatus.READY)
        .setTemplateUuid(templateRule.getUuid()));

    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto from = createProfile(rule.getLanguage());
    ActiveRuleDto activeRule = activate(from, rule, param);

    QProfileDto to = createProfile(rule.getLanguage());
    underTest.copy(db.getSession(), from, to);

    assertThat(reset.calledActivations).extracting(RuleActivation::getRuleUuid).containsOnly(activeRule.getRuleUuid());
    assertThat(reset.calledActivations.get(0).getParameter(param.getName())).isEqualTo("20");
    assertThat(reset.calledProfile).isEqualTo(to);
  }

  @Test
  void fail_to_restore_if_bad_xml_format() {
    DbSession session = db.getSession();
    StringReader backup = new StringReader("<rules><rule></rules>");
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> underTest.restore(session, backup, (String) null));
    assertThat(thrown).hasMessage("Backup XML is not valid. Root element must be <profile>.");
    assertThat(reset.calledProfile).isNull();
  }

  @Test
  void fail_to_restore_if_not_xml_backup() {
    DbSession session = db.getSession();
    StringReader backup = new StringReader("foo");
    assertThrows(IllegalArgumentException.class, () -> underTest.restore(session, backup, (String) null));
    assertThat(reset.calledProfile).isNull();
  }

  @Test
  void fail_to_restore_if_xml_is_not_well_formed() {
    assertThatThrownBy(() -> {
      String notWellFormedXml = "<?xml version='1.0' encoding='UTF-8'?><profile><name>\"profil\"</name><language>\"language\"</language><rules/></profile";

      underTest.restore(db.getSession(), new StringReader(notWellFormedXml), (String) null);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Fail to restore Quality profile backup, XML document is not well formed");
  }

  @Test
  void fail_to_restore_if_duplicate_rule() throws Exception {
    DbSession session = db.getSession();
    String xml = Resources.toString(getClass().getResource("QProfileBackuperIT/duplicates-xml-backup.xml"), UTF_8);
    StringReader backup = new StringReader(xml);
    IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> underTest.restore(session, backup, (String) null));
    assertThat(thrown).hasMessage("The quality profile cannot be restored as it contains duplicates for the following rules: xoo:x1, xoo:x2");
    assertThat(reset.calledProfile).isNull();
  }

  @Test
  void fail_to_restore_external_rule() {
    db.rules().insert(RuleKey.of("sonarjs", "s001"), r -> r.setIsExternal(true));
    Reader backup = new StringReader("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile><name>foo</name>" +
      "<language>js</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>sonarjs</repositoryKey>" +
      "<key>s001</key>" +
      "<priority>BLOCKER</priority>" +
      "<parameters>" +
      "<parameter><key>bar</key><value>baz</value></parameter>" +
      "</parameters>" +
      "</rule>" +
      "</rules>" +
      "</profile>");

    assertThatThrownBy(() -> {
      underTest.restore(db.getSession(), backup, (String) null);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The quality profile cannot be restored as it contains rules from external rule engines: sonarjs:s001");
  }

  private RuleDto createRule() {
    return db.rules().insert();
  }

  private QProfileDto createProfile(String language) {
    return db.qualityProfiles().insert(p -> p.setLanguage(language));
  }

  private ActiveRuleDto activate(QProfileDto profile, RuleDto rule, Consumer<ActiveRuleDto> consumer) {
    return db.qualityProfiles().activateRule(profile, rule, consumer);
  }

  private ActiveRuleDto activate(QProfileDto profile, RuleDto rule, RuleParamDto param) {
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(profile, rule, ar -> ar.setPrioritizedRule(false));
    ActiveRuleParamDto dto = ActiveRuleParamDto.createFor(param)
      .setValue("20")
      .setActiveRuleUuid(activeRule.getUuid());
    db.getDbClient().activeRuleDao().insertParam(db.getSession(), activeRule, dto);
    return activeRule;
  }

  private static class DummyReset implements QProfileReset {
    private QProfileDto calledProfile;
    private List<RuleActivation> calledActivations;

    @Override
    public BulkChangeResult reset(DbSession dbSession, QProfileDto profile, Collection<RuleActivation> activations) {
      this.calledProfile = profile;
      this.calledActivations = new ArrayList<>(activations);
      return new BulkChangeResult();
    }
  }

  private static class DummyProfileFactory implements QProfileFactory {
    @Override
    public QProfileDto getOrCreateCustom(DbSession dbSession, QProfileName key) {
      return QualityProfileTesting.newQualityProfileDto()
        .setLanguage(key.getLanguage())
        .setName(key.getName());
    }

    @Override
    public QProfileDto checkAndCreateCustom(DbSession dbSession, QProfileName name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public QProfileDto createCustom(DbSession dbSession, QProfileName name, @Nullable String parentKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(DbSession dbSession, Collection<QProfileDto> profiles) {
      throw new UnsupportedOperationException();
    }
  }
}
