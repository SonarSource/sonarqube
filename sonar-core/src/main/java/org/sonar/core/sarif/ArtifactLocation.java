/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;

public class ArtifactLocation {
  private static final String URI_BASE_ID = "%SRCROOT";

  @SerializedName("uri")
  private final String uri;
  @SerializedName("uriBaseId")
  private final String uriBaseId;

  private ArtifactLocation(String uriBaseId, String uri) {
    this.uriBaseId = uriBaseId;
    this.uri = uri;
  }

  public static ArtifactLocation of(String uri) {
    return new ArtifactLocation(URI_BASE_ID, uri);
  }

  public String getUri() {
    return uri;
  }

  public String getUriBaseId() {
    return uriBaseId;
  }
}
