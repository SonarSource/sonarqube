/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.telemetry;

import java.util.Date;
import org.sonar.api.platform.Server;

import static org.apache.commons.lang3.RandomStringUtils.secure;

public class FakeServer extends Server {
  private String id;
  private String version;

  public FakeServer() {
    this.id = secure().nextAlphanumeric(20);
    this.version = secure().nextAlphanumeric(10);
  }

  @Override
  public String getId() {
    return id;
  }

  public FakeServer setId(String id) {
    this.id = id;
    return this;
  }

  @Override
  public String getVersion() {
    return this.version;
  }

  public FakeServer setVersion(String version) {
    this.version = version;
    return this;
  }

  @Override
  public Date getStartedAt() {
    return null;
  }

  @Override
  public String getContextPath() {
    return null;
  }

  @Override
  public String getPublicRootUrl() {
    return null;
  }
}
