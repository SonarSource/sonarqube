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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Request is not valid and can not be processed.
 */
public class BadRequestException extends ServerException {

  private static final int BAD_REQUEST = 400;

  private List<Message> errors = newArrayList();

  public BadRequestException(String message) {
    super(BAD_REQUEST, message);
  }

  public BadRequestException(String message, List<Message> errors) {
    super(BAD_REQUEST, message);
    this.errors = errors;
  }

  public BadRequestException(@Nullable String message, @Nullable String l10nKey, @Nullable Object[] l10nParams) {
    super(BAD_REQUEST, message, l10nKey, l10nParams);
  }

  public BadRequestException(@Nullable String message, @Nullable String l10nKey, @Nullable Object[] l10nParams, List<Message> errors) {
    super(BAD_REQUEST, message, l10nKey, l10nParams);
    this.errors = errors;
  }

  public static BadRequestException of(String message) {
    return new BadRequestException(message);
  }

  public static BadRequestException of(@Nullable  String message, List<Message> errors) {
    return new BadRequestException(message, errors);
  }

  public static BadRequestException of(List<Message> errors) {
    return new BadRequestException(null, errors);
  }

  public static BadRequestException ofL10n(String l10nKey, Object... l10nParams) {
    return new BadRequestException(null, l10nKey, l10nParams);
  }

  public static BadRequestException ofL10n(List<Message> errors, String l10nKey, Object... l10nParams) {
    return new BadRequestException(null, l10nKey, l10nParams, errors);
  }

  /**
   * List of error messages. Empty if there are no errors.
   */
  public List<Message> errors() {
    return errors;
  }

  public static class Message {
    private final String l10nKey;
    private final Object[] l10nParams;
    private final String text;

    private Message(@Nullable String l10nKey, @Nullable Object[] l10nParams, @Nullable String text) {
      this.l10nKey = l10nKey;
      this.l10nParams = l10nParams;
      this.text = text;
    }

    public static Message of(String text) {
      return new Message(null, null, text);
    }

    public static Message ofL10n(String l10nKey, Object... l10nParams) {
      return new Message(l10nKey, l10nParams, null);
    }

    @CheckForNull
    public String text() {
      return text;
    }

    @CheckForNull
    public String l10nKey() {
      return l10nKey;
    }

    @CheckForNull
    public Object[] l10nParams() {
      return Arrays.copyOf(l10nParams, l10nParams.length);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Message message = (Message) o;

      if (l10nKey != null ? !l10nKey.equals(message.l10nKey) : message.l10nKey != null) {
        return false;
      }
      // Probably incorrect - comparing Object[] arrays with Arrays.equals
      if (!Arrays.equals(l10nParams, message.l10nParams)) {
        return false;
      }
      if (text != null ? !text.equals(message.text) : message.text != null) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = l10nKey != null ? l10nKey.hashCode() : 0;
      result = 31 * result + (l10nParams != null ? Arrays.hashCode(l10nParams) : 0);
      result = 31 * result + (text != null ? text.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return new ReflectionToStringBuilder(this).toString();
    }
  }

}
