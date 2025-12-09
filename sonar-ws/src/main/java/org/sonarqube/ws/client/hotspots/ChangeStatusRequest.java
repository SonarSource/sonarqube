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
package org.sonarqube.ws.client.hotspots;

import jakarta.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/hotspots/change_status">Further information about this action online (including a response example)</a>
 * @since 8.1
 */
@Generated("sonar-ws-generator")
public class ChangeStatusRequest {

  private String comment;
  private String hotspot;
  private String resolution;
  private String status;

  /**
   * Example value: "This is safe because user input is validated by the calling code"
   */
  public ChangeStatusRequest setComment(String comment) {
    this.comment = comment;
    return this;
  }

  public String getComment() {
    return comment;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "AU-TpxcA-iU5OvuD2FL0"
   */
  public ChangeStatusRequest setHotspot(String hotspot) {
    this.hotspot = hotspot;
    return this;
  }

  public String getHotspot() {
    return hotspot;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"FIXED"</li>
   *   <li>"SAFE"</li>
   * </ul>
   */
  public ChangeStatusRequest setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String getResolution() {
    return resolution;
  }

  /**
   * This is a mandatory parameter.
   * Possible values:
   * <ul>
   *   <li>"TO_REVIEW"</li>
   *   <li>"REVIEWED"</li>
   * </ul>
   */
  public ChangeStatusRequest setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getStatus() {
    return status;
  }
}
