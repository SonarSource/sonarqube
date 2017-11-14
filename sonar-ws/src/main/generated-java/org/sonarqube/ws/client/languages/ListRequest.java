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
package org.sonarqube.ws.client.languages;

/*
 * THIS FILE HAS BEEN AUTOMATICALLY GENERATED
 */

import java.util.List;

/**
 * List supported programming languages
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/languages/list">Further information about this action online (including a response example)</a>
 * @since 5.1
 */
public class ListRequest {

  private String ps;
  private String q;

  /**
   * The size of the list to return, 0 for all languages
   *
   * Example value: "25"
   */
  public ListRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * A pattern to match language keys/names against
   *
   * Example value: "java"
   */
  public ListRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }
}
