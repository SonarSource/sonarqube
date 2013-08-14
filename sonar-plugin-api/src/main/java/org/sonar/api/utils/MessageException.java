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
 * Runtime exception for "functional" errors. It aims to be displayed to end-users, without any technical information
 * like stack traces.
 *
 * @since 4.0
 */
public class MessageException extends RuntimeException {

  private String l10nKey;
  private Collection<Object> l10nParams;

  public MessageException(String s) {
    super(s);
  }

  public MessageException(@Nullable String message, @Nullable String l10nKey, @Nullable Object[] l10nParams) {
    super(message);
    this.l10nKey = l10nKey;
    this.l10nParams = l10nParams == null ? Collections.emptyList() : Collections.unmodifiableCollection(newArrayList(l10nParams));
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
