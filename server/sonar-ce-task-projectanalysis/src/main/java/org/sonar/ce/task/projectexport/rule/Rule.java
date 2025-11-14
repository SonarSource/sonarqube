/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectexport.rule;

import java.util.Objects;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public record Rule(String ref, String repository, String key) {
  public Rule(String ref, String repository, String key) {
    this.ref = ref;
    this.repository = requireNonNull(repository, "repository can not be null");
    this.key = requireNonNull(key, "key can not be null");
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Rule)) {
      return false;
    }
    Rule rule = (Rule) o;
    return repository.equals(rule.repository) && key.equals(rule.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repository, key);
  }

  @Override
  public String toString() {
    return "Rule{" +
      "ref='" + ref + "', repository='" + repository + "', key='" + key + "'}";
  }
}
