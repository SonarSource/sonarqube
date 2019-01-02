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
package org.sonarqube.ws.client.components;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/components/suggestions">Further information about this action online (including a response example)</a>
 * @since 4.2
 */
@Generated("sonar-ws-generator")
public class SuggestionsRequest {

  private String more;
  private List<String> recentlyBrowsed;
  private String s;

  /**
   * Possible values:
   * <ul>
   *   <li>"VW"</li>
   *   <li>"SVW"</li>
   *   <li>"APP"</li>
   *   <li>"TRK"</li>
   *   <li>"BRC"</li>
   *   <li>"FIL"</li>
   *   <li>"UTS"</li>
   * </ul>
   */
  public SuggestionsRequest setMore(String more) {
    this.more = more;
    return this;
  }

  public String getMore() {
    return more;
  }

  /**
   * Example value: "org.sonarsource:sonarqube,some.other:project"
   */
  public SuggestionsRequest setRecentlyBrowsed(List<String> recentlyBrowsed) {
    this.recentlyBrowsed = recentlyBrowsed;
    return this;
  }

  public List<String> getRecentlyBrowsed() {
    return recentlyBrowsed;
  }

  /**
   * Example value: "sonar"
   */
  public SuggestionsRequest setS(String s) {
    this.s = s;
    return this;
  }

  public String getS() {
    return s;
  }
}
