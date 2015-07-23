/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class QProfileBackuperMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);

    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newXooX1().setSeverity("MINOR").setLanguage("xoo");
    RuleDto xooRule2 = RuleTesting.newXooX2().setSeverity("MAJOR").setLanguage("xoo");
    db.deprecatedRuleDao().insert(dbSession, xooRule1, xooRule2);
    db.deprecatedRuleDao().insertRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));
    dbSession.commit();
    dbSession.clearCache();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void backup() throws Exception {
    RuleKey blahRuleKey = RuleKey.of("blah", "my-rule");
    RuleDto blahRule = RuleTesting.newDto(blahRuleKey).setSeverity("INFO").setLanguage("xoo");
    db.deprecatedRuleDao().insert(dbSession, blahRule);
    dbSession.commit();
    dbSession.clearCache();

    // create profile P1 with rules x2 and x1 activated
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    RuleActivation activation1 = new RuleActivation(RuleTesting.XOO_X2).setSeverity("MINOR");
    RuleActivation activation2 = new RuleActivation(RuleTesting.XOO_X1);
    RuleActivation activation3 = new RuleActivation(blahRuleKey);
    activation2.setSeverity(Severity.BLOCKER);
    activation2.setParameter("max", "7");
    tester.get(RuleActivator.class).activate(dbSession, activation1, QProfileTesting.XOO_P1_NAME);
    tester.get(RuleActivator.class).activate(dbSession, activation2, QProfileTesting.XOO_P1_NAME);
    tester.get(RuleActivator.class).activate(dbSession, activation3, QProfileTesting.XOO_P1_NAME);
    dbSession.commit();
    dbSession.clearCache();

    StringWriter output = new StringWriter();
    tester.get(QProfileBackuper.class).backup(QProfileTesting.XOO_P1_KEY, output);

    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreComments(true);
    Diff diff = XMLUnit.compareXML(output.toString(),
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/expected-backup.xml"), StandardCharsets.UTF_8));

    assertThat(diff.identical()).as(diff.toString()).isTrue();
  }

  @Test
  public void fail_to_backup_unknown_profile() {
    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Quality profile not found: unknown");

    tester.get(QProfileBackuper.class).backup("unknown", new StringWriter());
  }

  @Test
  public void restore_and_create_profile() throws Exception {
    // Backup file declares profile P1 on xoo
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore.xml"), StandardCharsets.UTF_8)),
      null);

    QualityProfileDto profile = db.qualityProfileDao().selectByNameAndLanguage("P1", "xoo", dbSession);
    assertThat(profile).isNotNull();

    List<ActiveRule> activeRules = Lists.newArrayList(tester.get(QProfileLoader.class).findActiveRulesByProfile(profile.getKey()));
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");
  }

  @Test
  public void restore_and_update_profile() throws Exception {
    // create profile P1 with rules x1 and x2 activated
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation, QProfileTesting.XOO_P1_NAME);

    activation = new RuleActivation(RuleTesting.XOO_X2);
    activation.setSeverity(Severity.INFO);
    tester.get(RuleActivator.class).activate(dbSession, activation, QProfileTesting.XOO_P1_NAME);
    dbSession.commit();
    dbSession.clearCache();

    // restore backup, which activates only x1
    // -> update x1 and deactivate x2
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore.xml"), StandardCharsets.UTF_8)), null);

    List<ActiveRule> activeRules = Lists.newArrayList(tester.get(QProfileLoader.class).findActiveRulesByProfile(QProfileTesting.XOO_P1_KEY));
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");
  }

  @Test
  public void restore_child_profile() throws Exception {
    // define two parent/child profiles
    db.qualityProfileDao().insert(dbSession,
      QProfileTesting.newXooP1(),
      QProfileTesting.newXooP2().setParentKee(QProfileTesting.XOO_P1_KEY));
    dbSession.commit();

    // rule x1 is activated on parent profile (so inherited by child profile)
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation, QProfileTesting.XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    // restore backup of child profile -> overrides x1
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore-child.xml"), StandardCharsets.UTF_8)), null);

    // parent profile is unchanged
    List<ActiveRule> activeRules = Lists.newArrayList(tester.get(QProfileLoader.class).findActiveRulesByProfile(QProfileTesting.XOO_P1_KEY));
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("INFO");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("10");

    // child profile overrides parent
    activeRules = Lists.newArrayList(tester.get(QProfileLoader.class).findActiveRulesByProfile(QProfileTesting.XOO_P2_KEY));
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.OVERRIDES);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");
  }

  @Test
  public void restore_parent_profile() throws Exception {
    // define two parent/child profiles
    db.qualityProfileDao().insert(dbSession,
      QProfileTesting.newXooP1(),
      QProfileTesting.newXooP2().setParentKee(QProfileTesting.XOO_P1_KEY));
    dbSession.commit();

    // rule x1 is activated on parent profile (so inherited by child profile)
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation, QProfileTesting.XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    // restore backup of parent profile -> update x1 and propagates to child
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore-parent.xml"), StandardCharsets.UTF_8)), null);

    // parent profile is updated
    List<ActiveRule> activeRules = Lists.newArrayList(tester.get(QProfileLoader.class).findActiveRulesByProfile(QProfileTesting.XOO_P1_KEY));
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");

    // child profile is inherited
    activeRules = Lists.newArrayList(tester.get(QProfileLoader.class).findActiveRulesByProfile(QProfileTesting.XOO_P2_KEY));
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.INHERITED);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");
  }

  @Test
  public void keep_other_inherited_rules() throws Exception {
    // define two parent/child profiles
    db.qualityProfileDao().insert(dbSession,
      QProfileTesting.newXooP1(),
      QProfileTesting.newXooP2().setParentKee(QProfileTesting.XOO_P1_KEY));
    dbSession.commit();

    // rule x1 is activated on parent profile and is inherited by child profile
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation, QProfileTesting.XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    // backup of child profile contains x2 but not x1
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/keep_other_inherited_rules.xml"), StandardCharsets.UTF_8)), QProfileTesting.XOO_P2_NAME);

    // x1 and x2
    List<ActiveRule> activeRules = Lists.newArrayList(tester.get(QProfileLoader.class).findActiveRulesByProfile(QProfileTesting.XOO_P2_KEY));
    assertThat(activeRules).hasSize(2);
  }

  @Test
  public void fail_to_restore_if_not_xml_backup() throws Exception {
    try {
      tester.get(QProfileBackuper.class).restore(new StringReader(
        Resources.toString(getClass().getResource("QProfileBackuperMediumTest/not-xml-backup.txt"), StandardCharsets.UTF_8)), null);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Fail to restore Quality profile backup");
      assertThat(e.getCause()).isInstanceOf(XMLStreamException.class);
    }
  }

  @Test
  public void fail_to_restore_if_bad_xml_format() throws Exception {
    try {
      tester.get(QProfileBackuper.class).restore(new StringReader(
        Resources.toString(getClass().getResource("QProfileBackuperMediumTest/bad-xml-backup.xml"), StandardCharsets.UTF_8)), null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Backup XML is not valid. Root element must be <profile>.");
    }
  }

  @Test
  public void fail_to_restore_if_duplicate_rule() throws Exception {
    try {
      tester.get(QProfileBackuper.class).restore(new StringReader(
        Resources.toString(getClass().getResource("QProfileBackuperMediumTest/duplicates-xml-backup.xml"), StandardCharsets.UTF_8)), null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("The quality profile cannot be restored as it contains duplicates for the following rules: xoo:x1, xoo:x2");
    }
  }

  @Test
  public void restore_and_override_profile_name() throws Exception {
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore.xml"), StandardCharsets.UTF_8)),
      QProfileTesting.XOO_P3_NAME);

    List<ActiveRule> activeRules = Lists.newArrayList(tester.get(QProfileLoader.class).findActiveRulesByProfile(QProfileTesting.XOO_P1_KEY));
    assertThat(activeRules).hasSize(0);

    QualityProfileDto target = db.qualityProfileDao().selectByNameAndLanguage("P3", "xoo", dbSession);
    assertThat(target).isNotNull();
    activeRules = Lists.newArrayList(tester.get(QProfileLoader.class).findActiveRulesByProfile(target.getKey()));
    assertThat(activeRules).hasSize(1);
  }

  @Test
  public void restore_profile_with_zero_rules() throws Exception {
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/empty.xml"), StandardCharsets.UTF_8)),
      null);

    dbSession.clearCache();
    assertThat(db.activeRuleDao().selectAll(dbSession)).hasSize(0);
    List<QualityProfileDto> profiles = db.qualityProfileDao().selectAll(dbSession);
    assertThat(profiles).hasSize(1);
    assertThat(profiles.get(0).getName()).isEqualTo("P1");
  }
}
