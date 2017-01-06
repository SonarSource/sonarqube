/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import javax.annotation.concurrent.Immutable;
import org.sonar.api.resources.Qualifiers;

import static java.util.Objects.requireNonNull;
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

  private NewComponent(NewComponent.Builder builder) {
    this.organizationUuid = builder.organizationUuid;
    this.key = builder.key;
    this.branch = builder.branch;
    this.qualifier = builder.qualifier == null ? Qualifiers.PROJECT : checkComponentQualifier(builder.qualifier);
    this.name = builder.name;
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
  public String branch() {
    return branch;
  }

  public String qualifier() {
    return qualifier;
  }

  public static class Builder {
    private String organizationUuid;
    private String key;
    private String branch;
    private String qualifier;
    private String name;

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

    public Builder setBranch(String branch) {
      this.branch = branch;
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

    public NewComponent build() {
      requireNonNull(organizationUuid, "organization uuid can't be null");
      checkComponentKey(requireNonNull(key, "key can't be null"));
      checkComponentName(requireNonNull(name, "name can't be null"));
      return new NewComponent(this);
    }
  }

}
