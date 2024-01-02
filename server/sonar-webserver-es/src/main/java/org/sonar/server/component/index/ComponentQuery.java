/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;

public class ComponentQuery {
  private final String query;
  private final Collection<String> qualifiers;

  private ComponentQuery(Builder builder) {
    this.query = builder.query;
    this.qualifiers = builder.qualifiers;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public Collection<String> getQualifiers() {
    return qualifiers;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String query;
    private Collection<String> qualifiers = emptySet();

    private Builder() {
      // enforce static factory method
    }

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setQualifiers(Collection<String> qualifiers) {
      this.qualifiers = unmodifiableCollection(qualifiers);
      return this;
    }

    public ComponentQuery build() {
      return new ComponentQuery(this);
    }
  }
}
