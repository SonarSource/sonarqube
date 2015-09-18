/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation;

import javax.annotation.Nullable;

public class TestTaskSubmission implements TaskSubmission {
  private final String uuid;
  private String type;
  private String componentUuid;
  private String submitterLogin;

  public TestTaskSubmission(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public TaskSubmission setType(String s) {
    this.type = s;
    return this;
  }

  @Override
  public String getComponentUuid() {
    return componentUuid;
  }

  @Override
  public TaskSubmission setComponentUuid(@Nullable String s) {
    this.componentUuid = s;
    return this;
  }

  @Override
  public String getSubmitterLogin() {
    return submitterLogin;
  }

  @Override
  public TaskSubmission setSubmitterLogin(@Nullable String s) {
    this.submitterLogin = s;
    return this;
  }
}
