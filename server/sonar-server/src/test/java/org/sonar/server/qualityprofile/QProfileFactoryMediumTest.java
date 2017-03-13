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

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P1_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P2_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.XOO_P3_KEY;
import static org.sonar.server.qualityprofile.QProfileTesting.getDefaultOrganization;

public class QProfileFactoryMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private DbClient db;
  private DbSession dbSession;
  private ActiveRuleIndexer activeRuleIndexer;
  private RuleIndexer ruleIndexer;
  private QProfileFactory factory;
  private OrganizationDto organization = OrganizationTesting.newOrganizationDto();

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    dbSession = db.openSession(false);
    factory = tester.get(QProfileFactory.class);
    activeRuleIndexer = tester.get(ActiveRuleIndexer.class);
    ruleIndexer = tester.get(RuleIndexer.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void create() {
    String uuid = organization.getUuid();

    QualityProfileDto writtenDto = factory.create(dbSession, organization, new QProfileName("xoo", "P1"));
    dbSession.commit();
    dbSession.clearCache();
    assertThat(writtenDto.getOrganizationUuid()).isEqualTo(uuid);
    assertThat(writtenDto.getKey()).startsWith("xoo-p1-");
    assertThat(writtenDto.getName()).isEqualTo("P1");
    assertThat(writtenDto.getLanguage()).isEqualTo("xoo");
    assertThat(writtenDto.getId()).isNotNull();

    // reload the dto
    QualityProfileDto readDto = db.qualityProfileDao().selectByNameAndLanguage(organization, "P1", "xoo", dbSession);
    assertThat(readDto.getOrganizationUuid()).isEqualTo(uuid);
    assertThat(readDto.getName()).isEqualTo("P1");
    assertThat(readDto.getKey()).startsWith("xoo-p1");
    assertThat(readDto.getLanguage()).isEqualTo("xoo");
    assertThat(readDto.getId()).isNotNull();
    assertThat(readDto.getParentKee()).isNull();

    assertThat(db.qualityProfileDao().selectAll(dbSession, organization)).hasSize(1);
  }

  @Test
  public void fail_to_create_if_name_null() {
    QProfileName name = new QProfileName("xoo", null);

    expectBadRequestException("quality_profiles.profile_name_cant_be_blank");

    factory.create(dbSession, organization, name);
  }

  @Test
  public void fail_to_create_if_name_empty() {
    QProfileName name = new QProfileName("xoo", "");

    expectBadRequestException("quality_profiles.profile_name_cant_be_blank");

    factory.create(dbSession, organization, name);
  }

  @Test
  public void fail_to_create_if_already_exists() {
    QProfileName name = new QProfileName("xoo", "P1");
    factory.create(dbSession, organization, name);
    dbSession.commit();
    dbSession.clearCache();

    expectBadRequestException("Quality profile already exists: {lang=xoo, name=P1}");

    factory.create(dbSession, organization, name);
  }

  @Test
  public void rename() {
    QualityProfileDto dto = factory.create(dbSession, organization, new QProfileName("xoo", "P1"));
    dbSession.commit();
    dbSession.clearCache();
    String key = dto.getKey();

    assertThat(factory.rename(key, "the new name")).isTrue();
    dbSession.clearCache();

    QualityProfileDto reloaded = db.qualityProfileDao().selectByKey(dbSession, dto.getKee());
    assertThat(reloaded.getOrganizationUuid()).isEqualTo(organization.getUuid());
    assertThat(reloaded.getKey()).isEqualTo(key);
    assertThat(reloaded.getName()).isEqualTo("the new name");
  }

  @Test
  public void ignore_renaming_if_same_name() {
    QualityProfileDto dto = factory.create(dbSession, organization, new QProfileName("xoo", "P1"));
    dbSession.commit();
    dbSession.clearCache();
    String key = dto.getKey();

    assertThat(factory.rename(key, "P1")).isFalse();
    dbSession.clearCache();

    QualityProfileDto reloaded = db.qualityProfileDao().selectByKey(dbSession, dto.getKee());
    assertThat(reloaded.getOrganizationUuid()).isEqualTo(organization.getUuid());
    assertThat(reloaded.getKey()).isEqualTo(key);
    assertThat(reloaded.getName()).isEqualTo("P1");
  }

  @Test
  public void fail_if_blank_renaming() {
    QualityProfileDto dto = factory.create(dbSession, organization, new QProfileName("xoo", "P1"));
    dbSession.commit();
    dbSession.clearCache();
    String key = dto.getKey();

    expectBadRequestException("Name must be set");

    factory.rename(key, " ");
  }

  @Test
  public void fail_renaming_if_profile_not_found() {
    thrown.expect(NotFoundException.class);
    thrown.expectMessage("Quality profile not found: unknown");

    factory.rename("unknown", "the new name");
  }

  @Test
  public void fail_renaming_if_name_already_exists() {
    QualityProfileDto p1 = factory.create(dbSession, organization, new QProfileName("xoo", "P1"));
    QualityProfileDto p2 = factory.create(dbSession, organization, new QProfileName("xoo", "P2"));
    dbSession.commit();
    dbSession.clearCache();

    expectBadRequestException("Quality profile already exists: P2");

    factory.rename(p1.getKey(), "P2");
  }

  @Test
  public void delete() {
    initRules();
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1(organization.getUuid()));
    tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(RuleTesting.XOO_X1), XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    List<ActiveRuleChange> changes = factory.delete(dbSession, XOO_P1_KEY, false);
    dbSession.commit();
    activeRuleIndexer.index(changes);

    dbSession.clearCache();
    assertThat(db.qualityProfileDao().selectAll(dbSession, getDefaultOrganization(tester, db, dbSession))).isEmpty();
    assertThat(db.activeRuleDao().selectAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().selectAllParams(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, XOO_P1_KEY)).isEmpty();
    assertThat(tester.get(RuleIndex.class).searchAll(new RuleQuery().setQProfileKey(XOO_P1_KEY).setActivation(true))).isEmpty();
  }

  @Test
  public void delete_descendants() {
    initRules();

    // create parent and child profiles
    db.qualityProfileDao().insert(dbSession,
      QProfileTesting.newXooP1(organization),
      QProfileTesting.newXooP2(organization),
      QProfileTesting.newXooP3(organization));
    List<ActiveRuleChange> changes = tester.get(RuleActivator.class).setParent(dbSession, XOO_P2_KEY, XOO_P1_KEY);
    changes.addAll(tester.get(RuleActivator.class).setParent(dbSession, XOO_P3_KEY, XOO_P1_KEY));
    changes.addAll(tester.get(RuleActivator.class).activate(dbSession, new RuleActivation(RuleTesting.XOO_X1), XOO_P1_KEY));
    dbSession.commit();
    dbSession.clearCache();
    activeRuleIndexer.index(changes);

    assertThat(db.qualityProfileDao().selectAll(dbSession, organization)).hasSize(3);
    assertThat(db.activeRuleDao().selectAll(dbSession)).hasSize(3);

    changes = factory.delete(dbSession, XOO_P1_KEY, false);
    dbSession.commit();
    activeRuleIndexer.index(changes);

    dbSession.clearCache();
    assertThat(db.qualityProfileDao().selectAll(dbSession, getDefaultOrganization(tester, db, dbSession))).isEmpty();
    assertThat(db.activeRuleDao().selectAll(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().selectAllParams(dbSession)).isEmpty();
    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, XOO_P1_KEY)).isEmpty();
    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, XOO_P2_KEY)).isEmpty();
    assertThat(db.activeRuleDao().selectByProfileKey(dbSession, XOO_P3_KEY)).isEmpty();
  }

  @Test
  public void do_not_delete_default_profile() {
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1(organization));
    factory.setDefault(dbSession, XOO_P1_KEY);
    dbSession.commit();
    dbSession.clearCache();

    try {
      List<ActiveRuleChange> changes = factory.delete(dbSession, XOO_P1_KEY, false);
      dbSession.commit();
      activeRuleIndexer.index(changes);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("The profile marked as default can not be deleted: XOO_P1");
      assertThat(db.qualityProfileDao().selectAll(dbSession, organization)).hasSize(1);
    }
  }

  @Test
  public void do_not_delete_if_default_descendant() {
    db.qualityProfileDao().insert(dbSession,
      QProfileTesting.newXooP1(organization),
      QProfileTesting.newXooP2(organization),
      QProfileTesting.newXooP3(organization));

    List<ActiveRuleChange> changes = tester.get(RuleActivator.class).setParent(dbSession, XOO_P2_KEY, XOO_P1_KEY);
    changes.addAll(tester.get(RuleActivator.class).setParent(dbSession, XOO_P3_KEY, XOO_P1_KEY));
    factory.setDefault(dbSession, XOO_P3_KEY);
    dbSession.commit();
    dbSession.clearCache();
    activeRuleIndexer.index(changes);

    try {
      changes = factory.delete(dbSession, XOO_P1_KEY, false);
      dbSession.commit();
      activeRuleIndexer.index(changes);
      fail();
    } catch (BadRequestException e) {
      assertThat(e).hasMessage("The profile marked as default can not be deleted: XOO_P3");
      assertThat(db.qualityProfileDao().selectAll(dbSession, organization)).hasSize(3);
    }
  }

  @Test
  public void fail_if_unknown_profile_to_be_deleted() {
    thrown.expect(RowNotFoundException.class);
    thrown.expectMessage("Quality profile not found: XOO_P1");

    List<ActiveRuleChange> changes = factory.delete(dbSession, XOO_P1_KEY, false);
    dbSession.commit();
    activeRuleIndexer.index(changes);
  }

  @Test
  public void set_default_profile() {
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1("org-123"));
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

  private void initRules() {
    // create pre-defined rules
    RuleDto xooRule1 = RuleTesting.newXooX1();
    RuleDto xooRule2 = RuleTesting.newXooX2();
    db.ruleDao().insert(dbSession, xooRule1);
    db.ruleDao().insert(dbSession, xooRule2);
    db.ruleDao().insertRuleParam(dbSession, xooRule1, RuleParamDto.createFor(xooRule1)
      .setName("max").setDefaultValue("10").setType(RuleParamType.INTEGER.type()));
    dbSession.commit();
    dbSession.clearCache();
    ruleIndexer.index();
  }

  private void expectBadRequestException(String message) {
    thrown.expect(BadRequestException.class);
    thrown.expectMessage(message);
  }
}
