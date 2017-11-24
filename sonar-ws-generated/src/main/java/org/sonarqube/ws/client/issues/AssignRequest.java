/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.issues;

import java.util.List;
import javax.annotation.Generated;

/**
 * Assign/Unassign an issue. Requires authentication and Browse permission on project
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/issues/assign">Further information about this action online (including a response example)</a>
 * @since 3.6
 */
@Generated("sonar-ws-generator")
public class AssignRequest {

  private String assignee;
  private String issue;
  private String me;

  /**
   * Login of the assignee. When not set, it will unassign the issue. Use '_me' to assign to current user
   *
   * Example value: "admin"
   */
  public AssignRequest setAssignee(String assignee) {
    this.assignee = assignee;
    return this;
  }

  public String getAssignee() {
    return assignee;
  }

  /**
   * Issue key
   *
   * This is a mandatory parameter.
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public AssignRequest setIssue(String issue) {
    this.issue = issue;
    return this;
  }

  public String getIssue() {
    return issue;
  }

  /**
   * (deprecated) Assign the issue to the logged-in user. Replaced by the parameter assignee=_me
   *
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   * @deprecated since 5.2
   */
  @Deprecated
  public AssignRequest setMe(String me) {
    this.me = me;
    return this;
  }

  public String getMe() {
    return me;
  }
}
