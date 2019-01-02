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

import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

class PathAwareCallRecord {
  private final String method;
  @CheckForNull
  private final Integer ref;
  @CheckForNull
  private final String key;
  private final int current;
  @CheckForNull
  private final Integer parent;
  private final int root;
  private final List<Integer> path;

  private PathAwareCallRecord(String method, @Nullable Integer ref, @Nullable String key, int current, @Nullable Integer parent, int root, List<Integer> path) {
    this.method = method;
    this.ref = ref;
    this.key = key;
    this.current = current;
    this.parent = parent;
    this.root = root;
    this.path = path;
  }

  public static PathAwareCallRecord reportCallRecord(String method, Integer ref, int current, @Nullable Integer parent, int root, List<Integer> path) {
    return new PathAwareCallRecord(method, ref, method, current, parent, root, path);
  }

  public static PathAwareCallRecord viewsCallRecord(String method, String key, int current, @Nullable Integer parent, int root, List<Integer> path) {
    return new PathAwareCallRecord(method, null, key, current, parent, root, path);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PathAwareCallRecord that = (PathAwareCallRecord) o;
    return Objects.equals(ref, that.ref) &&
      Objects.equals(key, that.key) &&
      Objects.equals(current, that.current) &&
      Objects.equals(root, that.root) &&
      Objects.equals(method, that.method) &&
      Objects.equals(parent, that.parent) &&
      Objects.equals(path, that.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, ref, key, current, parent, root, path);
  }

  @Override
  public String toString() {
    return "{" +
      "method='" + method + '\'' +
      ", ref=" + ref +
      ", key=" + key +
      ", current=" + current +
      ", parent=" + parent +
      ", root=" + root +
      ", path=" + path +
      '}';
  }
}
