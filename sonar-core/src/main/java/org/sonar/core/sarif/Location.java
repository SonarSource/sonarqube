/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;

public class Location {
  @SerializedName("physicalLocation")
  private final PhysicalLocation physicalLocation;

  private Location(PhysicalLocation physicalLocation) {
    this.physicalLocation = physicalLocation;
  }

  public static Location of(PhysicalLocation physicalLocation) {
    return new Location(physicalLocation);
  }

  public PhysicalLocation getPhysicalLocation() {
    return physicalLocation;
  }

}
