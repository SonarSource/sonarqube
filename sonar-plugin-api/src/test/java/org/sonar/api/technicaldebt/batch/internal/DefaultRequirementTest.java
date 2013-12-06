/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import java.text.SimpleDateFormat;

import static org.fest.assertions.Assertions.assertThat;

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
      .setFactor(WorkUnit.create(2.0, "mn"))
      .setOffset(WorkUnit.create(1.0, "h"))
      .setCreatedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"))
      .setUpdatedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"));

    assertThat(requirement.id()).isEqualTo(3);
    assertThat(requirement.ruleKey()).isEqualTo(RuleKey.of("repo", "rule"));
    assertThat(requirement.characteristic()).isEqualTo(characteristic);
    assertThat(requirement.rootCharacteristic()).isEqualTo(root);
    assertThat(requirement.function()).isEqualTo("linear_offset");
    assertThat(requirement.factor()).isEqualTo(WorkUnit.create(2.0, "mn"));
    assertThat(requirement.offset()).isEqualTo(WorkUnit.create(1.0, "h"));
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
  }

  @Test
  public void test_hascode() throws Exception {
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
}
