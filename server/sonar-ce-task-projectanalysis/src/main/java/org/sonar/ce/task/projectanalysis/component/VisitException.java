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
package org.sonar.ce.task.projectanalysis.component;

import static java.lang.String.format;

/**
 * Wrapper {@link RuntimeException} of any {@link RuntimeException} thrown during the visit of the Component tree.
 *
 * Use {@link #rethrowOrWrap(RuntimeException, String, Object...)} to avoid having {@link VisitException} as cause of
 * another {@link VisitException}
 */
public class VisitException extends RuntimeException {
  public VisitException(String message, RuntimeException cause) {
    super(message, cause);
  }

  /**
   * @param e the {@link RuntimeException} to wrap unless it is an instance of {@link VisitException}
   * @param pattern a {@link String#format(String, Object...)} pattern
   * @param args optional {@link String#format(String, Object...)} arguments
   */
  public static void rethrowOrWrap(RuntimeException e, String pattern, Object... args) {
    if (e instanceof VisitException) {
      throw e;
    } else {
      throw new VisitException(format(pattern, args), e);
    }
  }
}
