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
package org.sonar.api.utils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Runtime exception for "functional" error. It aims to be displayed to end-users, without any technical information
 * like stack traces. It requires sonar-runner 2.4. Previous versions log stack trace.
 * <p/>
 * Note that by design Maven still logs the stack trace when the option -e is set.
 * <p/>
 * Message should be clear and complete. Keep in mind that context is not added to the exception.
 * Names of processed resource and decorator are for example not automatically added when throwing {@link MessageException}
 * from {@link org.sonar.api.batch.Decorator}.
 *
 * @since 3.7.1
 */
public class MessageException extends RuntimeException {

  private final String l10nKey;
  private final Collection<Object> l10nParams;

  private MessageException(String s) {
    this(s, null, null);
  }

  private MessageException(@Nullable String message, @Nullable String l10nKey, @Nullable Object[] l10nParams) {
    super(message);
    this.l10nKey = l10nKey;
    this.l10nParams = l10nParams == null ? Collections.emptyList() : Collections.unmodifiableCollection(newArrayList(l10nParams));
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
