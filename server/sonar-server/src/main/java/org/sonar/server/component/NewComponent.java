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
package org.sonar.server.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.component.ComponentValidator.checkComponentKey;
import static org.sonar.db.component.ComponentValidator.checkComponentName;
import static org.sonar.db.component.ComponentValidator.checkComponentQualifier;

@Immutable
public class NewComponent {
  private final String organizationUuid;
  private final String key;
  private final String branch;
  private final String qualifier;
  private final String name;
  private final boolean isPrivate;

  private NewComponent(NewComponent.Builder builder) {
    this.organizationUuid = builder.organizationUuid;
    this.key = builder.key;
    this.branch = builder.branch;
    this.qualifier = builder.qualifier;
    this.name = builder.name;
    this.isPrivate = builder.isPrivate;
  }

  public static Builder newComponentBuilder() {
    return new Builder();
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public String key() {
    return key;
  }

  public String name() {
    return name;
  }

  @CheckForNull
  public String deprecatedBranch() {
    return branch;
  }

  public String qualifier() {
    return qualifier;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public static class Builder {
    private String organizationUuid;
    private String key;
    private String qualifier = PROJECT;
    private String branch;
    private String name;
    private boolean isPrivate = false;

    private Builder() {
      // use static factory method newComponentBuilder()
    }

    public Builder setOrganizationUuid(String organizationUuid) {
      this.organizationUuid = organizationUuid;
      return this;
    }

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setDeprecatedBranch(@Nullable String s) {
      this.branch = s;
      return this;
    }

    public Builder setQualifier(String qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setPrivate(boolean isPrivate) {
      this.isPrivate = isPrivate;
      return this;
    }

    public NewComponent build() {
      requireNonNull(organizationUuid, "organization uuid can't be null");
      checkComponentKey(key);
      checkComponentName(name);
      checkComponentQualifier(qualifier);
      return new NewComponent(this);
    }
  }

}
