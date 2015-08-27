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

package org.sonar.server.issue.filter;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.issue.IssueFilterDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Objects.firstNonNull;

class IssueFilterJsonWriter {

  private static final String DEFAULT_LOGIN = "[SonarQube]";

  private IssueFilterJsonWriter() {
    // static methods only
  }

  static void writeWithName(JsonWriter json, IssueFilterDto filter, UserSession userSession) {
    json.name("filter");
    write(json, new IssueFilterWithFavorite(filter, null), userSession);
  }

  static void write(JsonWriter json, IssueFilterWithFavorite issueFilterWithFavorite, UserSession userSession) {
    IssueFilterDto issueFilter = issueFilterWithFavorite.issueFilter();
    json
      .beginObject()
      .prop("id", String.valueOf(issueFilter.getId()))
      .prop("name", issueFilter.getName())
      .prop("description", issueFilter.getDescription())
      .prop("user", firstNonNull(issueFilter.getUserLogin(), DEFAULT_LOGIN))
      .prop("shared", issueFilter.isShared())
      .prop("query", issueFilter.getData())
      .prop("canModify", canModifyFilter(userSession, issueFilter));
    if (issueFilterWithFavorite.isFavorite() != null) {
      json.prop("favorite", issueFilterWithFavorite.isFavorite());
    }
    json.endObject();
  }

  private static boolean canModifyFilter(UserSession userSession, IssueFilterDto filter) {
    return userSession.isLoggedIn() &&
      (StringUtils.equals(filter.getUserLogin(), userSession.getLogin()) || userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN));
  }

}
