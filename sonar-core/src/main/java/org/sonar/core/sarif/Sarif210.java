/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.annotations.SerializedName;
import java.util.Set;

public class Sarif210 {

  @VisibleForTesting
  public static final String SARIF_SCHEMA_URL = "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/master/Schemata/sarif-schema-2.1.0.json";
  @VisibleForTesting
  public static final String SARIF_VERSION = "2.1.0";

  @SerializedName("version")
  private final String version;
  @SerializedName("$schema")
  private final String schema;
  @SerializedName("runs")
  private final Set<Run> runs;

  public Sarif210(Run run) {
    this(SARIF_SCHEMA_URL, SARIF_VERSION, run);
  }

  private Sarif210(String schema, String version, Run run) {
    this.schema = schema;
    this.version = version;
    this.runs = Set.of(run);
  }

  public String getVersion() {
    return version;
  }

  public String getSchema() {
    return schema;
  }

  public Set<Run> getRuns() {
    return runs;
  }
}
