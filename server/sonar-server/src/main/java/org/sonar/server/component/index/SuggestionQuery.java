/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class SuggestionQuery {

  public static final int DEFAULT_LIMIT = 6;

  private final String query;
  private final Collection<String> qualifiers;
  private final Set<String> recentlyBrowsedKeys;
  private final Set<String> favoriteKeys;
  private final int skip;
  private final int limit;

  private SuggestionQuery(Builder builder) {
    this.query = requireNonNull(builder.query);
    this.qualifiers = requireNonNull(builder.qualifiers);
    this.recentlyBrowsedKeys = requireNonNull(builder.recentlyBrowsedKeys);
    this.favoriteKeys = requireNonNull(builder.favoriteKeys);
    this.skip = builder.skip;
    this.limit = builder.limit;
  }

  public Collection<String> getQualifiers() {
    return qualifiers;
  }

  public String getQuery() {
    return query;
  }

  public Set<String> getRecentlyBrowsedKeys() {
    return recentlyBrowsedKeys;
  }

  public int getSkip() {
    return skip;
  }

  public int getLimit() {
    return limit;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Set<String> getFavoriteKeys() {
    return favoriteKeys;
  }

  public static class Builder {
    private String query;
    private Collection<String> qualifiers = Collections.emptyList();
    private Set<String> recentlyBrowsedKeys = Collections.emptySet();
    private Set<String> favoriteKeys = Collections.emptySet();
    private int skip = 0;
    private int limit = DEFAULT_LIMIT;

    private Builder() {
    }

    public Builder setQuery(String query) {
      checkArgument(query.length() >= 2, "Query must be at least two characters long: %s", query);
      this.query = query;
      return this;
    }

    public Builder setQualifiers(Collection<String> qualifiers) {
      this.qualifiers = Collections.unmodifiableCollection(qualifiers);
      return this;
    }

    public Builder setRecentlyBrowsedKeys(Set<String> recentlyBrowsedKeys) {
      this.recentlyBrowsedKeys = Collections.unmodifiableSet(recentlyBrowsedKeys);
      return this;
    }

    public Builder setFavoriteKeys(Set<String> favoriteKeys) {
      this.favoriteKeys = Collections.unmodifiableSet(favoriteKeys);
      return this;
    }

    public Builder setSkip(int skip) {
      checkArgument(limit > 0, "Skip has to be strictly positive: %s", limit);
      this.skip = skip;
      return this;
    }

    public Builder setLimit(int limit) {
      checkArgument(limit > 0, "Limit has to be strictly positive: %s", limit);
      this.limit = limit;
      return this;
    }

    public SuggestionQuery build() {
      return new SuggestionQuery(this);
    }
  }
}
