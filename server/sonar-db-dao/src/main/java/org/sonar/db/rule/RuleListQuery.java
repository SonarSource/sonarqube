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
package org.sonar.db.rule;

import javax.annotation.Nullable;

public class RuleListQuery {
  private final Long createdAt;
  private final String language;
  private final String profileUuid;
  private final String sortField;
  private final String sortDirection;

  private RuleListQuery(Long createdAt, String language, String profileUuid, String sortField, String sortDirection) {
    this.createdAt = createdAt;
    this.language = language;
    this.profileUuid = profileUuid;
    this.sortField = sortField;
    this.sortDirection = sortDirection;
  }

  public Long getCreatedAt() {
    return createdAt;
  }

  public String getLanguage() {
    return language;
  }

  public String getProfileUuid() {
    return profileUuid;
  }

  public String getSortField() {
    return sortField;
  }

  public String getSortDirection() {
    return sortDirection;
  }

  public static final class RuleListQueryBuilder {
    private Long createdAt;
    private String language;
    private String profileUuid;
    private String sortField;
    private String sortDirection = "asc";

    private RuleListQueryBuilder() {
    }

    public static RuleListQueryBuilder newRuleListQueryBuilder() {
      return new RuleListQueryBuilder();
    }

    public RuleListQueryBuilder createdAt(@Nullable Long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public RuleListQueryBuilder language(String language) {
      this.language = language;
      return this;
    }

    public RuleListQueryBuilder profileUuid(@Nullable String profileUuid) {
      this.profileUuid = profileUuid;
      return this;
    }

    public RuleListQueryBuilder sortField(String sortField) {
      this.sortField = sortField;
      return this;
    }

    public RuleListQueryBuilder sortDirection(String sortDirection) {
      this.sortDirection = sortDirection;
      return this;
    }

    public RuleListQuery build() {
      return new RuleListQuery(createdAt, language, profileUuid, sortField, sortDirection);
    }
  }
}
