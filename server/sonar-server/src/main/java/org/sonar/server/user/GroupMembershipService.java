/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user;

import java.util.Map;
import org.sonar.api.server.ServerSide;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.server.util.RubyUtils;

/**
 * Used by ruby code <pre>Internal.group_membership</pre>
 */
@ServerSide
public class GroupMembershipService {

  private static final String SELECTED_MEMBERSHIP = "selected";
  private static final String DESELECTED_MEMBERSHIP = "deselected";

  private final GroupMembershipFinder finder;

  public GroupMembershipService(GroupMembershipFinder finder) {
    this.finder = finder;
  }

  public GroupMembershipFinder.Membership find(Map<String, Object> params) {
    return finder.find(parseQuery(params));
  }

  private static GroupMembershipQuery parseQuery(Map<String, Object> params) {
    GroupMembershipQuery.Builder builder = GroupMembershipQuery.builder();
    builder.membership(membership(params));
    builder.groupSearch((String) params.get("query"));
    builder.pageIndex(RubyUtils.toInteger(params.get("page")));
    builder.pageSize(RubyUtils.toInteger(params.get("pageSize")));
    builder.login((String) params.get("user"));
    return builder.build();
  }

  private static String membership(Map<String, Object> params) {
    String selected = (String) params.get("selected");
    if (SELECTED_MEMBERSHIP.equals(selected)) {
      return GroupMembershipQuery.IN;
    }
    if (DESELECTED_MEMBERSHIP.equals(selected)) {
      return GroupMembershipQuery.OUT;
    }
    return GroupMembershipQuery.ANY;
  }
}
