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
package org.sonar.server.qualityprofile;

import com.google.common.io.Resources;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.ActiveRuleDao;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.db.qualityprofile.ActiveRuleDto.INHERITED;
import static org.sonar.db.qualityprofile.ActiveRuleDto.OVERRIDES;
import static org.sonar.db.rule.RuleTesting.XOO_X1;
import static org.sonar.db.rule.RuleTesting.XOO_X2;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.newXooX1;
import static org.sonar.db.rule.RuleTesting.newXooX2;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_NAME;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P2_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P2_NAME;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P3_NAME;
import static org.sonar.server.qualityprofile.QProfileTesting.newXooP1;
import static org.sonar.server.qualityprofile.QProfileTesting.newXooP2;

public class QProfileBackuperMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private DbClient db;
  private DbSession dbSession;
  private RuleIndexer ruleIndexer;
  private ActiveRuleIndexer activeRuleIndexer;
  private OrganizationDto organization;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    ruleIndexer = tester.get(RuleIndexer.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);

    // create pre-defined rules
    RuleDto xooRule1 = newXooX1().setSeverity("MINOR").setLanguage("xoo");
    db.ruleDao().insert(dbSession, xooRule1.getDefinition());
    db.ruleDao().insertRuleParam(dbSession, xooRule1.getDefinition(), RuleParamDto.createFor(xooRule1.getDefinition())
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));
    dbSession.commit();
    ruleIndexer.indexRuleDefinition(xooRule1.getDefinition().getKey());

    RuleDto xooRule2 = newXooX2().setSeverity("MAJOR").setLanguage("xoo");
    db.ruleDao().insert(dbSession, xooRule2.getDefinition());
    dbSession.commit();
    ruleIndexer.indexRuleDefinition(xooRule2.getDefinition().getKey());

    this.organization = OrganizationTesting.newOrganizationDto();
    db.organizationDao().insert(dbSession, organization, false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void backup() throws Exception {
    RuleKey blahRuleKey = RuleKey.of("blah", "my-rule");
    RuleDefinitionDto blahRule = newRule(blahRuleKey).setSeverity("INFO").setLanguage("xoo");
    db.ruleDao().insert(dbSession, blahRule);
    dbSession.commit();
    ruleIndexer.indexRuleDefinition(blahRule.getKey());

    // create profile P1 with rules x2 and x1 activated
    QualityProfileDto profile = newXooP1(organization);
    db.qualityProfileDao().insert(dbSession, profile);
    RuleActivation activation1 = new RuleActivation(XOO_X2).setSeverity("MINOR");
    RuleActivation activation2 = new RuleActivation(XOO_X1);
    RuleActivation activation3 = new RuleActivation(blahRuleKey);
    activation2.setSeverity(Severity.BLOCKER);
    activation2.setParameter("max", "7");
    QualityProfileDto profileDto = get(XOO_P1_NAME);
    tester.get(RuleActivator.class).activate(dbSession, activation1, profileDto);
    tester.get(RuleActivator.class).activate(dbSession, activation2, profileDto);
    tester.get(RuleActivator.class).activate(dbSession, activation3, profileDto);
    dbSession.commit();
    dbSession.clearCache();
    activeRuleIndexer.index();

    StringWriter output = new StringWriter();
    tester.get(QProfileBackuper.class).backup(dbSession, profile, output);

    String expectedXml = Resources.toString(getClass().getResource("QProfileBackuperMediumTest/expected-backup.xml"), StandardCharsets.UTF_8);
    assertThat(output.toString()).isXmlEqualTo(expectedXml);
  }

  @Test
  public void restore_and_create_profile() throws Exception {
    // Backup file declares profile P1 on xoo
    tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore.xml"), StandardCharsets.UTF_8)),
      organization, null);

    // Check in db
    QualityProfileDto profile = db.qualityProfileDao().selectByNameAndLanguage(organization, "P1", "xoo", dbSession);
    assertThat(profile).isNotNull();

    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(dbSession, profile.getKey());
    assertThat(activeRules).hasSize(1);
    ActiveRuleDto activeRuleDoc = activeRules.get(0);
    assertThat(activeRuleDoc.getSeverityString()).isEqualTo("BLOCKER");
    assertThat(activeRuleDoc.getInheritance()).isNull();

    ActiveRuleDto activeRuleDto = db.activeRuleDao().selectOrFailByKey(dbSession, activeRuleDoc.getKey());
    List<ActiveRuleParamDto> params = tester.get(ActiveRuleDao.class).selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getKey()).isEqualTo("max");
    assertThat(params.get(0).getValue()).isEqualTo("7");

    // Check in es
    assertThat(tester.get(RuleIndex.class).searchAll(new RuleQuery().setQProfileKey(profile.getKey()).setActivation(true))).containsOnly(XOO_X1);
  }

  @Test
  public void restore_and_update_profile() throws Exception {
    // create profile P1 with rules x1 and x2 activated
    db.qualityProfileDao().insert(dbSession, newXooP1(organization));
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    QualityProfileDto profileDto = get(XOO_P1_NAME);
    tester.get(RuleActivator.class).activate(dbSession, activation, profileDto);

    activation = new RuleActivation(XOO_X2);
    activation.setSeverity(Severity.INFO);
    tester.get(RuleActivator.class).activate(dbSession, activation, profileDto);
    dbSession.commit();
    dbSession.clearCache();
    activeRuleIndexer.index();

    // restore backup, which activates only x1
    // -> update x1 and deactivate x2
    tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore.xml"), StandardCharsets.UTF_8)), organization, null);

    // Check in db
    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(dbSession, XOO_P1_KEY);
    assertThat(activeRules).hasSize(1);
    ActiveRuleDto activeRuleDoc = activeRules.get(0);
    assertThat(activeRuleDoc.getSeverityString()).isEqualTo("BLOCKER");
    assertThat(activeRuleDoc.getInheritance()).isNull();

    ActiveRuleDto activeRuleDto = db.activeRuleDao().selectOrFailByKey(dbSession, activeRuleDoc.getKey());
    List<ActiveRuleParamDto> params = tester.get(ActiveRuleDao.class).selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getKey()).isEqualTo("max");
    assertThat(params.get(0).getValue()).isEqualTo("7");

    // Check in es
    assertThat(tester.get(RuleIndex.class).searchAll(new RuleQuery().setQProfileKey(XOO_P1_KEY).setActivation(true))).containsOnly(XOO_X1);
  }

  @Test
  public void restore_child_profile() throws Exception {
    // define two parent/child profiles
    db.qualityProfileDao().insert(dbSession,
      newXooP1(organization),
      newXooP2(organization).setParentKee(XOO_P1_KEY));
    dbSession.commit();

    // rule x1 is activated on parent profile (so inherited by child profile)
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation, XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();
    activeRuleIndexer.index();

    // restore backup of child profile -> overrides x1
    tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore-child.xml"), StandardCharsets.UTF_8)), organization, null);

    // parent profile is unchanged
    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(dbSession, XOO_P1_KEY);
    assertThat(activeRules).hasSize(1);
    ActiveRuleDto activeRuleDoc = activeRules.get(0);
    assertThat(activeRuleDoc.getSeverityString()).isEqualTo("INFO");
    assertThat(activeRuleDoc.getInheritance()).isNull();

    ActiveRuleDto activeRuleDto = db.activeRuleDao().selectOrFailByKey(dbSession, activeRuleDoc.getKey());
    List<ActiveRuleParamDto> params = tester.get(ActiveRuleDao.class).selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getKey()).isEqualTo("max");
    assertThat(params.get(0).getValue()).isEqualTo("10");

    // child profile overrides parent
    activeRules = db.activeRuleDao().selectByProfileKey(dbSession, XOO_P2_KEY);
    assertThat(activeRules).hasSize(1);
    activeRuleDoc = activeRules.get(0);
    assertThat(activeRuleDoc.getSeverityString()).isEqualTo("BLOCKER");
    assertThat(activeRuleDoc.getInheritance()).isEqualTo(OVERRIDES);

    activeRuleDto = db.activeRuleDao().selectOrFailByKey(dbSession, activeRuleDoc.getKey());
    params = tester.get(ActiveRuleDao.class).selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getKey()).isEqualTo("max");
    assertThat(params.get(0).getValue()).isEqualTo("7");
  }

  @Test
  public void restore_parent_profile() throws Exception {
    // define two parent/child profiles
    db.qualityProfileDao().insert(dbSession,
      newXooP1(organization),
      newXooP2(organization).setParentKee(XOO_P1_KEY));
    dbSession.commit();

    // rule x1 is activated on parent profile (so inherited by child profile)
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation, XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();
    activeRuleIndexer.index();

    // restore backup of parent profile -> update x1 and propagates to child
    tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore-parent.xml"), StandardCharsets.UTF_8)), organization, null);

    // parent profile is updated
    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(dbSession, XOO_P1_KEY);
    assertThat(activeRules).hasSize(1);

    ActiveRuleDto activeRuleDoc = activeRules.get(0);
    assertThat(activeRuleDoc.getSeverityString()).isEqualTo("BLOCKER");
    assertThat(activeRuleDoc.getInheritance()).isNull();

    ActiveRuleDto activeRuleDto = db.activeRuleDao().selectOrFailByKey(dbSession, activeRuleDoc.getKey());
    List<ActiveRuleParamDto> params = tester.get(ActiveRuleDao.class).selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getKey()).isEqualTo("max");
    assertThat(params.get(0).getValue()).isEqualTo("7");

    // child profile is inherited
    activeRules = db.activeRuleDao().selectByProfileKey(dbSession, XOO_P2_KEY);
    assertThat(activeRules).hasSize(1);
    activeRuleDoc = activeRules.get(0);
    assertThat(activeRuleDoc.getSeverityString()).isEqualTo("BLOCKER");
    assertThat(activeRuleDoc.getInheritance()).isEqualTo(INHERITED);

    activeRuleDto = db.activeRuleDao().selectOrFailByKey(dbSession, activeRuleDoc.getKey());
    params = tester.get(ActiveRuleDao.class).selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());
    assertThat(params).hasSize(1);
    assertThat(params.get(0).getKey()).isEqualTo("max");
    assertThat(params.get(0).getValue()).isEqualTo("7");
  }

  @Test
  public void keep_other_inherited_rules() throws Exception {
    // define two parent/child profiles
    db.qualityProfileDao().insert(dbSession,
      newXooP1(organization),
      newXooP2(organization).setParentKee(XOO_P1_KEY));
    dbSession.commit();

    // rule x1 is activated on parent profile and is inherited by child profile
    RuleActivation activation = new RuleActivation(XOO_X1);
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation, XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();
    activeRuleIndexer.index();

    // backup of child profile contains x2 but not x1
    tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/keep_other_inherited_rules.xml"), StandardCharsets.UTF_8)), organization, XOO_P2_NAME.getName());

    // x1 and x2
    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, XOO_P2_KEY)).hasSize(2);
  }

  @Test
  public void fail_to_restore_if_not_xml_backup() throws Exception {
    try {
      tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
        Resources.toString(getClass().getResource("QProfileBackuperMediumTest/not-xml-backup.txt"), StandardCharsets.UTF_8)), organization, null);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Fail to restore Quality profile backup");
      assertThat(e.getCause()).isInstanceOf(XMLStreamException.class);
    }
  }

  @Test
  public void fail_to_restore_if_bad_xml_format() throws Exception {
    try {
      tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
        Resources.toString(getClass().getResource("QProfileBackuperMediumTest/bad-xml-backup.xml"), StandardCharsets.UTF_8)), organization, null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Backup XML is not valid. Root element must be <profile>.");
    }
  }

  @Test
  public void fail_to_restore_if_duplicate_rule() throws Exception {
    try {
      tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
        Resources.toString(getClass().getResource("QProfileBackuperMediumTest/duplicates-xml-backup.xml"), StandardCharsets.UTF_8)), organization, null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("The quality profile cannot be restored as it contains duplicates for the following rules: xoo:x1, xoo:x2");
    }
  }

  @Test
  public void restore_and_override_profile_name() throws Exception {
    tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore.xml"), StandardCharsets.UTF_8)),
      organization, XOO_P3_NAME.getName());

    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(dbSession, XOO_P1_KEY);
    assertThat(activeRules).hasSize(0);

    QualityProfileDto target = db.qualityProfileDao().selectByNameAndLanguage(organization, "P3", "xoo", dbSession);
    assertThat(target).isNotNull();
    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, target.getKey())).hasSize(1);
  }

  @Test
  public void restore_profile_with_zero_rules() throws Exception {
    tester.get(QProfileBackuper.class).restore(dbSession, new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/empty.xml"), StandardCharsets.UTF_8)),
      organization, null);

    dbSession.clearCache();
    assertThat(anyActiveRuleExists()).isFalse();
    List<QualityProfileDto> profiles = db.qualityProfileDao().selectAll(dbSession, organization);
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).getName()).isEqualTo("P1");
  }

  private boolean anyActiveRuleExists() throws SQLException {
    try (PreparedStatement preparedStatement = db.openSession(false).getConnection().prepareStatement("SELECT * FROM active_rules");
      ResultSet resultSet = preparedStatement.executeQuery();) {
      return resultSet.next();
    }
  }

  private QualityProfileDto get(QProfileName profileName) {
    return db.qualityProfileDao().selectByNameAndLanguage(organization, profileName.getName(), profileName.getLanguage(), dbSession);
  }
}
