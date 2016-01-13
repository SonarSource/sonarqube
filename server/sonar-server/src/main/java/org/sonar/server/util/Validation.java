/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Strings;
import org.sonar.server.exceptions.BadRequestException;

public class Validation {

  public static final String CANT_BE_EMPTY_MESSAGE = "errors.cant_be_empty";
  public static final String IS_TOO_SHORT_MESSAGE = "errors.is_too_short";
  public static final String IS_TOO_LONG_MESSAGE = "errors.is_too_long";
  public static final String IS_ALREADY_USED_MESSAGE = "errors.is_already_used";

  private Validation() {
    // only static methods
  }

  public static void checkMandatoryParameter(String value, String paramName) {
    if (Strings.isNullOrEmpty(value)) {
      throw new BadRequestException(Validation.CANT_BE_EMPTY_MESSAGE, paramName);
    }
  }

  public static void checkMandatorySizeParameter(String value, String paramName, Integer size) {
    checkMandatoryParameter(value, paramName);
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      throw new BadRequestException(Validation.IS_TOO_LONG_MESSAGE, paramName, size);
    }
  }

}
