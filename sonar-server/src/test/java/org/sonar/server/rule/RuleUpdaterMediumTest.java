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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.db.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

public class RuleUpdaterMediumTest {

  private static final RuleKey RULE_KEY = RuleKey.of("squid", "S001");

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db = tester.get(DbClient.class);
  DbSession dbSession;
  System2 system = mock(System2.class);
  RuleUpdater updater = new RuleUpdater(db, system);
  int reliabilityId, softReliabilityId, hardReliabilityId;

  @Before
  public void before() {
    tester.clearDbAndEs();
    dbSession = db.openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void no_changes() throws Exception {
    grantPermission();
    db.ruleDao().insert(dbSession, RuleTests.newDto(RULE_KEY)
      // the following fields are not supposed to be updated
      .setNoteData("my *note*")
      .setNoteUserLogin("me")
      .setTags(ImmutableSet.of("tag1"))
      .setSubCharacteristicId(33)
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationCoefficient("1d")
      .setRemediationOffset("5min"));
    dbSession.commit();

    RuleUpdate update = new RuleUpdate(RULE_KEY);
    assertThat(update.isEmpty()).isTrue();
    updater.update(update, UserSession.get());

    dbSession.clearCache();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RULE_KEY);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserLogin()).isEqualTo("me");
    assertThat(rule.getTags()).containsOnly("tag1");
    assertThat(rule.getSubCharacteristicId()).isEqualTo(33);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationCoefficient()).isEqualTo("1d");
    assertThat(rule.getRemediationOffset()).isEqualTo("5min");
  }

  @Test
  public void set_markdown_note() throws Exception {
    grantPermission();
    db.ruleDao().insert(dbSession, RuleTests.newDto(RULE_KEY)
      .setNoteData(null)
      .setNoteUserLogin(null)

        // the following fields are not supposed to be updated
      .setTags(ImmutableSet.of("tag1"))
      .setSubCharacteristicId(33)
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationCoefficient("1d")
      .setRemediationOffset("5min"));
    dbSession.commit();

    RuleUpdate update = new RuleUpdate(RULE_KEY);
    update.setMarkdownNote("my *note*");
    updater.update(update, UserSession.get());

    dbSession.clearCache();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RULE_KEY);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserLogin()).isEqualTo("me");
    assertThat(rule.getNoteCreatedAt()).isNotNull();
    assertThat(rule.getNoteUpdatedAt()).isNotNull();
    // no other changes
    assertThat(rule.getTags()).containsOnly("tag1");
    assertThat(rule.getSubCharacteristicId()).isEqualTo(33);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationCoefficient()).isEqualTo("1d");
    assertThat(rule.getRemediationOffset()).isEqualTo("5min");
  }

  @Test
  public void delete_markdown_note() throws Exception {
    grantPermission();
    db.ruleDao().insert(dbSession, RuleTests.newDto(RULE_KEY)
      .setNoteData("my *note*")
      .setNoteUserLogin("me"));
    dbSession.commit();

    RuleUpdate update = new RuleUpdate(RULE_KEY).setMarkdownNote(null);
    updater.update(update, UserSession.get());

    dbSession.clearCache();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RULE_KEY);
    assertThat(rule.getNoteData()).isNull();
    assertThat(rule.getNoteUserLogin()).isNull();
    assertThat(rule.getNoteCreatedAt()).isNull();
    assertThat(rule.getNoteUpdatedAt()).isNull();
  }

  @Test
  public void set_tags() throws Exception {
    grantPermission();
    // insert db
    db.ruleDao().insert(dbSession, RuleTests.newDto(RULE_KEY)
        .setTags(Sets.newHashSet("security"))
        .setSystemTags(Sets.newHashSet("java8", "javadoc"))
    );
    dbSession.commit();

    // java8 is a system tag -> ignore
    RuleUpdate update = new RuleUpdate(RULE_KEY).setTags(Sets.newHashSet("bug", "java8"));
    updater.update(update, UserSession.get());

    dbSession.clearCache();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RULE_KEY);
    assertThat(rule.getTags()).containsOnly("bug");
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    Set<String> tags = tester.get(RuleService.class).listTags();
    assertThat(tags).containsOnly("bug", "java8", "javadoc");
  }

  @Test
  public void remove_tags() throws Exception {
    grantPermission();
    db.ruleDao().insert(dbSession, RuleTests.newDto(RULE_KEY)
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc")));
    dbSession.commit();

    RuleUpdate update = new RuleUpdate(RULE_KEY).setTags(null);
    updater.update(update, UserSession.get());

    dbSession.clearCache();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RULE_KEY);
    assertThat(rule.getTags()).isEmpty();
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    Set<String> tags = tester.get(RuleService.class).listTags();
    assertThat(tags).containsOnly("java8", "javadoc");
  }

  @Test
  public void fail_to_update_if_not_granted() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    db.ruleDao().insert(dbSession, RuleTests.newDto(RULE_KEY)
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc")));
    dbSession.commit();

    RuleUpdater updater = new RuleUpdater(tester.get(DbClient.class), system);
    RuleUpdate update = new RuleUpdate(RULE_KEY).setMarkdownNote("my *note*");

    try {
      updater.update(update, UserSession.get());
      fail();
    } catch (UnauthorizedException e) {
      // ok
    }
  }

  @Test
  public void override_debt() throws Exception {
    grantPermission();
    insertDebtCharacteristics(dbSession);
    db.ruleDao().insert(dbSession, RuleTests.newDto(RULE_KEY)
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefaultRemediationCoefficient("1d")
      .setDefaultRemediationOffset("5min")
      .setRemediationFunction(null)
      .setRemediationCoefficient(null)
      .setRemediationOffset(null));
    dbSession.commit();

    DefaultDebtRemediationFunction fn = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "1min");
    RuleUpdate update = new RuleUpdate(RULE_KEY)
      .setDebtSubCharacteristic("SOFT_RELIABILITY")
      .setDebtRemediationFunction(fn);
    updater.update(update, UserSession.get());

    // verify db
    dbSession.clearCache();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RULE_KEY);
    assertThat(rule.getSubCharacteristicId()).isEqualTo(softReliabilityId);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isEqualTo("1min");

    // verify index
    Rule indexedRule = tester.get(RuleIndex.class).getByKey(RULE_KEY);
    assertThat(indexedRule.debtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.debtSubCharacteristicKey()).isEqualTo("SOFT_RELIABILITY");
    assertThat(indexedRule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(indexedRule.debtRemediationFunction().coefficient()).isNull();
    assertThat(indexedRule.debtRemediationFunction().offset()).isEqualTo("1min");
  }

  @Test
  public void reset_debt() throws Exception {
    grantPermission();
    insertDebtCharacteristics(dbSession);
    db.ruleDao().insert(dbSession, RuleTests.newDto(RULE_KEY)
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefaultRemediationCoefficient("1d")
      .setDefaultRemediationOffset("5min")
      .setSubCharacteristicId(softReliabilityId)
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationCoefficient(null)
      .setRemediationOffset("1min"));
    dbSession.commit();

    RuleUpdate update = new RuleUpdate(RULE_KEY)
      .setDebtSubCharacteristic(RuleUpdate.DEFAULT_DEBT_CHARACTERISTIC);
    updater.update(update, UserSession.get());

    // verify db
    dbSession.clearCache();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RULE_KEY);
    assertThat(rule.getSubCharacteristicId()).isNull();
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();

    // verify index
    Rule indexedRule = tester.get(RuleIndex.class).getByKey(RULE_KEY);
    assertThat(indexedRule.debtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.debtSubCharacteristicKey()).isEqualTo("HARD_RELIABILITY");
    assertThat(indexedRule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(indexedRule.debtRemediationFunction().coefficient()).isEqualTo("1d");
    assertThat(indexedRule.debtRemediationFunction().offset()).isEqualTo("5min");
  }

  @Test
  public void unset_debt() throws Exception {
    grantPermission();
    insertDebtCharacteristics(dbSession);
    db.ruleDao().insert(dbSession, RuleTests.newDto(RULE_KEY)
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefaultRemediationCoefficient("1d")
      .setDefaultRemediationOffset("5min")
      .setSubCharacteristicId(softReliabilityId)
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationCoefficient(null)
      .setRemediationOffset("1min"));
    dbSession.commit();

    RuleUpdate update = new RuleUpdate(RULE_KEY)
      .setDebtSubCharacteristic(null);
    updater.update(update, UserSession.get());

    // verify db
    dbSession.clearCache();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RULE_KEY);
    assertThat(rule.getSubCharacteristicId()).isEqualTo(-1);
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();

    // verify index
    Rule indexedRule = tester.get(RuleIndex.class).getByKey(RULE_KEY);
    assertThat(indexedRule.debtCharacteristicKey()).isNull();
    assertThat(indexedRule.debtSubCharacteristicKey()).isNull();
    //TODO pb with db code -1 ? assertThat(indexedRule.debtRemediationFunction()).isNull();
  }

  private void grantPermission() {
    MockUserSession.set()
      .setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN)
      .setLogin("me");
  }

  private void insertDebtCharacteristics(DbSession dbSession) {
    CharacteristicDto reliability = new CharacteristicDto()
      .setKey("RELIABILITY")
      .setName("Reliability")
      .setEnabled(true);
    db.debtCharacteristicDao().insert(reliability, dbSession);
    reliabilityId = reliability.getId();

    CharacteristicDto softReliability = new CharacteristicDto()
      .setKey("SOFT_RELIABILITY")
      .setName("Software reliability")
      .setParentId(reliability.getId())
      .setEnabled(true);
    db.debtCharacteristicDao().insert(softReliability, dbSession);
    softReliabilityId = softReliability.getId();

    CharacteristicDto hardReliability = new CharacteristicDto()
      .setKey("HARD_RELIABILITY")
      .setName("Hardware reliability")
      .setParentId(reliability.getId())
      .setEnabled(true);
    db.debtCharacteristicDao().insert(hardReliability, dbSession);
    hardReliabilityId = hardReliability.getId();
  }
}
