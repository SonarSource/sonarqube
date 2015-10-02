/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.qualitygate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

public class ProjectQgateAssociationQuery {

  public static final int DEFAULT_PAGE_INDEX = 1;
  public static final int DEFAULT_PAGE_SIZE = 100;

  public static final String ANY = "all";
  public static final String IN = "selected";
  public static final String OUT = "deselected";
  public static final Set<String> AVAILABLE_MEMBERSHIP = ImmutableSet.of(ANY, IN, OUT);

  private final String gateId;
  private final String membership;

  private final String projectSearch;

  // for internal use in MyBatis
  private final String projectSearchSql;

  // max results per page
  private final int pageSize;

  // index of selected page. Start with 1.
  private final int pageIndex;

  private ProjectQgateAssociationQuery(Builder builder) {
    this.gateId = builder.gateId;
    this.membership = builder.membership;
    this.projectSearch = builder.projectSearch;
    this.projectSearchSql = projectSearchToSql(projectSearch);

    this.pageSize = builder.pageSize;
    this.pageIndex = builder.pageIndex;
  }

  private String projectSearchToSql(@Nullable String value) {
    if (value == null) {
      return null;
    }

    return value
      .replaceAll("%", "\\\\%")
      .replaceAll("_", "\\\\_")
      .toLowerCase() + "%";
  }

  public String gateId() {
    return gateId;
  }

  @CheckForNull
  public String membership() {
    return membership;
  }

  /**
   * Search for projects containing a given string
   */
  @CheckForNull
  public String projectSearch() {
    return projectSearch;
  }

  public String projectSearchSql() {
    return projectSearchSql;
  }

  public int pageSize() {
    return pageSize;
  }

  public int pageIndex() {
    return pageIndex;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String gateId;
    private String membership;
    private String projectSearch;

    private Integer pageIndex = DEFAULT_PAGE_INDEX;
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    private Builder() {
    }

    public Builder gateId(String gateId) {
      this.gateId = gateId;
      return this;
    }

    public Builder membership(@Nullable String membership) {
      this.membership = membership;
      return this;
    }

    public Builder projectSearch(@Nullable String s) {
      this.projectSearch = StringUtils.defaultIfBlank(s, null);
      return this;
    }

    public Builder pageSize(@Nullable Integer i) {
      this.pageSize = i;
      return this;
    }

    public Builder pageIndex(@Nullable Integer i) {
      this.pageIndex = i;
      return this;
    }

    private void initMembership() {
      if (membership == null) {
        membership = ProjectQgateAssociationQuery.ANY;
      } else {
        Preconditions.checkArgument(AVAILABLE_MEMBERSHIP.contains(membership),
          "Membership is not valid (got " + membership + "). Available values are " + AVAILABLE_MEMBERSHIP);
      }
    }

    private void initPageSize() {
      if (pageSize == null) {
        pageSize = DEFAULT_PAGE_SIZE;
      }
    }

    private void initPageIndex() {
      if (pageIndex == null) {
        pageIndex = DEFAULT_PAGE_INDEX;
      }
      Preconditions.checkArgument(pageIndex > 0, "Page index must be greater than 0 (got " + pageIndex + ")");
    }

    public ProjectQgateAssociationQuery build() {
      Preconditions.checkNotNull(gateId, "Gate ID cannot be null.");
      initMembership();
      initPageIndex();
      initPageSize();
      return new ProjectQgateAssociationQuery(this);
    }
  }
}
