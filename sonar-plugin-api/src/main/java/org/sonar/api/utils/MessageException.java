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
 * @since 4.0
 */
public class MessageException extends RuntimeException {

  private MessageException(String message) {
    super(message);
  }

  public static MessageException of(String message) {
    return new MessageException(message);
  }

}
