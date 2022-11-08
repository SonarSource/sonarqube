/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;
import java.util.Set;

public class PropertiesBag {
  @SerializedName("tags")
  private final Set<String> tags;
  @SerializedName("security-severity")
  private final String securitySeverity;

  private PropertiesBag(String securitySeverity, Set<String> tags) {
    this.tags = Set.copyOf(tags);
    this.securitySeverity = securitySeverity;
  }

  public static PropertiesBag of(String securitySeverity, Set<String> tags) {
    return new PropertiesBag(securitySeverity, tags);
  }

  public Set<String> getTags() {
    return tags;
  }

  public String getSecuritySeverity() {
    return securitySeverity;
  }
  
}
