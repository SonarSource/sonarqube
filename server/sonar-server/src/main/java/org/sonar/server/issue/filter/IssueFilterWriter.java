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
import org.sonar.api.ServerSide;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.user.UserSession;

@ServerSide
public class IssueFilterWriter {

  void write(UserSession session, IssueFilterDto filter, JsonWriter json) {
    json.name("filter").beginObject()
      .prop("id", filter.getId())
      .prop("name", filter.getName())
      .prop("description", filter.getDescription())
      .prop("user", filter.getUserLogin())
      .prop("shared", filter.isShared())
      .prop("query", filter.getData())
      .prop("canModify", canModifyFilter(session, filter))
      .endObject();
  }

  private boolean canModifyFilter(UserSession session, IssueFilterDto filter) {
    return session.isLoggedIn() &&
      (StringUtils.equals(filter.getUserLogin(), session.login()) || session.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN));
  }

}
