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
package org.sonarqube.ws.client.sources;

/*
 * THIS FILE HAS BEEN AUTOMATICALLY GENERATED
 */

import java.util.List;

/**
 * Get source code. Require See Source Code permission on file's project<br/>Each element of the result array is composed of:<ol><li>Line number</li><li>Content of the line</li></ol>
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/sources/show">Further information about this action online (including a response example)</a>
 * @since 4.4
 */
public class ShowRequest {

  private String from;
  private String key;
  private String to;

  /**
   * First line to return. Starts at 1
   *
   * Example value: "10"
   */
  public ShowRequest setFrom(String from) {
    this.from = from;
    return this;
  }

  public String getFrom() {
    return from;
  }

  /**
   * File key
   *
   * This is a mandatory parameter.
   * Example value: "my_project:/src/foo/Bar.php"
   */
  public ShowRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * Last line to return (inclusive)
   *
   * Example value: "20"
   */
  public ShowRequest setTo(String to) {
    this.to = to;
    return this;
  }

  public String getTo() {
    return to;
  }
}
