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
package org.sonar.api.utils;

import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Runtime exception for "functional" error. It aims to be displayed to end-users, without any technical information
 * like stack traces. 
 * <br>
 * 
 * It's handling depends on the versions of the sonar-batch and sonar-runner. sonar-runner 2.4 will only show the 
 * message associated with this exception.
 * Starting from sonar-batch 5.3, this is handled in the batch side, and the main goal is to hide all wrappers of this
 * exception. If this exception is created without cause, then only the message associated with this exception is shown; 
 * otherwise, its causes are also shown.
 * Previous combinations of sonar-batch/sonar-runner log all stack trace.
 * <br>
 * Message should be clear and complete. Keep in mind that context might not be added to the exception.
 * Names of processed resource and decorator are for example not automatically added when throwing {@link MessageException}
 * from {@link org.sonar.api.batch.Decorator}.
 *
 * @since 3.7.1
 */
public class MessageException extends RuntimeException {

  private final String l10nKey;
  private final Collection<Object> l10nParams;

  protected MessageException(String s) {
    this(s, null, null);
  }

  private MessageException(@Nullable String message, @Nullable String l10nKey, @Nullable Object[] l10nParams) {
    super(message);
    this.l10nKey = l10nKey;
    this.l10nParams = l10nParams == null ? emptyList() : asList(l10nParams);
  }

  private MessageException(String message, Throwable cause) {
    super(message, cause);
    l10nKey = null;
    l10nParams = emptyList();
  }

  public static MessageException of(String message, Throwable cause) {
    return new MessageException(message, cause);
  }

  public static MessageException of(String message) {
    return new MessageException(message);
  }

  public static MessageException ofL10n(String l10nKey, Object... l10nParams) {
    return new MessageException(null, l10nKey, l10nParams);
  }

  /**
   * Does not fill in the stack trace
   *
   * @see java.lang.Throwable#fillInStackTrace()
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

  @Override
  public String toString() {
    return getMessage();
  }

  @CheckForNull
  public String l10nKey() {
    return l10nKey;
  }

  @CheckForNull
  public Collection<Object> l10nParams() {
    return l10nParams;
  }

}
