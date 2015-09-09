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

package org.sonar.server.computation.debt;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class CharacteristicImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void test_getter_and_setter() throws Exception {
    CharacteristicImpl characteristic = new CharacteristicImpl(1, "PORTABILITY", null);
    assertThat(characteristic.getId()).isEqualTo(1);
    assertThat(characteristic.getKey()).isEqualTo("PORTABILITY");
    assertThat(characteristic.getParentId()).isNull();
  }

  @Test
  public void test_to_string() throws Exception {
    assertThat(new CharacteristicImpl(1, "PORTABILITY", null).toString()).isEqualTo("Characteristic{id=1, key='PORTABILITY', parentId=null}");
  }

  @Test
  public void test_equals_and_hashcode() throws Exception {
    CharacteristicImpl characteristic = new CharacteristicImpl(1, "PORTABILITY", null);
    CharacteristicImpl sameCharacteristic = new CharacteristicImpl(1, "PORTABILITY", null);
    CharacteristicImpl anotherCharacteristic = new CharacteristicImpl(2, "MAINTABILITY", null);

    assertThat(characteristic).isEqualTo(characteristic);
    assertThat(characteristic).isEqualTo(sameCharacteristic);
    assertThat(characteristic).isNotEqualTo(anotherCharacteristic);
    assertThat(characteristic).isNotEqualTo(null);
    assertThat(characteristic).isNotEqualTo("foo");

    assertThat(characteristic.hashCode()).isEqualTo(characteristic.hashCode());
    assertThat(characteristic.hashCode()).isEqualTo(sameCharacteristic.hashCode());
    assertThat(characteristic.hashCode()).isNotEqualTo(anotherCharacteristic.hashCode());
  }

  @Test
  public void creating_a_new_characteristic_with_null_key_throws_a_NPE() {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("key cannot be null");

    new CharacteristicImpl(1, null, null);
  }

}
