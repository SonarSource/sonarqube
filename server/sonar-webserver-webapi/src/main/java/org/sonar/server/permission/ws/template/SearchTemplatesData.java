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
package org.sonar.server.permission.ws.template;

import com.google.common.collect.Table;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.utils.Paging;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.common.permission.DefaultTemplatesResolver.ResolvedDefaultTemplates;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableTable.copyOf;

class SearchTemplatesData {
  private final List<PermissionTemplateDto> templates;
  private final ResolvedDefaultTemplates defaultTemplates;
  private final Table<String, String, Integer> userCountByTemplateUuidAndPermission;
  private final Table<String, String, Integer> groupCountByTemplateUuidAndPermission;
  private final Table<String, String, Boolean> withProjectCreatorByTemplateUuidAndPermission;
  private final Paging paging;
  // Special fields for pageSize=0 case (can't be represented by standard Paging)
  private final Integer pagingPageIndex;
  private final Integer pagingPageSize;
  private final Integer pagingTotal;

  private SearchTemplatesData(Builder builder) {
    this.templates = copyOf(builder.templates);
    this.defaultTemplates = builder.defaultTemplates;
    this.userCountByTemplateUuidAndPermission = copyOf(builder.userCountByTemplateUuidAndPermission);
    this.groupCountByTemplateUuidAndPermission = copyOf(builder.groupCountByTemplateUuidAndPermission);
    this.withProjectCreatorByTemplateUuidAndPermission = copyOf(builder.withProjectCreatorByTemplateUuidAndPermission);
    this.paging = builder.paging;
    this.pagingPageIndex = builder.pagingPageIndex;
    this.pagingPageSize = builder.pagingPageSize;
    this.pagingTotal = builder.pagingTotal;
  }

  public static Builder builder() {
    return new Builder();
  }

  public List<PermissionTemplateDto> templates() {
    return templates;
  }

  public ResolvedDefaultTemplates defaultTemplates() {
    return defaultTemplates;
  }

  public int userCount(String templateUuid, String permission) {
    return firstNonNull(userCountByTemplateUuidAndPermission.get(templateUuid, permission), 0);
  }

  public int groupCount(String templateUuid, String permission) {
    return firstNonNull(groupCountByTemplateUuidAndPermission.get(templateUuid, permission), 0);
  }

  public boolean withProjectCreator(String templateUuid, String permission) {
    return firstNonNull(withProjectCreatorByTemplateUuidAndPermission.get(templateUuid, permission), false);
  }

  @Nullable
  public Paging paging() {
    return paging;
  }

  @Nullable
  public Integer pagingPageIndex() {
    return pagingPageIndex;
  }

  @Nullable
  public Integer pagingPageSize() {
    return pagingPageSize;
  }

  @Nullable
  public Integer pagingTotal() {
    return pagingTotal;
  }

  public static class Builder {
    private List<PermissionTemplateDto> templates;
    private ResolvedDefaultTemplates defaultTemplates;
    private Table<String, String, Integer> userCountByTemplateUuidAndPermission;
    private Table<String, String, Integer> groupCountByTemplateUuidAndPermission;
    private Table<String, String, Boolean> withProjectCreatorByTemplateUuidAndPermission;
    private Paging paging;
    private Integer pagingPageIndex;
    private Integer pagingPageSize;
    private Integer pagingTotal;

    private Builder() {
      // prevents instantiation outside main class
    }

    public SearchTemplatesData build() {
      checkState(templates != null);
      checkState(defaultTemplates != null);
      checkState(userCountByTemplateUuidAndPermission != null);
      checkState(groupCountByTemplateUuidAndPermission != null);
      checkState(withProjectCreatorByTemplateUuidAndPermission != null);

      return new SearchTemplatesData(this);
    }

    public Builder templates(List<PermissionTemplateDto> templates) {
      this.templates = templates;
      return this;
    }

    public Builder defaultTemplates(ResolvedDefaultTemplates defaultTemplates) {
      this.defaultTemplates = defaultTemplates;
      return this;
    }

    public Builder userCountByTemplateUuidAndPermission(Table<String, String, Integer> userCountByTemplateUuidAndPermission) {
      this.userCountByTemplateUuidAndPermission = userCountByTemplateUuidAndPermission;
      return this;
    }

    public Builder groupCountByTemplateUuidAndPermission(Table<String, String, Integer> groupCountByTemplateUuidAndPermission) {
      this.groupCountByTemplateUuidAndPermission = groupCountByTemplateUuidAndPermission;
      return this;
    }

    public Builder withProjectCreatorByTemplateUuidAndPermission(Table<String, String, Boolean> withProjectCreatorByTemplateUuidAndPermission) {
      this.withProjectCreatorByTemplateUuidAndPermission = withProjectCreatorByTemplateUuidAndPermission;
      return this;
    }

    public Builder paging(Paging paging) {
      this.paging = paging;
      return this;
    }

    public Builder pagingValues(int pageIndex, int pageSize, int total) {
      pagingPageIndex = pageIndex;
      pagingPageSize = pageSize;
      pagingTotal = total;
      return this;
    }
  }
}
