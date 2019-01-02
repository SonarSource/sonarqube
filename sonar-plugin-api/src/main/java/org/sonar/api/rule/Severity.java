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
package org.sonar.api.rule;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * @since 3.6
 */
public final class Severity {

  public static final String INFO = "INFO";
  public static final String MINOR = "MINOR";
  public static final String MAJOR = "MAJOR";
  public static final String CRITICAL = "CRITICAL";
  public static final String BLOCKER = "BLOCKER";

  /**
   * All the supported severity values, ordered from {@link #INFO} to {@link #BLOCKER}.
   */
  public static final List<String> ALL = unmodifiableList(asList(INFO, MINOR, MAJOR, CRITICAL, BLOCKER));

  private Severity() {
    // utility
  }

  public static String defaultSeverity() {
    return MAJOR;
  }
}
