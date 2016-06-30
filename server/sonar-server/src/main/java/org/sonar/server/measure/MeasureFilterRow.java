/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.measure;

import org.apache.commons.lang.StringUtils;

public class MeasureFilterRow {
  private final String componentUuid;
  private final String rootComponentUuid;
  private String sortText = null;
  private Long sortDate = null;
  private Double sortDouble = null;

  MeasureFilterRow(String componentUuid, String rootComponentUuid) {
    this.componentUuid = componentUuid;
    this.rootComponentUuid = rootComponentUuid;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public String getRootComponentUuid() {
    return rootComponentUuid;
  }

  public String getSortText() {
    return sortText;
  }

  void setSortText(String s) {
    this.sortText = StringUtils.defaultString(s);
  }

  Long getSortDate() {
    return sortDate;
  }

  void setSortDate(Long sortDate) {
    this.sortDate = sortDate;
  }

  Double getSortDouble() {
    return sortDouble;
  }

  void setSortDouble(Double sortDouble) {
    this.sortDouble = sortDouble;
  }
}
