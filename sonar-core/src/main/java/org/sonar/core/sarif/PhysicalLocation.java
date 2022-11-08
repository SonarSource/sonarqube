/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;

public class PhysicalLocation {
  @SerializedName("artifactLocation")
  private final ArtifactLocation artifactLocation;
  @SerializedName("region")
  private final Region region;

  private PhysicalLocation(ArtifactLocation artifactLocation, Region region) {
    this.artifactLocation = artifactLocation;
    this.region = region;
  }

  public static PhysicalLocation of(ArtifactLocation artifactLocation, Region region) {
    return new PhysicalLocation(artifactLocation, region);
  }

  public ArtifactLocation getArtifactLocation() {
    return artifactLocation;
  }

  public Region getRegion() {
    return region;
  }
}
