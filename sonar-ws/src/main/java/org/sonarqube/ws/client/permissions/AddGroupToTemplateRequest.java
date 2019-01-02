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
package org.sonarqube.ws.client.permissions;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/add_group_to_template">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class AddGroupToTemplateRequest {

  private String groupId;
  private String groupName;
  private String organization;
  private String permission;
  private String templateId;
  private String templateName;

  /**
   * Example value: "42"
   */
  public AddGroupToTemplateRequest setGroupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  public String getGroupId() {
    return groupId;
  }

  /**
   * Example value: "sonar-administrators"
   */
  public AddGroupToTemplateRequest setGroupName(String groupName) {
    this.groupName = groupName;
    return this;
  }

  public String getGroupName() {
    return groupName;
  }

  /**
   * This is part of the internal API.
   * Example value: "my-org"
   */
  public AddGroupToTemplateRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * This is a mandatory parameter.
   * Possible values:
   * <ul>
   *   <li>"admin"</li>
   *   <li>"codeviewer"</li>
   *   <li>"issueadmin"</li>
   *   <li>"scan"</li>
   *   <li>"user"</li>
   * </ul>
   */
  public AddGroupToTemplateRequest setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  public String getPermission() {
    return permission;
  }

  /**
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public AddGroupToTemplateRequest setTemplateId(String templateId) {
    this.templateId = templateId;
    return this;
  }

  public String getTemplateId() {
    return templateId;
  }

  /**
   * Example value: "Default Permission Template for Projects"
   */
  public AddGroupToTemplateRequest setTemplateName(String templateName) {
    this.templateName = templateName;
    return this;
  }

  public String getTemplateName() {
    return templateName;
  }
}
