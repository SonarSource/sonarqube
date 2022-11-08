/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ThreadFlow {
  @SerializedName("locations")
  private final List<LocationWrapper> locations;

  private ThreadFlow(List<LocationWrapper> locations) {
    this.locations = locations;
  }

  public static ThreadFlow of(List<LocationWrapper> locations) {
    return new ThreadFlow(locations);
  }

  public List<LocationWrapper> getLocations() {
    return locations;
  }
}
