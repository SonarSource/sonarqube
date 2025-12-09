/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonarqube.ws.client.qualitygates;

import jakarta.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/qualitygate/remove_group">Further information about this action online (including a response example)</a>
 * @since 9.2
 */
@Generated("sonar-ws-generator")
public class RemoveGroupRequest {

  private String group;
  private String qualityGate;

  /**
   * This is a mandatory parameter.
   * Example value: "sonar-administrators"
   */
  public RemoveGroupRequest setGroup(String group) {
    this.group = group;
    return this;
  }

  public String getGroup() {
    return group;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "SonarSource Way"
   */
  public RemoveGroupRequest setQualityGate(String qualityGate) {
    this.qualityGate = qualityGate;
    return this;
  }

  public String getQualityGate() {
    return qualityGate;
  }
}
