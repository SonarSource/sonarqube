/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.base.MoreObjects;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.util.Arrays.asList;

/**
 * Request is not valid and can not be processed.
 */
public class BadRequestException extends ServerException {

  private final transient List<String> errors;

  private BadRequestException(List<String> errors) {
    super(HTTP_BAD_REQUEST, errors.get(0));
    this.errors = errors;
  }

  public static BadRequestException create(List<String> errorMessages) {
    checkArgument(!errorMessages.isEmpty(), "At least one error message is required");
    checkArgument(errorMessages.stream().noneMatch(message -> message == null || message.isEmpty()), "Message cannot be empty");
    return new BadRequestException(errorMessages);
  }

  public static BadRequestException create(String... errorMessages) {
    return create(asList(errorMessages));
  }

  public List<String> errors() {
    return errors;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("errors", errors)
      .toString();
  }

}
