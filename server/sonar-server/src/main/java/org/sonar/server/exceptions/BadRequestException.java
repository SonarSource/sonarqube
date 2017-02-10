/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

/**
 * Request is not valid and can not be processed.
 */
public class BadRequestException extends ServerException {

  private final transient Errors errors;

  public BadRequestException(String message) {
    super(HTTP_BAD_REQUEST, message);
    this.errors = new Errors().add(Message.of(message));
  }

  private BadRequestException(Errors e) {
    super(HTTP_BAD_REQUEST, e.messages().get(0).getMessage());
    this.errors = e;
  }

  public static BadRequestException create(List<String> errorMessages) {
    return create(new Errors().add(errorMessages.stream().map(Message::of).collect(Collectors.toList())));
  }

  public static BadRequestException create(Errors e) {
    checkArgument(!e.messages().isEmpty(), "At least one error message is required");
    return new BadRequestException(e);
  }

  public Errors errors() {
    return errors;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("errors", errors)
      .toString();
  }

}
