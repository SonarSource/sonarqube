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
 * @author Evgeny Mandrikov
 */
public class Server extends Model {

  public enum Status {
    SETUP,
    UP,
    DOWN,

    /**
     * @since 3.3
     */
    MIGRATION_RUNNING
  }

  private String id;
  private String version;
  private Status status;
  private String statusMessage;

  @CheckForNull
  public String getVersion() {
    return version;
  }

  @CheckForNull
  public String getId() {
    return id;
  }

  public Server setVersion(@Nullable String s) {
    this.version = s;
    return this;
  }

  public Server setId(@Nullable String id) {
    this.id = id;
    return this;
  }

  @CheckForNull
  public Status getStatus() {
    return status;
  }

  @CheckForNull
  public String getStatusMessage() {
    return statusMessage;
  }

  public Server setStatus(Status status) {
    this.status = status;
    return this;
  }

  public Server setStatusMessage(@Nullable String statusMessage) {
    this.statusMessage = statusMessage;
    return this;
  }

}
