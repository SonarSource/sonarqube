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

package org.sonar.api.technicaldebt.server.internal;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WorkUnit;
import org.sonar.api.utils.internal.WorkDuration;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCharacteristicTest {

  @Test
  public void test_setters_and_getters_for_characteristic() throws Exception {
    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("NETWORK_USE")
      .setName("Network use")
      .setOrder(5)
      .setParentId(2)
      .setRootId(2);

    assertThat(characteristic.id()).isEqualTo(1);
    assertThat(characteristic.key()).isEqualTo("NETWORK_USE");
    assertThat(characteristic.name()).isEqualTo("Network use");
    assertThat(characteristic.order()).isEqualTo(5);
    assertThat(characteristic.ruleKey()).isNull();
    assertThat(characteristic.function()).isNull();
    assertThat(characteristic.factorValue()).isNull();
    assertThat(characteristic.factorUnit()).isNull();
    assertThat(characteristic.offsetValue()).isNull();
    assertThat(characteristic.offsetUnit()).isNull();
    assertThat(characteristic.parentId()).isEqualTo(2);
    assertThat(characteristic.rootId()).isEqualTo(2);
  }

  @Test
  public void test_setters_and_getters_for_requirement() throws Exception {
    DefaultCharacteristic requirement = new DefaultCharacteristic()
      .setId(1)
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setFunction("linear_offset")
      .setFactorValue(2)
      .setFactorUnit(WorkDuration.UNIT.MINUTES)
      .setOffsetValue(1)
      .setOffsetUnit(WorkDuration.UNIT.HOURS)
      .setRootId(3)
      .setParentId(2);

    assertThat(requirement.id()).isEqualTo(1);
    assertThat(requirement.key()).isNull();
    assertThat(requirement.name()).isNull();
    assertThat(requirement.order()).isNull();
    assertThat(requirement.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(requirement.function()).isEqualTo("linear_offset");
    assertThat(requirement.factorValue()).isEqualTo(2);
    assertThat(requirement.factorUnit()).isEqualTo(WorkDuration.UNIT.MINUTES);
    assertThat(requirement.offsetValue()).isEqualTo(1);
    assertThat(requirement.offsetUnit()).isEqualTo(WorkDuration.UNIT.HOURS);
    assertThat(requirement.parentId()).isEqualTo(2);
    assertThat(requirement.rootId()).isEqualTo(3);
  }

  @Test
  public void is_root() {
    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("NETWORK_USE")
      .setName("Network use")
      .setOrder(5)
      .setParentId(null)
      .setRootId(null);

    assertThat(characteristic.isRoot()).isTrue();
  }

  @Test
  public void is_requirement() {
    DefaultCharacteristic requirement = new DefaultCharacteristic()
      .setId(1)
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setFunction("linear_offset")
      .setFactorValue(2)
      .setFactorUnit(WorkDuration.UNIT.MINUTES)
      .setOffsetValue(1)
      .setOffsetUnit(WorkDuration.UNIT.HOURS)
      .setRootId(3)
      .setParentId(2);

    assertThat(requirement.isRequirement()).isTrue();
  }

  @Test
  public void test_equals() throws Exception {
    assertThat(new DefaultCharacteristic().setKey("NETWORK_USE")).isEqualTo(new DefaultCharacteristic().setKey("NETWORK_USE"));
    assertThat(new DefaultCharacteristic().setKey("NETWORK_USE")).isNotEqualTo(new DefaultCharacteristic().setKey("MAINTABILITY"));

    assertThat(new DefaultCharacteristic().setRuleKey(RuleKey.of("repo", "rule"))).isEqualTo(new DefaultCharacteristic().setRuleKey(RuleKey.of("repo", "rule")));
    assertThat(new DefaultCharacteristic().setRuleKey(RuleKey.of("repo", "rule"))).isNotEqualTo(new DefaultCharacteristic().setRuleKey(RuleKey.of("repo2", "rule2")));
  }

  @Test
  public void test_hascode() throws Exception {
    assertThat(new DefaultCharacteristic().setKey("NETWORK_USE").hashCode()).isEqualTo(new DefaultCharacteristic().setKey("NETWORK_USE").hashCode());
    assertThat(new DefaultCharacteristic().setKey("NETWORK_USE").hashCode()).isNotEqualTo(new DefaultCharacteristic().setKey("MAINTABILITY").hashCode());

    assertThat(new DefaultCharacteristic().setRuleKey(RuleKey.of("repo", "rule")).hashCode()).isEqualTo(new DefaultCharacteristic().setRuleKey(RuleKey.of("repo", "rule")).hashCode());
    assertThat(new DefaultCharacteristic().setRuleKey(RuleKey.of("repo", "rule")).hashCode()).isNotEqualTo(new DefaultCharacteristic().setRuleKey(RuleKey.of("repo2", "rule2")).hashCode());
  }

  @Test
  public void test_deprecated_setters_and_getters_for_characteristic() throws Exception {
    DefaultCharacteristic requirement = new DefaultCharacteristic()
      .setId(1)
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setFunction("linear_offset")
      .setFactor(WorkUnit.create(2d, WorkUnit.MINUTES))
      .setOffset(WorkUnit.create(1d, WorkUnit.HOURS));

    assertThat(requirement.factor()).isEqualTo(WorkUnit.create(2d, WorkUnit.MINUTES));
    assertThat(requirement.offset()).isEqualTo(WorkUnit.create(1d, WorkUnit.HOURS));

    assertThat(new DefaultCharacteristic()
      .setId(1)
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setFunction("linear")
      .setFactor(WorkUnit.create(2d, WorkUnit.DAYS))
      .factor()).isEqualTo(WorkUnit.create(2d, WorkUnit.DAYS));
  }

}
