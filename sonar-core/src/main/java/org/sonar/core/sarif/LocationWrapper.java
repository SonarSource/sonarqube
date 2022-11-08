/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;

public class LocationWrapper {
  @SerializedName("location")
  private final Location location;

  private LocationWrapper(Location location) {
    this.location = location;
  }

  public static LocationWrapper of(Location location)  {
    return new LocationWrapper(location);
  }

  public Location getLocation() {
    return location;
  }
}
