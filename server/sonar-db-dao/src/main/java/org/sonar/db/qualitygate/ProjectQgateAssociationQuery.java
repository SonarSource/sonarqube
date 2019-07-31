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
package org.sonar.db.qualitygate;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.DaoUtils;
import org.sonar.db.WildcardPosition;

public class ProjectQgateAssociationQuery {

  public static final int DEFAULT_PAGE_INDEX = 1;
  public static final int DEFAULT_PAGE_SIZE = 100;

  public static final String ANY = "all";
  public static final String IN = "selected";
  public static final String OUT = "deselected";
  public static final Set<String> AVAILABLE_MEMBERSHIP = ImmutableSet.of(ANY, IN, OUT);

  private final String gateUuid;
  private final String organizationUuid;
  private final String membership;

  private final String projectSearch;

  // for internal use in MyBatis
  private final String projectSearchUpperLikeSql;

  // max results per page
  private final int pageSize;

  // index of selected page. Start with 1.
  private final int pageIndex;

  private ProjectQgateAssociationQuery(Builder builder) {
    this.gateUuid = builder.qualityGate.getUuid();
    this.organizationUuid = builder.qualityGate.getOrganizationUuid();
    this.membership = builder.membership;
    this.projectSearch = builder.projectSearch;
    if (this.projectSearch == null) {
      this.projectSearchUpperLikeSql = null;
    } else {
      this.projectSearchUpperLikeSql = DaoUtils.buildLikeValue(projectSearch.toUpperCase(Locale.ENGLISH), WildcardPosition.BEFORE_AND_AFTER);
    }

    this.pageSize = builder.pageSize;
    this.pageIndex = builder.pageIndex;
  }

  public String gateUuid() {
    return gateUuid;
  }

  public String organizationUuid() {
    return organizationUuid;
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
    private QGateWithOrgDto qualityGate;
    private String membership;
    private String projectSearch;

    private Integer pageIndex = DEFAULT_PAGE_INDEX;
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    private Builder() {
    }

    public Builder qualityGate(QGateWithOrgDto qualityGate) {
      this.qualityGate = qualityGate;
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
      initMembership();
      initPageIndex();
      initPageSize();
      return new ProjectQgateAssociationQuery(this);
    }
  }
}
