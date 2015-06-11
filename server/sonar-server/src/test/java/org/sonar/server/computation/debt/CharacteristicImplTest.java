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
    CharacteristicImpl characteristic = new CharacteristicImpl(1, "PORTABILITY");
    assertThat(characteristic.id()).isEqualTo(1);
    assertThat(characteristic.key()).isEqualTo("PORTABILITY");
  }

  @Test
  public void test_to_string() throws Exception {
    assertThat(new CharacteristicImpl(1, "PORTABILITY").toString()).isNotEmpty();
  }

  @Test
  public void creating_a_new_characteristic_with_null_key_throws_a_NPE() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("key cannot be null");

    new CharacteristicImpl(1, null);
  }
}
