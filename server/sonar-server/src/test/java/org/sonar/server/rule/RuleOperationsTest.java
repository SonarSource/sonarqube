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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.debt.CharacteristicDao;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.RuleOperations.RuleChange;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.MockUserSession;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RuleOperationsTest {
  @Mock
  DbClient dbClient;

  @Mock
  DbSession session;

  @Mock
  RuleDao ruleDao;

  @Mock
  CharacteristicDao characteristicDao;

  @Captor
  ArgumentCaptor<RuleDto> ruleCaptor;

  UserSession authorizedUserSession = new MockUserSession("nicolas").setName("Nicolas").setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);

  RuleOperations operations;

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.deprecatedRuleDao()).thenReturn(ruleDao);
    when(dbClient.debtCharacteristicDao()).thenReturn(characteristicDao);
    operations = new RuleOperations(dbClient);
  }

  @Test
  public void update_rule() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(6).setRemediationFunction("CONSTANT_ISSUE").setRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);

    // Call when reindexing rule in E/S
    when(characteristicDao.selectById(session, 2)).thenReturn(subCharacteristic);
    CharacteristicDto characteristic = new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(2);
    when(characteristicDao.selectById(session, 1)).thenReturn(characteristic);

    operations.updateRule(
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
        .setDebtRemediationFunction("LINEAR_OFFSET").setDebtRemediationCoefficient("2h").setDebtRemediationOffset("20min"),
      authorizedUserSession
    );

    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verify(session).commit();

    RuleDto result = ruleCaptor.getValue();

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getSubCharacteristicId()).isEqualTo(2);
    assertThat(result.getRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(result.getRemediationCoefficient()).isEqualTo("2h");
    assertThat(result.getRemediationOffset()).isEqualTo("20min");
  }

  @Test
  public void update_rule_set_overridden_values_to_null_when_new_values_are_equals_to_default_values() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(6).setRemediationFunction("CONSTANT_ISSUE").setRemediationOffset("10min")
      .setDefaultSubCharacteristicId(2).setDefaultRemediationFunction("CONSTANT_ISSUE").setDefaultRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);
    when(characteristicDao.selectById(session, 2)).thenReturn(subCharacteristic);
    CharacteristicDto characteristic = new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(2);
    when(characteristicDao.selectById(session, 1)).thenReturn(characteristic);

    operations.updateRule(
      // Same value as default values -> overridden values will be set to null
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
        .setDebtRemediationFunction("CONSTANT_ISSUE").setDebtRemediationOffset("10min"),
      authorizedUserSession
    );

    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verify(session).commit();

    RuleDto result = ruleCaptor.getValue();

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getSubCharacteristicId()).isNull();
    assertThat(result.getRemediationFunction()).isNull();
    assertThat(result.getRemediationCoefficient()).isNull();
    assertThat(result.getRemediationOffset()).isNull();
  }

  @Test
  public void not_update_rule_if_same_sub_characteristic_and_function() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(2).setRemediationFunction("CONSTANT_ISSUE").setRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);

    operations.updateRule(
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
        .setDebtRemediationFunction("CONSTANT_ISSUE").setDebtRemediationOffset("10min"),
      authorizedUserSession
    );

    verify(ruleDao, never()).update(eq(session), any(RuleDto.class));
    verify(session, never()).commit();
  }

  @Test
  public void update_rule_set_characteristic_if_different_from_default_one() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setDefaultSubCharacteristicId(2).setDefaultRemediationFunction("CONSTANT_ISSUE").setDefaultRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);
    when(characteristicDao.selectById(session, 2)).thenReturn(subCharacteristic);
    CharacteristicDto characteristic = new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(2);
    when(characteristicDao.selectById(session, 1)).thenReturn(characteristic);

    operations.updateRule(
      // Remediation function is not the same as default one -> Overridden value should be set
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
        .setDebtRemediationFunction("LINEAR_OFFSET").setDebtRemediationCoefficient("2h").setDebtRemediationOffset("20min"),
      authorizedUserSession
    );

    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verify(session).commit();

    RuleDto result = ruleCaptor.getValue();

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getSubCharacteristicId()).isEqualTo(2);
    assertThat(result.getRemediationFunction()).isEqualTo("LINEAR_OFFSET");
    assertThat(result.getRemediationCoefficient()).isEqualTo("2h");
    assertThat(result.getRemediationOffset()).isEqualTo("20min");
  }

  @Test
  public void update_rule_set_remediation_function_if_different_from_default_one() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setDefaultSubCharacteristicId(6).setDefaultRemediationFunction("CONSTANT_ISSUE").setDefaultRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);
    when(characteristicDao.selectById(session, 2)).thenReturn(subCharacteristic);
    CharacteristicDto characteristic = new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(2);
    when(characteristicDao.selectById(session, 1)).thenReturn(characteristic);

    operations.updateRule(
      // Characteristic is the not same as the default one -> Overridden values should be set
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
        .setDebtRemediationFunction("CONSTANT_ISSUE").setDebtRemediationOffset("10min"),
      authorizedUserSession
    );

    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verify(session).commit();

    RuleDto result = ruleCaptor.getValue();

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getSubCharacteristicId()).isEqualTo(2);
    assertThat(result.getRemediationFunction()).isEqualTo("CONSTANT_ISSUE");
    assertThat(result.getRemediationCoefficient()).isNull();
    assertThat(result.getRemediationOffset()).isEqualTo("10min");
  }

  @Test
  public void disable_rule_debt_when_update_rule_with_no_sub_characteristic() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setDefaultSubCharacteristicId(6).setDefaultRemediationFunction("LINEAR").setDefaultRemediationCoefficient("10min")
      .setSubCharacteristicId(6)
      .setRemediationFunction("CONSTANT_ISSUE")
      .setRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    operations.updateRule(new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey(null), authorizedUserSession);

    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verify(session).commit();

    RuleDto result = ruleCaptor.getValue();

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getSubCharacteristicId()).isEqualTo(-1);
    assertThat(result.getRemediationFunction()).isNull();
    assertThat(result.getRemediationCoefficient()).isNull();
    assertThat(result.getRemediationOffset()).isNull();
  }

  @Test
  public void disable_rule_debt_when_update_rule_with_no_function() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setDefaultSubCharacteristicId(6).setDefaultRemediationFunction("CONSTANT_ISSUE").setDefaultRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);
    when(characteristicDao.selectById(session, 2)).thenReturn(subCharacteristic);
    CharacteristicDto characteristic = new CharacteristicDto().setId(1).setKey("PORTABILITY").setName("Portability").setOrder(2);
    when(characteristicDao.selectById(session, 1)).thenReturn(characteristic);

    operations.updateRule(new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER"), authorizedUserSession);

    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verify(session).commit();

    RuleDto result = ruleCaptor.getValue();

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getSubCharacteristicId()).isEqualTo(-1);
    assertThat(result.getRemediationFunction()).isNull();
    assertThat(result.getRemediationCoefficient()).isNull();
    assertThat(result.getRemediationOffset()).isNull();
  }

  @Test
  public void disable_characteristic_on_rule_having_no_debt_info() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    operations.updateRule(new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey(null), authorizedUserSession);

    verify(ruleDao).update(eq(session), ruleCaptor.capture());
    verify(session).commit();

    RuleDto result = ruleCaptor.getValue();

    assertThat(result.getId()).isEqualTo(1);
    assertThat(result.getSubCharacteristicId()).isNull();
    assertThat(result.getRemediationFunction()).isNull();
    assertThat(result.getRemediationCoefficient()).isNull();
    assertThat(result.getRemediationOffset()).isNull();
  }

  @Test
  public void not_disable_characteristic_when_update_rule_if_already_disabled() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck").setSubCharacteristicId(-1);
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    operations.updateRule(
      new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey(null),
      authorizedUserSession
    );

    verify(ruleDao, never()).update(eq(session), any(RuleDto.class));
    verify(session, never()).commit();
  }

  @Test
  public void fail_to_update_rule_on_unknown_rule() {
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(null);

    try {
      operations.updateRule(
        new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
          .setDebtRemediationFunction("LINEAR_OFFSET").setDebtRemediationCoefficient("2h").setDebtRemediationOffset("20min"),
        authorizedUserSession
      );
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }

    verify(ruleDao, never()).update(eq(session), any(RuleDto.class));
    verify(session, never()).commit();
  }

  @Test
  public void fail_to_update_rule_on_unknown_sub_characteristic() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(2).setRemediationFunction("CONSTANT_ISSUE").setRemediationOffset("10min");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(null);

    try {
      operations.updateRule(
        new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
          .setDebtRemediationFunction("LINEAR_OFFSET").setDebtRemediationCoefficient("2h").setDebtRemediationOffset("20min"),
        authorizedUserSession
      );
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class);
    }

    verify(ruleDao, never()).update(eq(session), any(RuleDto.class));
    verify(session, never()).commit();
  }

  @Test
  public void fail_to_update_rule_on_invalid_coefficient() {
    RuleDto dto = new RuleDto().setId(1).setRepositoryKey("squid").setRuleKey("UselessImportCheck")
      .setSubCharacteristicId(2).setRemediationFunction("LINEAR").setRemediationCoefficient("1h");
    RuleKey ruleKey = RuleKey.of("squid", "UselessImportCheck");

    when(ruleDao.getNullableByKey(session, ruleKey)).thenReturn(dto);

    CharacteristicDto subCharacteristic = new CharacteristicDto().setId(2).setKey("COMPILER").setName("Compiler").setParentId(1);
    when(characteristicDao.selectByKey("COMPILER", session)).thenReturn(subCharacteristic);

    try {
      operations.updateRule(
        new RuleChange().setRuleKey(ruleKey).setDebtCharacteristicKey("COMPILER")
          .setDebtRemediationFunction("LINEAR").setDebtRemediationCoefficient("foo"),
        authorizedUserSession
      );
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class).hasMessage("Invalid coefficient: foo (Duration 'foo' is invalid, it should use the following sample format : 2d 10h 15min)");
    }

    verify(ruleDao, never()).update(eq(session), any(RuleDto.class));
    verify(session, never()).commit();
  }
}
