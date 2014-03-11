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

import java.text.SimpleDateFormat;

import static org.fest.assertions.Assertions.assertThat;

public class DefaultCharacteristicTest {

  @Test
  public void test_setters_and_getters_for_characteristic() throws Exception {
    DefaultCharacteristic root = new DefaultCharacteristic().setKey("REUSABILITY");

    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("MODULARITY")
      .setName("Modularity")
      .setOrder(5)
      .setParent(root)
      .setRoot(root)
      .setCreatedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"))
      .setUpdatedAt(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"));

    assertThat(characteristic.id()).isEqualTo(1);
    assertThat(characteristic.key()).isEqualTo("MODULARITY");
    assertThat(characteristic.name()).isEqualTo("Modularity");
    assertThat(characteristic.order()).isEqualTo(5);
    assertThat(characteristic.parent()).isEqualTo(root);
    assertThat(characteristic.root()).isEqualTo(root);
    assertThat(characteristic.createdAt()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"));
    assertThat(characteristic.updatedAt()).isEqualTo(new SimpleDateFormat("yyyy-MM-dd").parse("2013-08-19"));
  }

  @Test
  public void set_name_as_key() throws Exception {
    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setName("Compiler related portability", true);

    assertThat(characteristic.key()).isEqualTo("COMPILER_RELATED_PORTABILITY");
    assertThat(characteristic.name()).isEqualTo("Compiler related portability");
  }

  @Test
  public void add_requirement() throws Exception {
    DefaultRequirement requirement = new DefaultRequirement().setRuleKey(RuleKey.of("repo", "rule"));

    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("MODULARITY")
      .addRequirement(requirement);

    assertThat(characteristic.requirements()).hasSize(1);
  }

  @Test
  public void add_child() throws Exception {
    DefaultCharacteristic root = new DefaultCharacteristic()
      .setId(1)
      .setKey("REUSABILITY");

    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("MODULARITY")
      .setParent(root);

    assertThat(root.children()).hasSize(1);
    assertThat(characteristic.parent()).isEqualTo(root);
  }



  @Test
  public void is_root() throws Exception {
    DefaultCharacteristic characteristic = new DefaultCharacteristic()
      .setId(1)
      .setKey("NETWORK_USE")
      .setName("Network use")
      .setOrder(5)
      .setParent(null)
      .setRoot(null);

    assertThat(characteristic.isRoot()).isTrue();
  }

  @Test
  public void test_equals() throws Exception {
    assertThat(new DefaultCharacteristic().setKey("NETWORK_USE")).isEqualTo(new DefaultCharacteristic().setKey("NETWORK_USE"));
    assertThat(new DefaultCharacteristic().setKey("NETWORK_USE")).isNotEqualTo(new DefaultCharacteristic().setKey("MAINTABILITY"));
  }

  @Test
  public void test_hascode() throws Exception {
    assertThat(new DefaultCharacteristic().setKey("NETWORK_USE").hashCode()).isEqualTo(new DefaultCharacteristic().setKey("NETWORK_USE").hashCode());
    assertThat(new DefaultCharacteristic().setKey("NETWORK_USE").hashCode()).isNotEqualTo(new DefaultCharacteristic().setKey("MAINTABILITY").hashCode());
  }

}
