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
package org.sonar.server.permission.ws;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.server.ws.WsUtils.checkRequest;

/**
 * Project identifiers from a WS request. Guaranties the project id and project key are not provided at the same time.
 */
public class WsProjectRef {
  private static final String MSG_ID_OR_KEY_MUST_BE_PROVIDED = "Project id or project key must be provided, not both.";
  private final String uuid;
  private final String key;

  private WsProjectRef(@Nullable String uuid, @Nullable String key) {
    this.uuid = uuid;
    this.key = key;
    checkRequest(this.uuid != null ^ this.key != null, "Project id or project key can be provided, not both.");
  }

  public static Optional<WsProjectRef> newOptionalWsProjectRef(@Nullable String uuid, @Nullable String key) {
    if (uuid == null && key == null) {
      return Optional.absent();
    }

    return Optional.of(new WsProjectRef(uuid, key));
  }

  public static WsProjectRef newWsProjectRef(@Nullable String uuid, @Nullable String key) {
    checkRequest(uuid == null ^ key == null, MSG_ID_OR_KEY_MUST_BE_PROVIDED);
    return new WsProjectRef(uuid, key);
  }

  @CheckForNull
  public String uuid() {
    return this.uuid;
  }

  @CheckForNull
  public String key() {
    return this.key;
  }
}
