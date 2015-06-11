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

import java.util.Arrays;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class DebtModelHolderImplTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  DebtModelHolderImpl sut = new DebtModelHolderImpl();

  @Test
  public void add_characteristics() throws Exception {
    sut.addCharacteristics(new CharacteristicImpl(1, "PORTABILITY"),
      Arrays.asList(new CharacteristicImpl(2, "COMPILER_RELATED_PORTABILITY"), new CharacteristicImpl(4, "READABILITY")));
    sut.addCharacteristics(new CharacteristicImpl(3, "MAINTAINABILITY"), Collections.singletonList(new CharacteristicImpl(4, "READABILITY")));

    assertThat(sut.rootCharacteristics()).hasSize(2);
    assertThat(sut.subCharacteristicsByRootKey("PORTABILITY")).hasSize(2);
  }

  @Test
  public void add_characteristics_fail_with_a_NPE_if_root_characteristic_is_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("rootCharacteristic cannot be null");

    sut.addCharacteristics(null, Collections.singletonList(new CharacteristicImpl(2, "COMPILER_RELATED_PORTABILITY")));
  }

  @Test
  public void add_characteristics_fail_with_a_NPE_if_sub_characteristics_are_null() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("subCharacteristics cannot be null");

    sut.addCharacteristics(new CharacteristicImpl(1, "PORTABILITY"), null);
  }

  @Test
  public void add_characteristics_fail_with_a_ISE_if_sub_characteristics_are_empty() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("subCharacteristics cannot be empty");

    sut.addCharacteristics(new CharacteristicImpl(1, "PORTABILITY"), Collections.<Characteristic>emptyList());
  }

  @Test
  public void get_characteristic_by_key() throws Exception {
    sut.addCharacteristics(new CharacteristicImpl(1, "PORTABILITY"), Collections.singletonList(new CharacteristicImpl(2, "COMPILER_RELATED_PORTABILITY")));

    assertThat(sut.characteristicByKey("PORTABILITY")).isNotNull();
    assertThat(sut.characteristicByKey("COMPILER_RELATED_PORTABILITY")).isNotNull();
    assertThat(sut.characteristicByKey("UNKNOWN")).isNull();
  }

  @Test
  public void get_characteristic_by_key_throws_a_ISE_when_not_initialized() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Characteristics have not been initialized yet");

    sut.characteristicByKey("PORTABILITY");
  }

  @Test
  public void get_root_characteristics() throws Exception {
    sut.addCharacteristics(new CharacteristicImpl(1, "PORTABILITY"),
      Arrays.asList(new CharacteristicImpl(2, "COMPILER_RELATED_PORTABILITY"), new CharacteristicImpl(4, "READABILITY")));
    sut.addCharacteristics(new CharacteristicImpl(3, "MAINTAINABILITY"), Collections.singletonList(new CharacteristicImpl(4, "READABILITY")));

    assertThat(sut.rootCharacteristics()).hasSize(2);
  }

  @Test
  public void get_root_characteristics_throws_a_ISE_when_not_initialized() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Characteristics have not been initialized yet");

    sut.rootCharacteristics();
  }

  @Test
  public void get_sub_characteristics_by_root_key() throws Exception {
    sut.addCharacteristics(new CharacteristicImpl(1, "PORTABILITY"),
      Arrays.asList(new CharacteristicImpl(2, "COMPILER_RELATED_PORTABILITY"), new CharacteristicImpl(4, "READABILITY")));

    assertThat(sut.subCharacteristicsByRootKey("PORTABILITY")).hasSize(2);
    assertThat(sut.subCharacteristicsByRootKey("UNKNOWN")).isEmpty();
  }

  @Test
  public void get_sub_characteristics_by_root_key_throws_a_ISE_when_not_initialized() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Characteristics have not been initialized yet");

    sut.subCharacteristicsByRootKey("PORTABILITY");
  }
}
