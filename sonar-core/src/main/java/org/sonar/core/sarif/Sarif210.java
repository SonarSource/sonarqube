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

public class Sarif210 {

  public static final String SARIF_VERSION = "2.1.0";

  @SerializedName("version")
  private final String version;
  @SerializedName("$schema")
  private final String schema;
  @SerializedName("runs")
  private final Set<Run> runs;

  public Sarif210(String schema, Run run) {
    this.schema = schema;
    this.version = SARIF_VERSION;
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
