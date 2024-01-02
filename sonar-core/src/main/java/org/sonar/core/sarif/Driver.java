/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Driver {

  @SerializedName("name")
  private final String name;
  @SerializedName("organization")
  private final String organization;
  @SerializedName("semanticVersion")
  private final String semanticVersion;
  @SerializedName("rules")
  private final Set<Rule> rules;

  private Driver(String name, @Nullable String organization, @Nullable String semanticVersion, Set<Rule> rules) {
    this.name = name;
    this.organization = organization;
    this.semanticVersion = semanticVersion;
    this.rules = Set.copyOf(rules);
  }

  public String getName() {
    return name;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  @CheckForNull
  public String getSemanticVersion() {
    return semanticVersion;
  }

  public Set<Rule> getRules() {
    return rules;
  }

  public static DriverBuilder builder() {
    return new DriverBuilder();
  }

  public static final class DriverBuilder {
    private String name;
    private String organization;
    private String semanticVersion;
    private Set<Rule> rules;

    private DriverBuilder() {
    }

    public DriverBuilder name(String name) {
      this.name = name;
      return this;
    }

    public DriverBuilder organization(String organization) {
      this.organization = organization;
      return this;
    }

    public DriverBuilder semanticVersion(String semanticVersion) {
      this.semanticVersion = semanticVersion;
      return this;
    }

    public DriverBuilder rules(Set<Rule> rules) {
      this.rules = rules;
      return this;
    }

    public Driver build() {
      return new Driver(name, organization, semanticVersion, rules);
    }
  }
}
