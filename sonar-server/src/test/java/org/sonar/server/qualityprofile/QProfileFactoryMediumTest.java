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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.ServerTester;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class QProfileFactoryMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  static final QualityProfileKey XOO_PROFILE_1 = QualityProfileKey.of("P1", "xoo");
  static final QualityProfileKey XOO_PROFILE_2 = QualityProfileKey.of("P2", "xoo");
  static final QualityProfileKey XOO_PROFILE_3 = QualityProfileKey.of("P3", "xoo");
  static final RuleKey XOO_RULE_1 = RuleKey.of("xoo", "x1");
  static final RuleKey XOO_RULE_2 = RuleKey.of("xoo", "x2");

  DbClient db;
  DbSession dbSession;
  IndexClient index;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    index = tester.get(IndexClient.class);
  }

  @After
  public void after() throws Exception {
    dbSession.close();
  }

  @Test
  public void delete() {
    initRules();
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_1));
    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_1, XOO_RULE_1)));
    dbSession.commit();
    dbSession.clearCache();

    tester.get(QProfileFactory.class).delete(XOO_PROFILE_1);

    dbSession.clearCache();
    assertThat(db.qualityProfileDao().findAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().findAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().findAllParams(dbSession)).isEmpty();
    assertThat(index.get(ActiveRuleIndex.class).findByProfile(XOO_PROFILE_1)).isEmpty();
  }

  @Test
  public void delete_descendants() {
    initRules();

    // create parent and child profiles
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_1));
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_2));
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_3));
    tester.get(RuleActivator.class).setParent(dbSession, XOO_PROFILE_2, XOO_PROFILE_1);
    tester.get(RuleActivator.class).setParent(dbSession, XOO_PROFILE_3, XOO_PROFILE_2);
    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(ActiveRuleKey.of(XOO_PROFILE_1, XOO_RULE_1)));
    dbSession.commit();
    dbSession.clearCache();
    assertThat(db.qualityProfileDao().findAll(dbSession)).hasSize(3);
    assertThat(db.activeRuleDao().findAll(dbSession)).hasSize(3);

    tester.get(QProfileFactory.class).delete(XOO_PROFILE_1);

    dbSession.clearCache();
    assertThat(db.qualityProfileDao().findAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().findAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().findAllParams(dbSession)).isEmpty();
    assertThat(index.get(ActiveRuleIndex.class).findByProfile(XOO_PROFILE_1)).isEmpty();
  }

  private void initRules() {
    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newDto(XOO_RULE_1).setLanguage("xoo");
    RuleDto xooRule2 = RuleTesting.newDto(XOO_RULE_2).setLanguage("xoo");
    db.ruleDao().insert(dbSession, xooRule1, xooRule2);
    db.ruleDao().addRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));
    dbSession.commit();
    dbSession.clearCache();
  }

  @Test
  public void do_not_delete_default_profile() {
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_1));
    tester.get(QProfileFactory.class).setDefault(dbSession, XOO_PROFILE_1);
    dbSession.commit();
    dbSession.clearCache();

    try {
      tester.get(QProfileFactory.class).delete(XOO_PROFILE_1);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("The profile marked as default can not be deleted: P1:xoo");
      assertThat(db.qualityProfileDao().findAll(dbSession)).hasSize(1);
    }
  }

  @Test
  public void do_not_delete_if_default_descendant() {
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_1));
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_2));
    db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(XOO_PROFILE_3));
    tester.get(RuleActivator.class).setParent(dbSession, XOO_PROFILE_2, XOO_PROFILE_1);
    tester.get(RuleActivator.class).setParent(dbSession, XOO_PROFILE_3, XOO_PROFILE_2);
    tester.get(QProfileFactory.class).setDefault(dbSession, XOO_PROFILE_3);
    dbSession.commit();
    dbSession.clearCache();

    try {
      tester.get(QProfileFactory.class).delete(XOO_PROFILE_1);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("The profile marked as default can not be deleted: P3:xoo");
      assertThat(db.qualityProfileDao().findAll(dbSession)).hasSize(3);
    }
  }

  @Test
  public void fail_if_unknown_profile_to_be_deleted() {
    try {
      tester.get(QProfileFactory.class).delete(XOO_PROFILE_1);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Quality profile not found: P1:xoo");
    }
  }

  @Test
  public void set_default_profile() {
    // TODO
  }

  @Test
  public void fail_if_unknown_profile_to_be_set_as_default() {
    // TODO
  }
}
