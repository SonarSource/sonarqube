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
package org.sonarqube.ws.client.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class ShowWsRequest {
  private String id;
  private String key;
  private String branch;

  @CheckForNull
  public String getId() {
    return id;
  }

  public ShowWsRequest setId(@Nullable String id) {
    this.id = id;
    return this;
  }

  @CheckForNull
  public String getKey() {
    return key;
  }

  public ShowWsRequest setKey(@Nullable String key) {
    this.key = key;
    return this;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public ShowWsRequest setBranch(@Nullable String branch) {
    this.branch = branch;
    return this;
  }
}
