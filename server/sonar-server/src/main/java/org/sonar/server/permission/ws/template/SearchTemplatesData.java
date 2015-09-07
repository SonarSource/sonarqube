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

package org.sonar.server.permission.ws.template;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import java.util.List;
import java.util.Set;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.server.permission.ws.template.DefaultPermissionTemplateFinder.TemplateUuidQualifier;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableTable.copyOf;
import static com.google.common.collect.Ordering.natural;

class SearchTemplatesData {
  private final List<PermissionTemplateDto> templates;
  private final List<TemplateUuidQualifier> defaultTemplates;
  private final Table<Long, String, Integer> userCountByTemplateIdAndPermission;
  private final Table<Long, String, Integer> groupCountByTemplateIdAndPermission;

  private SearchTemplatesData(Builder builder) {
    this.templates = copyOf(builder.templates);
    this.defaultTemplates = copyOf(builder.defaultTemplates);
    this.userCountByTemplateIdAndPermission = copyOf(builder.userCountByTemplateIdAndPermission);
    this.groupCountByTemplateIdAndPermission = copyOf(builder.groupCountByTemplateIdAndPermission);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public List<PermissionTemplateDto> templates() {
    return templates;
  }

  public List<TemplateUuidQualifier> defaultTempltes() {
    return defaultTemplates;
  }

  public int userCount(long templateId, String permission) {
    return firstNonNull(userCountByTemplateIdAndPermission.get(templateId, permission), 0);
  }

  public int groupCount(long templateId, String permission) {
    return firstNonNull(groupCountByTemplateIdAndPermission.get(templateId, permission), 0);
  }

  public Set<String> permissions(long templateId) {
    return FluentIterable.from(
      Iterables.concat(
        userCountByTemplateIdAndPermission.row(templateId).keySet(),
        groupCountByTemplateIdAndPermission.row(templateId).keySet()
        )
      ).toSortedSet(natural());
  }

  public static class Builder {
    private List<PermissionTemplateDto> templates;
    private List<TemplateUuidQualifier> defaultTemplates;
    private Table<Long, String, Integer> userCountByTemplateIdAndPermission;
    private Table<Long, String, Integer> groupCountByTemplateIdAndPermission;

    private Builder() {
      // prevents instantiation outside main class
    }

    public SearchTemplatesData build() {
      checkState(templates != null);
      checkState(defaultTemplates != null);
      checkState(userCountByTemplateIdAndPermission != null);
      checkState(groupCountByTemplateIdAndPermission != null);

      return new SearchTemplatesData(this);
    }

    public Builder templates(List<PermissionTemplateDto> templates) {
      this.templates = templates;
      return this;
    }

    public Builder defaultTemplates(List<TemplateUuidQualifier> defaultTemplates) {
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
  }
}
