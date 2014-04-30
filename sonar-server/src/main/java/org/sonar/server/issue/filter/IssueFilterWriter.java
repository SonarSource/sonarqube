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
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.issue.DefaultIssueFilter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.user.UserSession;

public class IssueFilterWriter implements ServerComponent {

  void write(UserSession session, DefaultIssueFilter filter, JsonWriter json) {
    json.name("filter").beginObject()
      .prop("id", filter.id())
      .prop("name", filter.name())
      .prop("description", filter.description())
      .prop("user", filter.user())
      .prop("shared", filter.shared())
      .prop("query", filter.data())
      .prop("canModify", canModifyFilter(session, filter))
      .endObject();
  }

  private boolean canModifyFilter(UserSession session, DefaultIssueFilter filter) {
    return session.isLoggedIn() &&
      (StringUtils.equals(filter.user(), session.login()) || session.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN));
  }

}
