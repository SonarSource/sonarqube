/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.rule;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class Severity {

  private Severity() {
    // utility
  }

  public static final String INFO = "INFO";
  public static final String MINOR = "MINOR";
  public static final String MAJOR = "MAJOR";
  public static final String CRITICAL = "CRITICAL";
  public static final String BLOCKER = "BLOCKER";

  /**
   * All the supported severities, orderest from {@link #INFO} to {@link #BLOCKER}.
   */
  public static final List<String> ALL = ImmutableList.of(INFO, MINOR, MAJOR, CRITICAL, BLOCKER);
}
