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

import com.google.common.base.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.ce.CeQueueDto;

import static java.util.Objects.requireNonNull;

@Immutable
public class CeTask {

  private final String type;
  private final String uuid;
  private final String componentUuid;
  private final String submitterLogin;

  public CeTask(String uuid, String type, @Nullable String componentUuid, @Nullable String submitterLogin) {
    this.uuid = requireNonNull(uuid);
    this.type = requireNonNull(type);
    this.componentUuid = componentUuid;
    this.submitterLogin = submitterLogin;
  }

  CeTask(CeTaskSubmit submit) {
    this(submit.getUuid(), submit.getType(), submit.getComponentUuid(), submit.getSubmitterLogin());
  }

  CeTask(CeQueueDto dto) {
    this(dto.getUuid(), dto.getTaskType(), dto.getComponentUuid(), dto.getSubmitterLogin());
  }

  public String getUuid() {
    return uuid;
  }

  public String getType() {
    return type;
  }

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  @CheckForNull
  public String getSubmitterLogin() {
    return submitterLogin;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("componentUuid", componentUuid)
      .add("uuid", uuid)
      .add("type", type)
      .add("submitterLogin", submitterLogin)
      .toString();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CeTask ceTask = (CeTask) o;
    return uuid.equals(ceTask.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }
}
