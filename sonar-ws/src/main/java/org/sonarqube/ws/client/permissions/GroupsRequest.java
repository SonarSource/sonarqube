/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.ws.client.permissions;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/groups">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class GroupsRequest {

  private String organization;
  private String p;
  private String permission;
  private String projectId;
  private String projectKey;
  private String ps;
  private String q;

  /**
   * This is part of the internal API.
   * Example value: "my-org"
   */
  public GroupsRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "42"
   */
  public GroupsRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   */
  public GroupsRequest setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  public String getPermission() {
    return permission;
  }

  /**
   * Example value: "ce4c03d6-430f-40a9-b777-ad877c00aa4d"
   */
  public GroupsRequest setProjectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

  public String getProjectId() {
    return projectId;
  }

  /**
   * Example value: "my_project"
   */
  public GroupsRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  public String getProjectKey() {
    return projectKey;
  }

  /**
   * Example value: "20"
   */
  public GroupsRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Example value: "sonar"
   */
  public GroupsRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }
}
