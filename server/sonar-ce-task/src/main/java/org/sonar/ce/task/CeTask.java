/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task;

import com.google.common.base.MoreObjects;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

@Immutable
public class CeTask {

  private final String organizationUuid;
  private final String type;
  private final String uuid;
  private final Component component;
  private final Component mainComponent;
  private final User submitter;
  private final Map<String, String> characteristics;

  private CeTask(Builder builder) {
    this.organizationUuid = requireNonNull(emptyToNull(builder.organizationUuid), "organizationUuid can't be null nor empty");
    this.uuid = requireNonNull(emptyToNull(builder.uuid), "uuid can't be null nor empty");
    this.type = requireNonNull(emptyToNull(builder.type), "type can't be null nor empty");
    checkArgument((builder.component == null) == (builder.mainComponent == null),
      "None or both component and main component must be non null");
    this.component = builder.component;
    this.mainComponent = builder.mainComponent;
    this.submitter = builder.submitter;
    if (builder.characteristics == null) {
      this.characteristics = emptyMap();
    } else {
      this.characteristics = unmodifiableMap(new HashMap<>(builder.characteristics));
    }
  }

  @Immutable
  public static final class User {
    private final String uuid;
    private final String login;

    public User(String uuid, @Nullable String login) {
      this.uuid = requireNonNull(uuid);
      this.login = emptyToNull(login);
    }

    public String getUuid() {
      return uuid;
    }

    @CheckForNull
    public String getLogin() {
      return login;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      User other = (User) o;
      return uuid.equals(other.uuid);
    }

    @Override
    public String toString() {
      return "User{" +
              "uuid='" + uuid + '\'' +
              ", login='" + login + '\'' +
              '}';
    }

    @Override
    public int hashCode() {
      return uuid.hashCode();
    }
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public String getUuid() {
    return uuid;
  }

  public String getType() {
    return type;
  }

  public Optional<Component> getComponent() {
    return Optional.ofNullable(component);
  }

  public Optional<Component> getMainComponent() {
    return Optional.ofNullable(mainComponent);
  }

  @CheckForNull
  public User getSubmitter() {
    return submitter;
  }

  public Map<String, String> getCharacteristics() {
    return characteristics;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("organizationUuid", organizationUuid)
      .add("type", type)
      .add("uuid", uuid)
      .add("component", component)
      .add("mainComponent", mainComponent)
      .add("submitter", submitter)
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
    private String organizationUuid;
    private String uuid;
    private String type;
    private Component component;
    private Component mainComponent;
    private User submitter;
    private Map<String, String> characteristics;

    public Builder setOrganizationUuid(String organizationUuid) {
      this.organizationUuid = organizationUuid;
      return this;
    }

    // FIXME remove this method when organization support is added to the Compute Engine queue
    public boolean hasOrganizationUuid() {
      return organizationUuid != null;
    }

    public Builder setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setType(String type) {
      this.type = type;
      return this;
    }

    public Builder setComponent(@Nullable Component component) {
      this.component = component;
      return this;
    }

    public Builder setMainComponent(@Nullable Component mainComponent) {
      this.mainComponent = mainComponent;
      return this;
    }

    public Builder setSubmitter(@Nullable User s) {
      this.submitter = s;
      return this;
    }

    public Builder setCharacteristics(@Nullable Map<String, String> m) {
      this.characteristics = m;
      return this;
    }

    public CeTask build() {
      return new CeTask(this);
    }
  }

  public static final class Component {
    private final String uuid;
    @CheckForNull
    private final String key;
    @CheckForNull
    private final String name;

    public Component(String uuid, @Nullable String key, @Nullable String name) {
      this.uuid = requireNonNull(emptyToNull(uuid), "uuid can't be null nor empty");
      this.key = emptyToNull(key);
      this.name = emptyToNull(name);
    }

    public String getUuid() {
      return uuid;
    }

    public Optional<String> getKey() {
      return Optional.ofNullable(key);
    }

    public Optional<String> getName() {
      return Optional.ofNullable(name);
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
      return Objects.equals(uuid, component.uuid) &&
        Objects.equals(key, component.key) &&
        Objects.equals(name, component.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(uuid, key, name);
    }

    @Override
    public String toString() {
      return "Component{" +
        "uuid='" + uuid + '\'' +
        ", key='" + key + '\'' +
        ", name='" + name + '\'' +
        '}';
    }
  }
}
