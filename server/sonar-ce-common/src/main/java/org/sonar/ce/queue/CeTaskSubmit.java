/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.ce.queue;

import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

@Immutable
public final class CeTaskSubmit {

  private final String uuid;
  private final String type;
  private final String componentUuid;
  private final String submitterUuid;
  private final Map<String, String> characteristics;

  private CeTaskSubmit(Builder builder) {
    this.uuid = requireNonNull(emptyToNull(builder.uuid));
    this.type = requireNonNull(emptyToNull(builder.type));
    this.componentUuid = emptyToNull(builder.componentUuid);
    this.submitterUuid = emptyToNull(builder.submitterUuid);
    this.characteristics = unmodifiableMap(requireNonNull(builder.characteristics));
  }

  public String getType() {
    return type;
  }

  public String getUuid() {
    return uuid;
  }

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  @CheckForNull
  public String getSubmitterUuid() {
    return submitterUuid;
  }

  public Map<String, String> getCharacteristics() {
    return characteristics;
  }

  public static final class Builder {
    private final String uuid;
    private String type;
    private String componentUuid;
    private String submitterUuid;
    private Map<String, String> characteristics = null;

    public Builder(String uuid) {
      this.uuid = uuid;
    }

    public String getUuid() {
      return uuid;
    }

    public Builder setType(String s) {
      this.type = s;
      return this;
    }

    public Builder setComponentUuid(@Nullable String s) {
      this.componentUuid = s;
      return this;
    }

    public Builder setSubmitterUuid(@Nullable String s) {
      this.submitterUuid = s;
      return this;
    }

    public Builder setCharacteristics(Map<String, String> m) {
      this.characteristics = m;
      return this;
    }

    public CeTaskSubmit build() {
      return new CeTaskSubmit(this);
    }
  }
}
