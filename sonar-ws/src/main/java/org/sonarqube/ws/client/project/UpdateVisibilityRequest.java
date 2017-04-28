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
package org.sonarqube.ws.client.project;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UpdateVisibilityRequest {
  private final String project;
  private final String visibility;

  public UpdateVisibilityRequest(Builder builder) {
    this.project = builder.project;
    this.visibility = builder.visibility;
  }

  @CheckForNull
  public String getProject() {
    return project;
  }

  @CheckForNull
  public String getVisibility() {
    return visibility;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String project;
    private String visibility;

    public Builder setProject(@Nullable String project) {
      this.project = project;
      return this;
    }

    public Builder setVisibility(@Nullable String visibility) {
      this.visibility = visibility;
      return this;
    }

    public UpdateVisibilityRequest build() {
      return new UpdateVisibilityRequest(this);
    }
  }
}
