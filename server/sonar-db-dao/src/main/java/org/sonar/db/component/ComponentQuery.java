/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.component;

import java.util.Locale;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.WildcardPosition;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.DaoDatabaseUtils.buildLikeValue;

public class ComponentQuery {
  private final String nameOrKeyQuery;
  private final String[] qualifiers;
  private final String language;
  private final Boolean isPrivate;
  private final Set<Long> componentIds;

  /**
   * Used by Dev Cockpit 1.9.
   * Could be removed when Developer Cockpit doesn't use it anymore.
   *
   * @deprecated since 5.4, use {@link Builder} instead
   */
  @Deprecated
  public ComponentQuery(@Nullable String nameOrKeyQuery, String... qualifiers) {
    this.nameOrKeyQuery = nameOrKeyQuery;
    this.qualifiers = Builder.validateQualifiers(qualifiers);
    this.language = null;
    this.componentIds = null;
    this.isPrivate = null;
  }

  private ComponentQuery(Builder builder) {
    this.nameOrKeyQuery = builder.nameOrKeyQuery;
    this.qualifiers = builder.qualifiers;
    this.language = builder.language;
    this.componentIds = builder.componentIds;
    this.isPrivate = builder.isPrivate;
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  @CheckForNull
  public String getNameOrKeyQuery() {
    return nameOrKeyQuery;
  }

  /**
   * Used by MyBatis mapper
   */
  @CheckForNull
  public String getNameOrKeyUpperLikeQuery() {
    return buildLikeValue(nameOrKeyQuery, WildcardPosition.BEFORE_AND_AFTER).toUpperCase(Locale.ENGLISH);
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  @CheckForNull
  public Set<Long> getComponentIds() {
    return componentIds;
  }

  @CheckForNull
  public Boolean getPrivate() {
    return isPrivate;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String nameOrKeyQuery;
    private String[] qualifiers;
    private String language;
    private Boolean isPrivate;
    private Set<Long> componentIds;

    public Builder setNameOrKeyQuery(@Nullable String nameOrKeyQuery) {
      this.nameOrKeyQuery = nameOrKeyQuery;
      return this;
    }

    public Builder setQualifiers(String... qualifiers) {
      this.qualifiers = qualifiers;
      return this;
    }

    public Builder setLanguage(@Nullable String language) {
      this.language = language;
      return this;
    }

    public Builder setComponentIds(@Nullable Set<Long> componentIds) {
      this.componentIds = componentIds;
      return this;
    }

    public Builder setPrivate(@Nullable Boolean isPrivate) {
      this.isPrivate = isPrivate;
      return this;
    }

    protected static String[] validateQualifiers(@Nullable String... qualifiers) {
      checkArgument(qualifiers != null && qualifiers.length > 0, "At least one qualifier must be provided");
      return qualifiers;
    }

    public ComponentQuery build() {
      validateQualifiers(this.qualifiers);
      return new ComponentQuery(this);
    }
  }
}
