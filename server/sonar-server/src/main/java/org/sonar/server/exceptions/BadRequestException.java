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
package org.sonar.server.exceptions;

import com.google.common.base.Objects;
import org.sonar.api.utils.ValidationMessages;

import java.util.List;

/**
 * Request is not valid and can not be processed.
 */
public class BadRequestException extends ServerException {

  private static final int BAD_REQUEST = 400;

  private final Errors errors;

  public BadRequestException(String l10nKey, Object... l10nParams) {
    super(BAD_REQUEST);
    this.errors = new Errors().add(Message.of(l10nKey, l10nParams));
  }

  public BadRequestException(List<Message> messages) {
    super(BAD_REQUEST);
    this.errors = new Errors().add(messages);
  }

  public BadRequestException(Errors e) {
    super(BAD_REQUEST);
    this.errors = e;
  }

  public BadRequestException(ValidationMessages validationMessages) {
    super(BAD_REQUEST);
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
    return Objects.toStringHelper(this)
      .add("errors", errors)
      .toString();
  }
}
