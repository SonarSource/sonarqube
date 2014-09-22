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
package org.sonar.wsclient.services;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 2.9
 */
public class ServerSetup extends Model {
  private String status;
  private String message;

  @CheckForNull
  public String getStatus() {
    return status;
  }

  public ServerSetup setStatus(@Nullable String status) {
    this.status = status;
    return this;
  }

  @CheckForNull
  public String getMessage() {
    return message;
  }

  public ServerSetup setMessage(@Nullable String message) {
    this.message = message;
    return this;
  }

  public boolean isSuccessful() {
    return "ok".equals(status);
  }
}
