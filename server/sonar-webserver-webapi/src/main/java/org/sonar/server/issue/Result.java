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
package org.sonar.server.issue;

import java.util.Arrays;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class Result<T> {

  private T object = null;
  private final List<Message> errors = newArrayList();

  private Result(@Nullable T object) {
    this.object = object;
  }

  public static <T> Result<T> of() {
    return new Result<>(null);
  }

  public Result<T> set(@Nullable T object) {
    this.object = object;
    return this;
  }

  public T get() {
    return object;
  }

  public Result<T> addError(String text) {
    return addError(Message.of(text));
  }

  public Result<T> addError(Message message) {
    errors.add(message);
    return this;
  }

  /**
   * List of error messages. Empty if there are no errors.
   */
  public List<Message> errors() {
    return errors;
  }

  /**
   * True if there are no errors.
   */
  public boolean ok() {
    return errors.isEmpty();
  }

  public int httpStatus() {
    if (ok()) {
      return 200;
    }
    // TODO support 401, 403 and 500
    return 400;
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
      return l10nParams;
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

      if ((l10nKey != null) ? !l10nKey.equals(message.l10nKey) : (message.l10nKey != null)) {
        return false;
      }
      // Probably incorrect - comparing Object[] arrays with Arrays.equals
      return Arrays.equals(l10nParams, message.l10nParams) && ((text != null) ? text.equals(message.text) : (message.text == null));
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
