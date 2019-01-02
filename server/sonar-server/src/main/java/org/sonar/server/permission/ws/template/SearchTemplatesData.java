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
package org.sonar.server.permission.ws.template;

import com.google.common.collect.Table;
import java.util.List;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ws.template.DefaultTemplatesResolver.ResolvedDefaultTemplates;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableTable.copyOf;

class SearchTemplatesData {
  private final List<PermissionTemplateDto> templates;
  private final ResolvedDefaultTemplates defaultTemplates;
  private final Table<Long, String, Integer> userCountByTemplateIdAndPermission;
  private final Table<Long, String, Integer> groupCountByTemplateIdAndPermission;
  private final Table<Long, String, Boolean> withProjectCreatorByTemplateIdAndPermission;

  private SearchTemplatesData(Builder builder) {
    this.templates = copyOf(builder.templates);
    this.defaultTemplates = builder.defaultTemplates;
    this.userCountByTemplateIdAndPermission = copyOf(builder.userCountByTemplateIdAndPermission);
    this.groupCountByTemplateIdAndPermission = copyOf(builder.groupCountByTemplateIdAndPermission);
    this.withProjectCreatorByTemplateIdAndPermission = copyOf(builder.withProjectCreatorByTemplateIdAndPermission);
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

  public int userCount(long templateId, String permission) {
    return firstNonNull(userCountByTemplateIdAndPermission.get(templateId, permission), 0);
  }

  public int groupCount(long templateId, String permission) {
    return firstNonNull(groupCountByTemplateIdAndPermission.get(templateId, permission), 0);
  }

  public boolean withProjectCreator(long templateId, String permission) {
    return firstNonNull(withProjectCreatorByTemplateIdAndPermission.get(templateId, permission), false);
  }

  public static class Builder {
    private List<PermissionTemplateDto> templates;
    private ResolvedDefaultTemplates defaultTemplates;
    private Table<Long, String, Integer> userCountByTemplateIdAndPermission;
    private Table<Long, String, Integer> groupCountByTemplateIdAndPermission;
    private Table<Long, String, Boolean> withProjectCreatorByTemplateIdAndPermission;

    private Builder() {
      // prevents instantiation outside main class
    }

    public SearchTemplatesData build() {
      checkState(templates != null);
      checkState(defaultTemplates != null);
      checkState(userCountByTemplateIdAndPermission != null);
      checkState(groupCountByTemplateIdAndPermission != null);
      checkState(withProjectCreatorByTemplateIdAndPermission != null);

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

    public Builder userCountByTemplateIdAndPermission(Table<Long, String, Integer> userCountByTemplateIdAndPermission) {
      this.userCountByTemplateIdAndPermission = userCountByTemplateIdAndPermission;
      return this;
    }

    public Builder groupCountByTemplateIdAndPermission(Table<Long, String, Integer> groupCountByTemplateIdAndPermission) {
      this.groupCountByTemplateIdAndPermission = groupCountByTemplateIdAndPermission;
      return this;
    }

    public Builder withProjectCreatorByTemplateIdAndPermission(Table<Long, String, Boolean> withProjectCreatorByTemplateIdAndPermission) {
      this.withProjectCreatorByTemplateIdAndPermission = withProjectCreatorByTemplateIdAndPermission;
      return this;
    }
  }
}
