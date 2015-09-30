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
package org.sonar.server.computation.queue;

import com.google.common.base.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Objects.requireNonNull;

@Immutable
public class CeTask {

  private final String type;
  private final String uuid;
  private final String componentUuid;
  private final String componentKey;
  private final String componentName;
  private final String submitterLogin;

  private CeTask(Builder builder) {
    this.uuid = requireNonNull(emptyToNull(builder.uuid));
    this.type = requireNonNull(emptyToNull(builder.type));
    this.componentUuid = emptyToNull(builder.componentUuid);
    this.componentKey = emptyToNull(builder.componentKey);
    this.componentName = emptyToNull(builder.componentName);
    this.submitterLogin = emptyToNull(builder.submitterLogin);
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
  public String getComponentKey() {
    return componentKey;
  }

  @CheckForNull
  public String getComponentName() {
    return componentName;
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

  public static final class Builder {
    private String uuid;
    private String type;
    private String componentUuid;
    private String componentKey;
    private String componentName;
    private String submitterLogin;

    public Builder setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setComponentUuid(String componentUuid) {
      this.componentUuid = componentUuid;
      return this;
    }

    public Builder setComponentKey(@Nullable String s) {
      this.componentKey = s;
      return this;
    }

    public Builder setComponentName(@Nullable String s) {
      this.componentName = s;
      return this;
    }

    public Builder setSubmitterLogin(@Nullable String s) {
      this.submitterLogin = s;
      return this;
    }

    public CeTask build() {
      return new CeTask(this);
    }
  }
}
