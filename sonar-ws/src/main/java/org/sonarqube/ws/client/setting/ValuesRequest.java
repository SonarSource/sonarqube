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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class ValuesRequest {

  private final String component;
  private final List<String> keys;

  private ValuesRequest(Builder builder) {
    this.component = builder.component;
    this.keys = builder.keys;
  }

  @CheckForNull
  public String getComponent() {
    return component;
  }

  public List<String> getKeys() {
    return keys;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String component;
    private List<String> keys = new ArrayList<>();

    private Builder() {
      // enforce factory method use
    }

    public Builder setComponent(@Nullable String component) {
      this.component = component;
      return this;
    }

    public Builder setKeys(List<String> keys) {
      this.keys = requireNonNull(keys);
      return this;
    }

    public Builder setKeys(String... keys) {
      return setKeys(asList(keys));
    }

    public ValuesRequest build() {
      return new ValuesRequest(this);
    }
  }

}
