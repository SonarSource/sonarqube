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
package org.sonar.server.exceptions;

import javax.annotation.Nullable;

/**
 * Request is not valid and can not be processed.
 */
public class BadRequestException extends ServerException {

  private static final int BAD_REQUEST = 400;

  public BadRequestException(String message) {
    super(BAD_REQUEST, message);
  }

  public BadRequestException(@Nullable String message, @Nullable String l10nKey, @Nullable Object[] l10nParams) {
    super(BAD_REQUEST, message, l10nKey, l10nParams);
  }

  public static BadRequestException of(String message) {
    return new BadRequestException(message);
  }

  public static BadRequestException ofL10n(String l10nKey, Object... l10nParams) {
    return new BadRequestException(null, l10nKey, l10nParams);
  }

}
