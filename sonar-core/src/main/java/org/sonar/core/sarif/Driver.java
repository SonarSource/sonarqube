/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;
import java.util.Set;

public class Driver {
  private static final String TOOL_NAME = "SonarQube";
  private static final String ORGANIZATION_NAME = "SonarSource";

  @SerializedName("name")
  private final String name;
  @SerializedName("organization")
  private final String organization;
  @SerializedName("semanticVersion")
  private final String semanticVersion;
  @SerializedName("rules")
  private final Set<Rule> rules;

  public Driver(String semanticVersion, Set<Rule> rules) {
    this(TOOL_NAME, ORGANIZATION_NAME, semanticVersion, rules);
  }

  private Driver(String name, String organization, String semanticVersion, Set<Rule> rules) {
    this.name = name;
    this.organization = organization;
    this.semanticVersion = semanticVersion;
    this.rules = Set.copyOf(rules);
  }

  public String getName() {
    return name;
  }

  public String getOrganization() {
    return organization;
  }

  public String getSemanticVersion() {
    return semanticVersion;
  }

  public Set<Rule> getRules() {
    return rules;
  }
}
