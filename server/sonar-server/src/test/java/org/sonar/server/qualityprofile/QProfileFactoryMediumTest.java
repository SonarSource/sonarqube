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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.search.IndexClient;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P2_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P3_KEY;

public class QProfileFactoryMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db;
  DbSession dbSession;
  IndexClient index;
  QProfileFactory factory;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    index = tester.get(IndexClient.class);
    factory = tester.get(QProfileFactory.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void create() {
    QualityProfileDto dto = factory.create(dbSession, new QProfileName("xoo", "P1"));
    dbSession.commit();
    dbSession.clearCache();
    assertThat(dto.getKey()).startsWith("xoo-p1-");
    assertThat(dto.getName()).isEqualTo("P1");
    assertThat(dto.getLanguage()).isEqualTo("xoo");
    assertThat(dto.getId()).isNotNull();

    // reload the dto
    dto = db.qualityProfileDao().selectByNameAndLanguage("P1", "xoo", dbSession);
    assertThat(dto.getLanguage()).isEqualTo("xoo");
    assertThat(dto.getName()).isEqualTo("P1");
    assertThat(dto.getKey()).startsWith("xoo-p1");
    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getParentKee()).isNull();

    assertThat(db.qualityProfileDao().selectAll(dbSession)).hasSize(1);
  }

  @Test
  public void fail_to_create_if_name_empty() {
    QProfileName name = new QProfileName("xoo", null);
    try {
      factory.create(dbSession, name);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("quality_profiles.profile_name_cant_be_blank");
    }

    name = new QProfileName("xoo", "");
    try {
      factory.create(dbSession, name);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("quality_profiles.profile_name_cant_be_blank");
    }
  }

  @Test
  public void fail_to_create_if_already_exists() {
    QProfileName name = new QProfileName("xoo", "P1");
    factory.create(dbSession, name);
    dbSession.commit();
    dbSession.clearCache();

    try {
      factory.create(dbSession, name);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Quality profile already exists: {lang=xoo, name=P1}");
    }
  }

  @Test
  public void rename() {
    QualityProfileDto dto = factory.create(dbSession, new QProfileName("xoo", "P1"));
    dbSession.commit();
    dbSession.clearCache();
    String key = dto.getKey();

    assertThat(factory.rename(key, "the new name")).isTrue();
    dbSession.clearCache();

    QualityProfileDto reloaded = db.qualityProfileDao().selectByKey(dbSession, dto.getKee());
    assertThat(reloaded.getKey()).isEqualTo(key);
    assertThat(reloaded.getName()).isEqualTo("the new name");
  }

  @Test
  public void ignore_renaming_if_same_name() {
    QualityProfileDto dto = factory.create(dbSession, new QProfileName("xoo", "P1"));
    dbSession.commit();
    dbSession.clearCache();
    String key = dto.getKey();

    assertThat(factory.rename(key, "P1")).isFalse();
    dbSession.clearCache();

    QualityProfileDto reloaded = db.qualityProfileDao().selectByKey(dbSession, dto.getKee());
    assertThat(reloaded.getKey()).isEqualTo(key);
    assertThat(reloaded.getName()).isEqualTo("P1");
  }

  @Test
  public void fail_if_blank_renaming() {
    QualityProfileDto dto = factory.create(dbSession, new QProfileName("xoo", "P1"));
    dbSession.commit();
    dbSession.clearCache();
    String key = dto.getKey();

    try {
      factory.rename(key, " ");
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Name must be set");
    }
  }

  @Test
  public void fail_renaming_if_profile_not_found() {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Quality profile not found: unknown");

    factory.rename("unknown", "the new name");
  }

  @Test
  public void fail_renaming_if_name_already_exists() {
    QualityProfileDto p1 = factory.create(dbSession, new QProfileName("xoo", "P1"));
    QualityProfileDto p2 = factory.create(dbSession, new QProfileName("xoo", "P2"));
    dbSession.commit();
    dbSession.clearCache();

    try {
      factory.rename(p1.getKey(), "P2");
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("Quality profile already exists: P2");
    }
  }

  @Test
  public void delete() {
    initRules();
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(RuleTesting.XOO_X1), XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    factory.delete(XOO_P1_KEY);

    dbSession.clearCache();
    assertThat(db.qualityProfileDao().selectAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().selectAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().selectAllParams(dbSession)).isEmpty();
    assertThat(index.get(ActiveRuleIndex.class).findByProfile(XOO_P1_KEY)).isEmpty();
  }

  @Test
  public void delete_descendants() {
    initRules();

    // create parent and child profiles
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1(), QProfileTesting.newXooP2(), QProfileTesting.newXooP3());
    tester.get(RuleActivator.class).setParent(dbSession, XOO_P2_KEY, XOO_P1_KEY);
    tester.get(RuleActivator.class).setParent(dbSession, XOO_P3_KEY, XOO_P1_KEY);
    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(RuleTesting.XOO_X1), XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();
    assertThat(db.qualityProfileDao().selectAll(dbSession)).hasSize(3);
    assertThat(db.activeRuleDao().selectAll(dbSession)).hasSize(3);

    factory.delete(XOO_P1_KEY);

    dbSession.clearCache();
    assertThat(db.qualityProfileDao().selectAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().selectAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().selectAllParams(dbSession)).isEmpty();
    assertThat(index.get(ActiveRuleIndex.class).findByProfile(XOO_P1_KEY)).isEmpty();
    assertThat(index.get(ActiveRuleIndex.class).findByProfile(XOO_P2_KEY)).isEmpty();
    assertThat(index.get(ActiveRuleIndex.class).findByProfile(XOO_P3_KEY)).isEmpty();
  }

  @Test
  public void do_not_delete_default_profile() {
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    factory.setDefault(dbSession, XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    try {
      factory.delete(XOO_P1_KEY);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("The profile marked as default can not be deleted: XOO_P1");
      assertThat(db.qualityProfileDao().selectAll(dbSession)).hasSize(1);
    }
  }

  @Test
  public void do_not_delete_if_default_descendant() {
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1(), QProfileTesting.newXooP2(), QProfileTesting.newXooP3());
    tester.get(RuleActivator.class).setParent(dbSession, XOO_P2_KEY, XOO_P1_KEY);
    tester.get(RuleActivator.class).setParent(dbSession, XOO_P3_KEY, XOO_P1_KEY);
    factory.setDefault(dbSession, XOO_P3_KEY);
    dbSession.commit();
    dbSession.clearCache();

    try {
      factory.delete(XOO_P1_KEY);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("The profile marked as default can not be deleted: XOO_P3");
      assertThat(db.qualityProfileDao().selectAll(dbSession)).hasSize(3);
    }
  }

  @Test
  public void fail_if_unknown_profile_to_be_deleted() {
    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Quality profile not found: XOO_P1");

    factory.delete(XOO_P1_KEY);
  }

  @Test
  public void set_default_profile() {
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    dbSession.commit();
    dbSession.clearCache();

    assertThat(db.qualityProfileDao().selectByKey(dbSession, XOO_P1_KEY).isDefault()).isFalse();

    factory.setDefault(XOO_P1_KEY);
    dbSession.clearCache();

    assertThat(db.qualityProfileDao().selectByKey(dbSession, XOO_P1_KEY).isDefault()).isTrue();
  }

  @Test
  public void fail_if_unknown_profile_to_be_set_as_default() {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Quality profile not found: " + XOO_P1_KEY);

    factory.setDefault(XOO_P1_KEY);
  }

  @Test
  public void get_profile_by_project_and_language() {
    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setUuid("ABCD")
      .setKey("org.codehaus.sonar:sonar")
      .setName("SonarQube")
      .setLongName("SonarQube")
      .setQualifier("TRK")
      .setScope("TRK")
      .setEnabled(true);
    db.componentDao().insert(dbSession, project);

    QualityProfileDto profileDto = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profileDto);
    dbSession.commit();
    dbSession.clearCache();
    assertThat(factory.getByProjectAndLanguage("org.codehaus.sonar:sonar", "xoo")).isNull();

    tester.get(QProfileProjectOperations.class).addProject(profileDto.getKey(), project.uuid(),
      new MockUserSession("me").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN), dbSession);
    dbSession.commit();
    dbSession.clearCache();
    assertThat(factory.getByProjectAndLanguage("org.codehaus.sonar:sonar", "xoo").getKey()).isEqualTo(XOO_P1_KEY);
  }

  @Test
  public void get_profile_by_name_and_language() {
    QualityProfileDto profileDto = QProfileTesting.newQProfileDto(new QProfileName("xoo", "SonarQube way"), "abcd");
    db.qualityProfileDao().insert(dbSession, profileDto);
    dbSession.commit();
    dbSession.clearCache();

    assertThat(factory.getByNameAndLanguage("SonarQube way", "xoo").getKey()).isEqualTo("abcd");
    assertThat(factory.getByNameAndLanguage("SonarQube way", "java")).isNull();
    assertThat(factory.getByNameAndLanguage("Unfound", "xoo")).isNull();
  }

  private void initRules() {
    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newXooX1();
    RuleDto xooRule2 = RuleTesting.newXooX2();
    db.deprecatedRuleDao().insert(dbSession, xooRule1, xooRule2);
    db.deprecatedRuleDao().insertRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));
    dbSession.commit();
    dbSession.clearCache();
  }
}
