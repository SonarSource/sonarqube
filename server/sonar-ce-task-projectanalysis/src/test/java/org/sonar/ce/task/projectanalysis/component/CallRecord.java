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

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

class CallRecord {
  private final String method;
  @CheckForNull
  private final Integer ref;
  @CheckForNull
  private final String key;

  private CallRecord(String method, @Nullable Integer ref, @Nullable String key) {
    this.method = method;
    this.ref = ref;
    this.key = key;
  }

  public static CallRecord reportCallRecord(String method, Integer ref) {
    return new CallRecord(method, ref, method);
  }

  public static CallRecord viewsCallRecord(String method, String key) {
    return new CallRecord(method, null, key);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CallRecord that = (CallRecord) o;
    return Objects.equals(ref, that.ref) &&
      Objects.equals(key, that.key) &&
      Objects.equals(method, that.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, ref, key);
  }

  @Override
  public String toString() {
    return "{" +
      "method='" + method + '\'' +
      ", ref=" + ref +
      ", key=" + key +
      '}';
  }
}
