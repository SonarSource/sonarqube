/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.v2.common.model;

import javax.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NullOrNotEmptyValidatorTest {

  NullOrNotEmptyValidator validator = new NullOrNotEmptyValidator();
  ConstraintValidatorContext context = mock();

  @Test
  void isValid_shouldValidateNull() {
    assertTrue(validator.isValid(null, context));
  }

  @Test
  void isValid_shouldValidateNotEmptyString() {
    assertTrue(validator.isValid("not-empty", context));
  }

  @Test
  void isValid_shouldNotValidateEmptyString() {
    assertFalse(validator.isValid("", context));
  }

}