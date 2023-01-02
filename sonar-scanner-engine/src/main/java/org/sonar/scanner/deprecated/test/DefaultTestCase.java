/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.deprecated.test;

import java.util.Locale;
import javax.annotation.Nullable;

public class DefaultTestCase {
  public enum Status {
    OK, FAILURE, ERROR, SKIPPED;

    public static Status of(@Nullable String s) {
      return s == null ? null : valueOf(s.toUpperCase(Locale.ENGLISH));
    }
  }

  private String type;
  private Long durationInMs;
  private Status status;
  private String name;

  public String type() {
    return type;
  }

  public DefaultTestCase setType(@Nullable String s) {
    this.type = s;
    return this;
  }

  public Long durationInMs() {
    return durationInMs;
  }

  public DefaultTestCase setDurationInMs(@Nullable Long l) {
    if (l != null && l < 0) {
      throw new IllegalStateException("Test duration must be positive (got: " + l + ")");
    }
    this.durationInMs = l;
    return this;
  }

  public Status status() {
    return status;
  }

  public DefaultTestCase setStatus(@Nullable Status s) {
    this.status = s;
    return this;
  }

  public String name() {
    return name;
  }

  public DefaultTestCase setName(String s) {
    this.name = s;
    return this;
  }
}
