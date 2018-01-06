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
package org.sonarqube.ws.client.sources;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/sources/lines">Further information about this action online (including a response example)</a>
 * @since 5.0
 */
@Generated("sonar-ws-generator")
public class LinesRequest {

  private String branch;
  private String from;
  private String key;
  private String to;
  private String uuid;

  /**
   * This is part of the internal API.
   * Example value: "feature/my_branch"
   */
  public LinesRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Example value: "10"
   */
  public LinesRequest setFrom(String from) {
    this.from = from;
    return this;
  }

  public String getFrom() {
    return from;
  }

  /**
   * Example value: "my_project:/src/foo/Bar.php"
   */
  public LinesRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * Example value: "20"
   */
  public LinesRequest setTo(String to) {
    this.to = to;
    return this;
  }

  public String getTo() {
    return to;
  }

  /**
   * Example value: "f333aab4-7e3a-4d70-87e1-f4c491f05e5c"
   */
  public LinesRequest setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getUuid() {
    return uuid;
  }
}
