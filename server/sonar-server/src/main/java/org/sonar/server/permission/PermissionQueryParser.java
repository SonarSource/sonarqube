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

package org.sonar.server.permission;

import java.util.Map;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.server.util.RubyUtils;

public class PermissionQueryParser {

  private PermissionQueryParser() {
    // Utility class
  }

  static PermissionQuery toQuery(Map<String, Object> params) {
    PermissionQuery.Builder builder = PermissionQuery.builder();
    builder.permission((String) params.get("permission"));
    builder.component((String) params.get("component"));
    builder.template((String) params.get("template"));
    builder.membership(toMembership((String) params.get("selected")));
    builder.search((String) params.get("query"));
    builder.pageIndex(RubyUtils.toInteger(params.get("page")));
    builder.pageSize(RubyUtils.toInteger(params.get("pageSize")));
    return builder.build();
  }

  public static String toMembership(String selectionModeString) {
    SelectionMode selectionMode = SelectionMode.fromParam(selectionModeString);
    if (SelectionMode.SELECTED == selectionMode) {
      return GroupMembershipQuery.IN;
    } else if (SelectionMode.DESELECTED == selectionMode) {
      return GroupMembershipQuery.OUT;
    } else {
      return GroupMembershipQuery.ANY;
    }
  }

}
