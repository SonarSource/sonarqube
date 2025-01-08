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
package org.sonar.server.rule.registration;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.db.rule.DeprecatedRuleKeyDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SingleDeprecatedRuleKeyTest {

  @Test
  public void test_creation_from_DeprecatedRuleKeyDto() {
    // Creation from DeprecatedRuleKeyDto
    DeprecatedRuleKeyDto deprecatedRuleKeyDto = new DeprecatedRuleKeyDto()
      .setOldRuleKey(secure().nextAlphanumeric(50))
      .setOldRepositoryKey(secure().nextAlphanumeric(50))
      .setRuleUuid(secure().nextAlphanumeric(50))
      .setUuid(secure().nextAlphanumeric(40));

    SingleDeprecatedRuleKey singleDeprecatedRuleKey = SingleDeprecatedRuleKey.from(deprecatedRuleKeyDto);

    assertThat(singleDeprecatedRuleKey.getOldRepositoryKey()).isEqualTo(deprecatedRuleKeyDto.getOldRepositoryKey());
    assertThat(singleDeprecatedRuleKey.getOldRuleKey()).isEqualTo(deprecatedRuleKeyDto.getOldRuleKey());
    assertThat(singleDeprecatedRuleKey.getNewRepositoryKey()).isEqualTo(deprecatedRuleKeyDto.getNewRepositoryKey());
    assertThat(singleDeprecatedRuleKey.getNewRuleKey()).isEqualTo(deprecatedRuleKeyDto.getNewRuleKey());
    assertThat(singleDeprecatedRuleKey.getUuid()).isEqualTo(deprecatedRuleKeyDto.getUuid());
    assertThat(singleDeprecatedRuleKey.getRuleUuid()).isEqualTo(deprecatedRuleKeyDto.getRuleUuid());
    assertThat(singleDeprecatedRuleKey.getOldRuleKeyAsRuleKey())
      .isEqualTo(RuleKey.of(deprecatedRuleKeyDto.getOldRepositoryKey(), deprecatedRuleKeyDto.getOldRuleKey()));
  }

  @Test
  public void test_creation_from_RulesDefinitionRule() {
    // Creation from RulesDefinition.Rule
    ImmutableSet<RuleKey> deprecatedRuleKeys = ImmutableSet.of(
      RuleKey.of(secure().nextAlphanumeric(50), secure().nextAlphanumeric(50)),
      RuleKey.of(secure().nextAlphanumeric(50), secure().nextAlphanumeric(50)),
      RuleKey.of(secure().nextAlphanumeric(50), secure().nextAlphanumeric(50)));

    RulesDefinition.Repository repository = mock(RulesDefinition.Repository.class);
    when(repository.key()).thenReturn(secure().nextAlphanumeric(50));

    RulesDefinition.Rule rule = mock(RulesDefinition.Rule.class);
    when(rule.key()).thenReturn(secure().nextAlphanumeric(50));
    when(rule.deprecatedRuleKeys()).thenReturn(deprecatedRuleKeys);
    when(rule.repository()).thenReturn(repository);

    Set<SingleDeprecatedRuleKey> singleDeprecatedRuleKeys = SingleDeprecatedRuleKey.from(rule);
    assertThat(singleDeprecatedRuleKeys).hasSize(deprecatedRuleKeys.size());
    assertThat(singleDeprecatedRuleKeys)
      .extracting(SingleDeprecatedRuleKey::getUuid, SingleDeprecatedRuleKey::getOldRepositoryKey, SingleDeprecatedRuleKey::getOldRuleKey,
        SingleDeprecatedRuleKey::getNewRepositoryKey, SingleDeprecatedRuleKey::getNewRuleKey, SingleDeprecatedRuleKey::getOldRuleKeyAsRuleKey)
      .containsExactlyInAnyOrder(
        deprecatedRuleKeys.stream().map(
          r -> tuple(null, r.repository(), r.rule(), rule.repository().key(), rule.key(), RuleKey.of(r.repository(), r.rule())))
          .toList().toArray(new Tuple[deprecatedRuleKeys.size()]));
  }

  @Test
  public void test_equality() {
    DeprecatedRuleKeyDto deprecatedRuleKeyDto1 = new DeprecatedRuleKeyDto()
      .setOldRuleKey(secure().nextAlphanumeric(50))
      .setOldRepositoryKey(secure().nextAlphanumeric(50))
      .setUuid(secure().nextAlphanumeric(40))
      .setRuleUuid("some-uuid");

    DeprecatedRuleKeyDto deprecatedRuleKeyDto1WithoutUuid = new DeprecatedRuleKeyDto()
      .setOldRuleKey(deprecatedRuleKeyDto1.getOldRuleKey())
      .setOldRepositoryKey(deprecatedRuleKeyDto1.getOldRepositoryKey());

    DeprecatedRuleKeyDto deprecatedRuleKeyDto2 = new DeprecatedRuleKeyDto()
      .setOldRuleKey(secure().nextAlphanumeric(50))
      .setOldRepositoryKey(secure().nextAlphanumeric(50))
      .setUuid(secure().nextAlphanumeric(40));

    SingleDeprecatedRuleKey singleDeprecatedRuleKey1 = SingleDeprecatedRuleKey.from(deprecatedRuleKeyDto1);
    SingleDeprecatedRuleKey singleDeprecatedRuleKey2 = SingleDeprecatedRuleKey.from(deprecatedRuleKeyDto2);

    assertThat(singleDeprecatedRuleKey1)
      .isEqualTo(singleDeprecatedRuleKey1)
      .isEqualTo(SingleDeprecatedRuleKey.from(deprecatedRuleKeyDto1))
      .isEqualTo(SingleDeprecatedRuleKey.from(deprecatedRuleKeyDto1WithoutUuid));
    assertThat(singleDeprecatedRuleKey2).isEqualTo(SingleDeprecatedRuleKey.from(deprecatedRuleKeyDto2));

    assertThat(singleDeprecatedRuleKey1)
      .hasSameHashCodeAs(singleDeprecatedRuleKey1)
      .hasSameHashCodeAs(SingleDeprecatedRuleKey.from(deprecatedRuleKeyDto1))
      .hasSameHashCodeAs(SingleDeprecatedRuleKey.from(deprecatedRuleKeyDto1WithoutUuid));
    assertThat(singleDeprecatedRuleKey2).hasSameHashCodeAs(SingleDeprecatedRuleKey.from(deprecatedRuleKeyDto2));

    assertThat(singleDeprecatedRuleKey1)
      .isNotNull()
      .isNotEqualTo("")
      .isNotEqualTo(null)
      .isNotEqualTo(singleDeprecatedRuleKey2);
    assertThat(singleDeprecatedRuleKey2).isNotEqualTo(singleDeprecatedRuleKey1);

    assertThat(singleDeprecatedRuleKey1.hashCode()).isNotEqualTo(singleDeprecatedRuleKey2.hashCode());
    assertThat(singleDeprecatedRuleKey2.hashCode()).isNotEqualTo(singleDeprecatedRuleKey1.hashCode());
  }
}
