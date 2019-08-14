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
package org.sonar.server.exceptions;

import com.google.common.base.Optional;
import javax.annotation.Nullable;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

public class NotFoundException extends ServerException {

  public NotFoundException(String message) {
    super(HTTP_NOT_FOUND, message);
  }

  /**
   * @throws NotFoundException if the value if null
   * @return the value
   */
  public static <T> T checkFound(@Nullable T value, String message, Object... messageArguments) {
    if (value == null) {
      throw new NotFoundException(format(message, messageArguments));
    }

    return value;
  }

  /**
   * @throws NotFoundException if the value is not present
   * @return the value
   */
  public static <T> T checkFoundWithOptional(Optional<T> value, String message, Object... messageArguments) {
    if (!value.isPresent()) {
      throw new NotFoundException(format(message, messageArguments));
    }

    return value.get();
  }

  public static <T> T checkFoundWithOptional(java.util.Optional<T> value, String message, Object... messageArguments) {
    if (!value.isPresent()) {
      throw new NotFoundException(format(message, messageArguments));
    }

    return value.get();
  }
}
