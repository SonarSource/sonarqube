/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.util;

import org.junit.Test;
import org.sonar.server.exceptions.BadRequestException;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TypeValidationsTest {

  @Test
  public void validate() {
    TypeValidation fakeTypeValidation = mock(TypeValidation.class);
    when(fakeTypeValidation.key()).thenReturn("Fake");

    TypeValidations typeValidations = new TypeValidations(newArrayList(fakeTypeValidation));
    typeValidations.validate("10", "Fake", newArrayList("a"));

    verify(fakeTypeValidation).validate("10", newArrayList("a"));
  }

  @Test
  public void validate__multiple_values() {
    TypeValidation fakeTypeValidation = mock(TypeValidation.class);
    when(fakeTypeValidation.key()).thenReturn("Fake");

    TypeValidations typeValidations = new TypeValidations(newArrayList(fakeTypeValidation));
    typeValidations.validate(newArrayList("10", "11", "12"), "Fake", newArrayList("11"));

    verify(fakeTypeValidation).validate("10", newArrayList("11"));
  }

  @Test
  public void fail_on_unknown_type() {
    TypeValidation fakeTypeValidation = mock(TypeValidation.class);
    when(fakeTypeValidation.key()).thenReturn("Fake");

    try {
      TypeValidations typeValidations = new TypeValidations(newArrayList(fakeTypeValidation));
      typeValidations.validate("10", "Unknown", null);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(BadRequestException.class);
      BadRequestException badRequestException = (BadRequestException) e;
      assertThat(badRequestException.getMessage()).isEqualTo("Type 'Unknown' is not valid.");
    }
  }

}
