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

/**
 * Because we don't like checked exceptions !
 *
 * @since 1.10
 * @deprecated in 4.4. Use standard exceptions like {@link java.lang.IllegalArgumentException}
 * or {@link java.lang.IllegalStateException}. Use {@link org.sonar.api.utils.MessageException}
 * for raising errors to end-users without displaying stackstrace.
 */
@Deprecated
public class SonarException extends RuntimeException {
  public SonarException() {
  }

  public SonarException(String s) {
    super(s);
  }

  public SonarException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public SonarException(Throwable throwable) {
    super(throwable);
  }
}
