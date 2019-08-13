/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;

public class QProfileBackuperImplTest {

  private static final String EMPTY_BACKUP = "<?xml version='1.0' encoding='UTF-8'?>" +
    "<profile><name>foo</name>" +
    "<language>js</language>" +
    "<rules/>" +
    "</profile>";

  private System2 system2 = new AlwaysIncreasingSystem2();

  @Rule
  public ExpectedException expectedException = none();
  @Rule
  public DbTester db = DbTester.create(system2);

  private DummyReset reset = new DummyReset();
  private QProfileFactory profileFactory = new DummyProfileFactory();
  private QProfileBackuper underTest = new QProfileBackuperImpl(db.getDbClient(), reset, profileFactory);

  @Test
  public void backup_generates_xml_file() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);
    ActiveRuleDto activeRule = activate(profile, rule);

    StringWriter writer = new StringWriter();
    underTest.backup(db.getSession(), profile, writer);

    assertThat(writer.toString()).isEqualTo("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile><name>" + profile.getName() + "</name>" +
      "<language>" + profile.getLanguage() + "</language>" +
      "<rules>" +
      "<rule>" +
      "<repositoryKey>" + rule.getRepositoryKey() + "</repositoryKey>" +
      "<key>" + rule.getRuleKey() + "</key>" +
      "<priority>" + activeRule.getSeverityString() + "</priority>" +
      "<parameters/>" +
      "</rule>" +
      "</rules>" +
      "</profile>");
  }

  @Test
  public void backup_rules_having_parameters() {
    RuleDefinitionDto rule = createRule();
    RuleParamDto param = db.rules().insertRuleParam(rule);
    QProfileDto profile = createProfile(rule);
    ActiveRuleDto activeRule = activate(profile, rule, param);

    StringWriter writer = new StringWriter();
    underTest.backup(db.getSession(), profile, writer);

    assertThat(writer.toString()).contains(
      "<rule>" +
        "<repositoryKey>" + rule.getRepositoryKey() + "</repositoryKey>" +
        "<key>" + rule.getRuleKey() + "</key>" +
        "<priority>" + activeRule.getSeverityString() + "</priority>" +
        "<parameters><parameter>" +
        "<key>" + param.getName() + "</key>" +
        "<value>20</value>" +
        "</parameter></parameters>" +
        "</rule>");
  }

  @Test
  public void backup_empty_profile() {
    RuleDefinitionDto rule = createRule();
    QProfileDto profile = createProfile(rule);

    StringWriter writer = new StringWriter();
    underTest.backup(db.getSession(), profile, writer);

    assertThat(writer.toString()).isEqualTo("<?xml version='1.0' encoding='UTF-8'?>" +
      "<profile><name>" + profile.getName() + "</name>" +
      "<language>" + profile.getLanguage() + "</language>" +
      "<rules/>" +
      "</profile>");
  }

  @Test
  public void restore_backup_on_the_profile_specified_in_backup() {
    OrganizationDto organization = db.organizations().insert();
    Reader backup = new StringReader(EMPTY_BACKUP);

    QProfileRestoreSummary summary = underTest.restore(db.getSession(), backup, organization, null);

    assertThat(summary.getProfile().getName()).isEqualTo("foo");
    assertThat(summary.getProfile().getLanguage()).isEqualTo("js");

    assertThat(reset.calledProfile.getKee()).isEqualTo(summary.getProfile().getKee());
    assertThat(reset.calledActivations).isEmpty();
  }

  @Test
  public void restore_backup_on_profile_having_different_name() {
    OrganizationDto organization = db.organizations().insert();
    Reader backup = new StringReader(EMPTY_BACKUP);

    QProfileRestoreSummary summary = underTest.restore(db.getSession(), backup, organization, "bar");

    assertThat(summary.getProfile().getName()).isEqualTo("bar");
    assertThat(summary.getProfile().getLanguage()).isEqualTo("js");

    assertThat(reset.calledProfile.getKee()).isEqualTo(summary.getProfile().getKee());
    assertThat(reset.calledActivations).isEmpty();
  }

  @Test
  public void restore_resets_the_activated_rules() {
    Integer ruleId = db.rules().insert(RuleKey.of("sonarjs", "s001")).getId();
    OrganizationDto organization = db.organizations().insert();
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

    underTest.restore(db.getSession(), backup, organization, null);

    assertThat(reset.calledActivations).hasSize(1);
    RuleActivation activation = reset.calledActivations.get(0);
    assertThat(activation.getSeverity()).isEqualTo("BLOCKER");
    assertThat(activation.getRuleId()).isEqualTo(ruleId);
    assertThat(activation.getParameter("bar")).isEqualTo("baz");
  }

  @Test
  public void fail_to_restore_if_bad_xml_format() {
    OrganizationDto organization = db.organizations().insert();
    try {
      underTest.restore(db.getSession(), new StringReader("<rules><rule></rules>"), organization, null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Backup XML is not valid. Root element must be <profile>.");
      assertThat(reset.calledProfile).isNull();
    }
  }

  @Test
  public void fail_to_restore_if_not_xml_backup() {
    OrganizationDto organization = db.organizations().insert();
    try {
      underTest.restore(db.getSession(), new StringReader("foo"), organization, null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(reset.calledProfile).isNull();
    }
  }

  @Test
  public void fail_to_restore_if_xml_is_not_well_formed() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Fail to restore Quality profile backup, XML document is not well formed");
    OrganizationDto organization = db.organizations().insert();
    String notWellFormedXml = "<?xml version='1.0' encoding='UTF-8'?><profile><name>\"profil\"</name><language>\"language\"</language><rules/></profile";

    underTest.restore(db.getSession(), new StringReader(notWellFormedXml), organization, null);
  }

  @Test
  public void fail_to_restore_if_duplicate_rule() throws Exception {
    OrganizationDto organization = db.organizations().insert();
    try {
      String xml = Resources.toString(getClass().getResource("QProfileBackuperTest/duplicates-xml-backup.xml"), UTF_8);
      underTest.restore(db.getSession(), new StringReader(xml), organization, null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("The quality profile cannot be restored as it contains duplicates for the following rules: xoo:x1, xoo:x2");
      assertThat(reset.calledProfile).isNull();
    }
  }

  @Test
  public void fail_to_restore_external_rule() {
    db.rules().insert(RuleKey.of("sonarjs", "s001"), r -> r.setIsExternal(true)).getId();
    OrganizationDto organization = db.organizations().insert();
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

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The quality profile cannot be restored as it contains rules from external rule engines: sonarjs:s001");
    underTest.restore(db.getSession(), backup, organization, null);
  }

  private RuleDefinitionDto createRule() {
    return db.rules().insert();
  }

  private QProfileDto createProfile(RuleDefinitionDto rule) {
    return db.qualityProfiles().insert(db.getDefaultOrganization(), p -> p.setLanguage(rule.getLanguage()));
  }

  private ActiveRuleDto activate(QProfileDto profile, RuleDefinitionDto rule) {
    return db.qualityProfiles().activateRule(profile, rule);
  }

  private ActiveRuleDto activate(QProfileDto profile, RuleDefinitionDto rule, RuleParamDto param) {
    ActiveRuleDto activeRule = db.qualityProfiles().activateRule(profile, rule);
    ActiveRuleParamDto dto = ActiveRuleParamDto.createFor(param)
      .setValue("20")
      .setActiveRuleId(activeRule.getId());
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
    public QProfileDto getOrCreateCustom(DbSession dbSession, OrganizationDto organization, QProfileName key) {
      return QualityProfileTesting.newQualityProfileDto()
        .setOrganizationUuid(organization.getUuid())
        .setLanguage(key.getLanguage())
        .setName(key.getName());
    }

    @Override
    public QProfileDto checkAndCreateCustom(DbSession dbSession, OrganizationDto organization, QProfileName name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void delete(DbSession dbSession, Collection<QProfileDto> profiles) {
      throw new UnsupportedOperationException();
    }
  }
}
