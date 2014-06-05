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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.tester.ServerTester;

import javax.xml.stream.XMLStreamException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class QProfileBackuperMediumTest {

  static final QualityProfileKey XOO_PROFILE_KEY = QualityProfileKey.of("P1", "xoo");
  static final QualityProfileKey XOO_CHILD_PROFILE_KEY = QualityProfileKey.of("P2", "xoo");

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);

    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newDto(RuleKey.of("xoo", "x1"))
      .setSeverity("MINOR").setLanguage("xoo");
    RuleDto xooRule2 = RuleTesting.newDto(RuleKey.of("xoo", "x2"))
      .setSeverity("MAJOR").setLanguage("xoo");
    db.ruleDao().insert(dbSession, xooRule1, xooRule2);
    db.ruleDao().addRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));
    dbSession.commit();
    dbSession.clearCache();
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void backup() throws Exception {
    // create profile with rule x1 activated
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_KEY));
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.BLOCKER);
    activation.setParameter("max", "7");
    tester.get(RuleActivator.class).activate(dbSession, activation);
    dbSession.commit();
    dbSession.clearCache();

    StringWriter output = new StringWriter();
    tester.get(QProfileBackuper.class).backup(XOO_PROFILE_KEY, output);

    XMLUnit.setIgnoreWhitespace(true);
    Diff diff = XMLUnit.compareXML(output.toString(),
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/expected-backup.xml"), Charsets.UTF_8));

    assertThat(diff.identical()).as(diff.toString()).isTrue();
  }

  @Test
  public void fail_to_backup_unknown_profile() throws Exception {
    try {
      tester.get(QProfileBackuper.class).backup(QualityProfileKey.of("unknown", "xoo"), new StringWriter());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Quality profile does not exist: unknown:xoo");
    }
  }

  @Test
  public void restore_and_create_profile() throws Exception {
    tester.get(QProfileBackuper.class).restore(new StringReader(
        Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore.xml"), Charsets.UTF_8)),
      null);

    List<ActiveRule> activeRules = tester.get(QProfileService.class).findActiveRulesByProfile(XOO_PROFILE_KEY);
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");
  }

  @Test
  public void restore_and_update_profile() throws Exception {
    // create profile with rules x1 and x2 activated
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_KEY));
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation);

    activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x2")));
    activation.setSeverity(Severity.INFO);
    tester.get(RuleActivator.class).activate(dbSession, activation);
    dbSession.commit();
    dbSession.clearCache();

    // restore backup, which activates only x1
    // -> update x1 and deactivate x2
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore.xml"), Charsets.UTF_8)), null);


    List<ActiveRule> activeRules = tester.get(QProfileService.class).findActiveRulesByProfile(XOO_PROFILE_KEY);
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");
  }

  @Test
  public void restore_child_profile() throws Exception {
    // define two parent/child profiles
    db.qualityProfileDao().insert(dbSession,
      QualityProfileDto.createFor(XOO_PROFILE_KEY),
      QualityProfileDto.createFor(XOO_CHILD_PROFILE_KEY).setParent(XOO_PROFILE_KEY.name()));
    dbSession.commit();

    // rule x1 is activated on parent profile (so inherited by child profile)
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation);
    dbSession.commit();
    dbSession.clearCache();

    // restore backup of child profile -> overrides x1
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore-child.xml"), Charsets.UTF_8)), null);

    // parent profile is unchanged
    List<ActiveRule> activeRules = tester.get(QProfileService.class).findActiveRulesByProfile(XOO_PROFILE_KEY);
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("INFO");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("10");

    // child profile overrides parent
    activeRules = tester.get(QProfileService.class).findActiveRulesByProfile(XOO_CHILD_PROFILE_KEY);
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.OVERRIDES);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");
  }

  @Test
  public void restore_parent_profile() throws Exception {
    // define two parent/child profiles
    db.qualityProfileDao().insert(dbSession,
      QualityProfileDto.createFor(XOO_PROFILE_KEY),
      QualityProfileDto.createFor(XOO_CHILD_PROFILE_KEY).setParent(XOO_PROFILE_KEY.name()));
    dbSession.commit();

    // rule x1 is activated on parent profile (so inherited by child profile)
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation);
    dbSession.commit();
    dbSession.clearCache();

    // restore backup of parent profile -> update x1 and propagates to child
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/restore-parent.xml"), Charsets.UTF_8)), null);

    // parent profile is updated
    List<ActiveRule> activeRules = tester.get(QProfileService.class).findActiveRulesByProfile(XOO_PROFILE_KEY);
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.NONE);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");

    // child profile is inherited
    activeRules = tester.get(QProfileService.class).findActiveRulesByProfile(XOO_CHILD_PROFILE_KEY);
    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.get(0).severity()).isEqualTo("BLOCKER");
    assertThat(activeRules.get(0).inheritance()).isEqualTo(ActiveRule.Inheritance.INHERITED);
    assertThat(activeRules.get(0).params().get("max")).isEqualTo("7");
  }

  @Test
  public void keep_other_inherited_rules() throws Exception {
    // define two parent/child profiles
    db.qualityProfileDao().insert(dbSession,
      QualityProfileDto.createFor(XOO_PROFILE_KEY),
      QualityProfileDto.createFor(XOO_CHILD_PROFILE_KEY).setParent(XOO_PROFILE_KEY.name()));
    dbSession.commit();

    // rule x1 is activated on parent profile and is inherited by child profile
    RuleActivation activation = new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_KEY, RuleKey.of("xoo", "x1")));
    activation.setSeverity(Severity.INFO);
    activation.setParameter("max", "10");
    tester.get(RuleActivator.class).activate(dbSession, activation);
    dbSession.commit();
    dbSession.clearCache();

    // backup of child profile contains x2 but not x1
    tester.get(QProfileBackuper.class).restore(new StringReader(
      Resources.toString(getClass().getResource("QProfileBackuperMediumTest/keep_other_inherited_rules.xml"), Charsets.UTF_8)), XOO_CHILD_PROFILE_KEY);

    // x1 and x2
    List<ActiveRule> activeRules = tester.get(QProfileService.class).findActiveRulesByProfile(XOO_CHILD_PROFILE_KEY);
    assertThat(activeRules).hasSize(2);
  }

  @Test
  public void fail_to_restore_if_not_xml_backup() throws Exception {
    try {
      tester.get(QProfileBackuper.class).restore(new StringReader(
        Resources.toString(getClass().getResource("QProfileBackuperMediumTest/not-xml-backup.txt"), Charsets.UTF_8)), null);
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
        Resources.toString(getClass().getResource("QProfileBackuperMediumTest/bad-xml-backup.xml"), Charsets.UTF_8)), null);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Backup XML is not valid. Root element must be <profile>.");
    }
  }
}
