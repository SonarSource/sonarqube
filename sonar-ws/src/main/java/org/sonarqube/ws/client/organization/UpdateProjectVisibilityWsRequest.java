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
package org.sonarqube.ws.client.organization;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class UpdateProjectVisibilityWsRequest {
  private final String organization;
  private final String projectVisibility;

  private UpdateProjectVisibilityWsRequest(Builder builder) {
    this.organization = builder.organization;
    this.projectVisibility = builder.projectVisibility;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  @CheckForNull
  public String getProjectVisibility() {
    return projectVisibility;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String organization;
    private String projectVisibility;

    public Builder setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setProjectVisibility(@Nullable String projectVisibility) {
      this.projectVisibility = projectVisibility;
      return this;
    }

    public UpdateProjectVisibilityWsRequest build() {
      return new UpdateProjectVisibilityWsRequest(this);
    }
  }
}
