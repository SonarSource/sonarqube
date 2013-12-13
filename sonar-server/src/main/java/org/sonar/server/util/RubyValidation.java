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
package org.sonar.server.util;

import com.google.common.base.Strings;
import org.sonar.server.exceptions.BadRequestException;

public class RubyValidation {

  public static final String ERRORS_CANT_BE_EMPTY_MESSAGE = "errors.cant_be_empty";
  public static final String ERRORS_IS_TOO_LONG_MESSAGE = "errors.is_too_long";

  private RubyValidation() {
    // only static methods
  }

  public static void checkMandatoryParameter(String value, String paramName) {
    if (Strings.isNullOrEmpty(value)) {
      throw BadRequestException.ofL10n(RubyValidation.ERRORS_CANT_BE_EMPTY_MESSAGE, paramName);
    }
  }

  public static void checkMandatorySizeParameter(String value, String paramName, Integer size) {
    checkMandatoryParameter(value, paramName);
    if (!Strings.isNullOrEmpty(value) && value.length() > size) {
      throw BadRequestException.ofL10n(RubyValidation.ERRORS_IS_TOO_LONG_MESSAGE, paramName, size);
    }
  }

}
