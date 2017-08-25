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
package org.sonarqube.ws.client.setting;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

public class ResetRequest {
  private final List<String> keys;
  private final String component;
  private final String branch;

  private ResetRequest(Builder builder) {
    this.keys = builder.keys;
    this.component = builder.component;
    this.branch = builder.branch;
  }

  public List<String> getKeys() {
    return keys;
  }

  @CheckForNull
  public String getComponent() {
    return component;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<String> keys;
    private String component;
    private String branch;

    private Builder() {
      // enforce factory method use
    }

    public Builder setKeys(List<String> keys) {
      this.keys = keys;
      return this;
    }

    public Builder setKeys(String... keys) {
      setKeys(asList(keys));
      return this;
    }

    public Builder setComponent(@Nullable String component) {
      this.component = component;
      return this;
    }

    public Builder setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    public ResetRequest build() {
      checkArgument(keys != null && !keys.isEmpty(), "Setting keys is mandatory and must not be empty.");
      return new ResetRequest(this);
    }
  }
}
