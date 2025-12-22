/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.component.BranchDto;
import org.sonar.db.portfolio.PortfolioDto;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

@Immutable
public final class CeTaskSubmit {

  private final String uuid;
  private final String type;
  private final Component component;
  private final String submitterUuid;
  private final Map<String, String> characteristics;
  private final int reportPartCount;

  private CeTaskSubmit(Builder builder) {
    this.uuid = requireNonNull(builder.uuid);
    this.type = requireNonNull(builder.type);
    this.component = builder.component;
    this.submitterUuid = builder.submitterUuid;
    this.characteristics = unmodifiableMap(builder.characteristics);
    this.reportPartCount = builder.reportPartCount;
  }

  public String getType() {
    return type;
  }

  public String getUuid() {
    return uuid;
  }

  public Optional<Component> getComponent() {
    return Optional.ofNullable(component);
  }

  @CheckForNull
  public String getSubmitterUuid() {
    return submitterUuid;
  }

  public Map<String, String> getCharacteristics() {
    return characteristics;
  }

  public int getReportPartCount() {
    return reportPartCount;
  }

  public static final class Builder {
    private final String uuid;
    private String type;
    private Component component;
    private String submitterUuid;
    private Map<String, String> characteristics = null;
    private  int reportPartCount = 1;

    public Builder(String uuid) {
      this.uuid = emptyToNull(uuid);
    }

    public String getUuid() {
      return uuid;
    }

    public Builder setType(String s) {
      this.type = emptyToNull(s);
      return this;
    }

    public Builder setComponent(@Nullable Component component) {
      this.component = component;
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

    public Builder setReportPartCount(int reportPartCount) {
      this.reportPartCount = reportPartCount;
      return this;
    }

    public CeTaskSubmit build() {
      requireNonNull(uuid, "uuid can't be null");
      requireNonNull(type, "type can't be null");
      requireNonNull(characteristics, "characteristics can't be null");
      return new CeTaskSubmit(this);
    }
  }

  public static class Component {
    private final String uuid;
    private final String entityUuid;

    public Component(String uuid, String entityUuid) {
      this.uuid = requireNonNull(nullToEmpty(uuid), "uuid can't be null");
      this.entityUuid = requireNonNull(nullToEmpty(entityUuid), "mainComponentUuid can't be null");
    }

    public static Component fromDto(String componentUuid, String entityUuid) {
      return new Component(componentUuid, entityUuid);
    }

    public static Component fromDto(PortfolioDto dto) {
      return new Component(dto.getUuid(), dto.getUuid());
    }

    public static Component fromDto(BranchDto dto) {
      return new Component(dto.getUuid(), dto.getProjectUuid());
    }

    public String getUuid() {
      return uuid;
    }

    public String getEntityUuid() {
      return entityUuid;
    }

    @Override
    public String toString() {
      return "Component{" +
        "uuid='" + uuid + '\'' +
        ", entityUuid='" + entityUuid + '\'' +
        '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Component component = (Component) o;
      return uuid.equals(component.uuid) && entityUuid.equals(component.entityUuid);
    }

    @Override
    public int hashCode() {
      return Objects.hash(uuid, entityUuid);
    }
  }
}
