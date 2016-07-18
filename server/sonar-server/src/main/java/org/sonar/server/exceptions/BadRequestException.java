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
package org.sonar.server.exceptions;

import com.google.common.base.MoreObjects;
import java.util.List;
import org.sonar.api.utils.ValidationMessages;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;

/**
 * Request is not valid and can not be processed.
 */
public class BadRequestException extends ServerException {

  private final transient Errors errors;

  public BadRequestException(String l10nKey, Object... l10nParams) {
    super(HTTP_BAD_REQUEST);
    this.errors = new Errors().add(Message.of(l10nKey, l10nParams));
  }

  public BadRequestException(List<Message> messages) {
    super(HTTP_BAD_REQUEST);
    this.errors = new Errors().add(messages);
  }

  public BadRequestException(Errors e) {
    super(HTTP_BAD_REQUEST);
    this.errors = e;
  }

  public BadRequestException(ValidationMessages validationMessages) {
    super(HTTP_BAD_REQUEST);
    this.errors = new Errors();
    for (String s : validationMessages.getErrors()) {
      errors.add(Message.of(s));
    }
  }

  public Errors errors() {
    return errors;
  }

  public Message firstError() {
    return errors.messages().get(0);
  }

  @Override
  public String getMessage() {
    return firstError().getKey();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("errors", errors)
      .toString();
  }
}
