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
