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
package org.sonarqube.ws.client.views;

import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/views/mode">Further information about this action online (including a response example)</a>
 * @since 2.6
 */
@Generated("sonar-ws-generator")
public class ModeRequest {

  private String key;
  private String measure;
  private String regexp;
  private String selectionMode;
  private String value;

  /**
   * This is a mandatory parameter.
   */
  public ModeRequest setKey(String key) {
    this.key = key;
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   */
  public ModeRequest setMeasure(String measure) {
    this.measure = measure;
    return this;
  }

  public String getMeasure() {
    return measure;
  }

  /**
   */
  public ModeRequest setRegexp(String regexp) {
    this.regexp = regexp;
    return this;
  }

  public String getRegexp() {
    return regexp;
  }

  /**
   * This is a mandatory parameter.
   * Possible values:
   * <ul>
   *   <li>"MANUAL"</li>
   *   <li>"REGEXP"</li>
   *   <li>"MANUAL_MEASURE"</li>
   *   <li>"REST"</li>
   * </ul>
   */
  public ModeRequest setSelectionMode(String selectionMode) {
    this.selectionMode = selectionMode;
    return this;
  }

  public String getSelectionMode() {
    return selectionMode;
  }

  /**
   */
  public ModeRequest setValue(String value) {
    this.value = value;
    return this;
  }

  public String getValue() {
    return value;
  }
}
