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
package org.sonar.server.component.index;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public class ComponentIndexResults {

  private final List<ComponentHitsPerQualifier> qualifiers;

  private ComponentIndexResults(Builder builder) {
    this.qualifiers = requireNonNull(builder.qualifiers);
  }

  public Stream<ComponentHitsPerQualifier> getQualifiers() {
    return qualifiers.stream();
  }

  public boolean isEmpty() {
    return qualifiers.isEmpty();
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private List<ComponentHitsPerQualifier> qualifiers = emptyList();

    private Builder() {
    }

    public Builder setQualifiers(Stream<ComponentHitsPerQualifier> qualifiers) {
      this.qualifiers = qualifiers.collect(Collectors.toList());
      return this;
    }

    public ComponentIndexResults build() {
      return new ComponentIndexResults(this);
    }
  }
}
