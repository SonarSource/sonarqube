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

package org.sonar.api.technicaldebt.batch.internal;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WorkUnit;
import org.sonar.api.utils.internal.WorkDuration;

import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultRequirementTest {

  @Test
  public void test_setters_and_getters_for_characteristic() throws Exception {
    DefaultCharacteristic root = new DefaultCharacteristic().setId(1).setKey("REUSABILITY");

    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("MODULARITY")
      .setName("Modularity")
      .setParent(root)
      .setRoot(root);

    DefaultRequirement requirement = new DefaultRequirement()
      .setId(3)
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setCharacteristic(characteristic)
      .setRootCharacteristic(root)
      .setFunction("linear_offset")
      .setFactorValue(2)
      .setFactorUnit(WorkDuration.UNIT.MINUTES)
      .setOffsetValue(1)
      .setOffsetUnit(WorkDuration.UNIT.HOURS)
      .setCreatedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"))
      .setUpdatedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"));

    assertThat(requirement.id()).isEqualTo(3);
    assertThat(requirement.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(requirement.characteristic()).isEqualTo(characteristic);
    assertThat(requirement.rootCharacteristic()).isEqualTo(root);
    assertThat(requirement.function()).isEqualTo("linear_offset");
    assertThat(requirement.factorValue()).isEqualTo(2);
    assertThat(requirement.factorUnit()).isEqualTo(WorkDuration.UNIT.MINUTES);
    assertThat(requirement.factor()).isEqualTo(WorkUnit.create(2d, WorkUnit.MINUTES));
    assertThat(requirement.offsetValue()).isEqualTo(1);
    assertThat(requirement.offsetUnit()).isEqualTo(WorkDuration.UNIT.HOURS);
    assertThat(requirement.offset()).isEqualTo(WorkUnit.create(1d, WorkUnit.HOURS));
    assertThat(requirement.createdAt()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"));
    assertThat(requirement.updatedAt()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"));
  }

  @Test
  public void test_equals() throws Exception {
    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("MODULARITY");

    assertThat(new DefaultRequirement().setRuleKey(RuleKey.of("repo", "rule")).setCharacteristic(characteristic))
      .isEqualTo(new DefaultRequirement().setRuleKey(RuleKey.of("repo", "rule")).setCharacteristic(characteristic));
    assertThat(new DefaultRequirement().setRuleKey(RuleKey.of("repo", "rule")).setCharacteristic(characteristic))
      .isNotEqualTo(new DefaultRequirement().setRuleKey(RuleKey.of("repo2", "rule2")).setCharacteristic(characteristic));

    assertThat(new DefaultRequirement().setRuleKey(RuleKey.of("repo", "rule")).setCharacteristic(characteristic))
      .isNotEqualTo(new DefaultRequirement().setRuleKey(RuleKey.of("repo", "rule")).setCharacteristic(
        new DefaultCharacteristic()
          .setId(2)
          .setKey("REUSABILITY")));

  }

  @Test
  public void test_hashcode() throws Exception {
    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("MODULARITY")
      .setName("Modularity")
      .setOrder(5);

    assertThat(new DefaultRequirement().setRuleKey(RuleKey.of("repo", "rule")).setCharacteristic(characteristic).hashCode())
      .isEqualTo(new DefaultRequirement().setRuleKey(RuleKey.of("repo", "rule")).setCharacteristic(characteristic).hashCode());
    assertThat(new DefaultRequirement().setRuleKey(RuleKey.of("repo", "rule")).setCharacteristic(characteristic).hashCode())
      .isNotEqualTo(new DefaultRequirement().setRuleKey(RuleKey.of("repo2", "rule2")).setCharacteristic(characteristic).hashCode());
  }

  @Test
  public void test_deprecated_setters_and_getters_for_characteristic() throws Exception {
    DefaultCharacteristic root = new DefaultCharacteristic().setId(1).setKey("REUSABILITY");

    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("MODULARITY")
      .setName("Modularity")
      .setParent(root)
      .setRoot(root);

    DefaultRequirement requirement = new DefaultRequirement()
      .setId(3)
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setCharacteristic(characteristic)
      .setRootCharacteristic(root)
      .setFunction("linear_offset")
      .setFactor(WorkUnit.create(2d, WorkUnit.MINUTES))
      .setOffset(WorkUnit.create(1d, WorkUnit.HOURS));

    assertThat(requirement.factor()).isEqualTo(WorkUnit.create(2d, WorkUnit.MINUTES));
    assertThat(requirement.offset()).isEqualTo(WorkUnit.create(1d, WorkUnit.HOURS));

    assertThat(new DefaultRequirement()
      .setId(3)
      .setRuleKey(RuleKey.of("repo", "rule"))
      .setCharacteristic(characteristic)
      .setRootCharacteristic(root)
      .setFunction("linear_offset")
      .setFactor(WorkUnit.create(2d, WorkUnit.DAYS))
      .factor()).isEqualTo(WorkUnit.create(2d, WorkUnit.DAYS));
  }
}
