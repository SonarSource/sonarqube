/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.pushapi.sonarlint;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SonarLintPushEvent {

  private final String name;
  private final byte[] data;
  private final String projectUuid;
  private final String language;

  public SonarLintPushEvent(String name, byte[] data, String projectUuid, @Nullable String language) {
    this.name = name;
    this.data = data;
    this.projectUuid = projectUuid;
    this.language = language;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  public String getName() {
    return name;
  }

  public byte[] getData() {
    return data;
  }

  public String serialize() {
    return "event: " + this.name + "\n"
      + "data: " + new String(this.data, UTF_8);
  }

}
