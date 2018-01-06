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
package org.sonar.core.util;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import java.util.Iterator;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

/**
 * A runtime exception that provides contextual information as a list of key-value
 * pairs. This information is added to the message and stack trace.
 * <p>
 * Example:
 * <pre>
 *   try {
 *
 *   } catch (Exception e) {
 *     throw ContextException.of("Unable to assign issue", e)
 *       .addContext("projectUuid", "P1")
 *       .addContext("issueUuid", "I1")
 *       .addContext("login", "felix");
 *   }
 * </pre>
 * </p>
 * <p>
 * Contexts of nested {@link ContextException}s are merged:
 * <pre>
 *   try {
 *     throw ContextException.of("Something wrong").addContext("login", "josette");
 *   } catch (Exception e) {
 *     throw ContextException.of("Unable to assign issue", e)
 *       .addContext("issueUuid", "I1")
 *       .addContext("login", "felix");
 *   }
 * </pre>
 * </p>
 * <p>
 * The generated message, usually written to a log with stack trace, looks like:
 * <pre>
 *   Unable to assign issue | issueUuid=I1 | login=[josette,felix]
 * </pre>
 * </p>
 */
public class ContextException extends RuntimeException {

  private static final Joiner COMMA_JOINER = Joiner.on(',');

  // LinkedListMultimap is used to keep order of keys and values
  private final transient ListMultimap<String, Object> context = LinkedListMultimap.create();

  private ContextException(Throwable t) {
    super(t);
  }

  private ContextException(String message, Throwable t) {
    super(message, t);
  }

  private ContextException(String message) {
    super(message);
  }

  private ContextException addContext(ContextException e) {
    this.context.putAll(e.context);
    return this;
  }

  public ContextException addContext(String key, @Nullable Object value) {
    context.put(key, value);
    return this;
  }

  public ContextException clearContext(String key) {
    context.removeAll(key);
    return this;
  }

  public ContextException setContext(String key, @Nullable Object value) {
    clearContext(key);
    return addContext(key, value);
  }

  /**
   * Returns the values associated with {@code key}, if any, else returns an
   * empty list.
   */
  public List<Object> getContext(String key) {
    return context.get(key);
  }

  public static ContextException of(Throwable t) {
    if (t instanceof ContextException) {
      return new ContextException(t.getCause()).addContext((ContextException) t);
    }
    return new ContextException(t);
  }

  public static ContextException of(String message, Throwable t) {
    if (t instanceof ContextException) {
      return new ContextException(message, t.getCause()).addContext((ContextException) t);
    }
    return new ContextException(message, t);
  }

  public static ContextException of(String message) {
    return new ContextException(message);
  }

  @Override
  @Nonnull
  public String getMessage() {
    return format(super.getMessage());
  }

  /**
   * Provides the message explaining the exception without the contextual data.
   */
  @CheckForNull
  public String getRawMessage() {
    return super.getMessage();
  }

  private String format(@Nullable String baseMessage) {
    StringBuilder sb = new StringBuilder();
    Iterator<String> keyIt = context.keySet().iterator();
    if (StringUtils.isNotBlank(baseMessage)) {
      sb.append(baseMessage);
      if (keyIt.hasNext()) {
        sb.append(" | ");
      }
    }
    while (keyIt.hasNext()) {
      String key = keyIt.next();
      sb.append(key).append("=");
      List<Object> values = getContext(key);
      if (values.size() > 1) {
        sb.append("[").append(COMMA_JOINER.join(values)).append("]");
      } else if (values.size() == 1) {
        sb.append(values.get(0));
      }
      if (keyIt.hasNext()) {
        sb.append(" | ");
      }
    }
    return sb.toString();
  }
}
