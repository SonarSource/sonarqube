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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Result {
  @SerializedName("ruleId")
  private final String ruleId;
  @SerializedName("message")
  private final WrappedText message;
  @SerializedName("locations")
  private final LinkedHashSet<Location> locations;
  @SerializedName("partialFingerprints")
  private final PartialFingerprints partialFingerprints;
  @SerializedName("codeFlows")
  private final List<CodeFlow> codeFlows;
  @SerializedName("level")
  private final String level;

  private Result(String ruleId, String message, @Nullable LinkedHashSet<Location> locations,
    @Nullable String primaryLocationLineHash, @Nullable List<CodeFlow> codeFlows, @Nullable String level) {
    this.ruleId = ruleId;
    this.message = WrappedText.of(message);
    this.locations = locations;
    this.partialFingerprints = primaryLocationLineHash == null ? null : new PartialFingerprints(primaryLocationLineHash);
    this.codeFlows = codeFlows == null ? null : List.copyOf(codeFlows);
    this.level = level;
  }

  public String getRuleId() {
    return ruleId;
  }

  public WrappedText getMessage() {
    return message;
  }

  @CheckForNull
  public Set<Location> getLocations() {
    return locations;
  }

  @CheckForNull
  public PartialFingerprints getPartialFingerprints() {
    return partialFingerprints;
  }

  @CheckForNull
  public List<CodeFlow> getCodeFlows() {
    return codeFlows;
  }

  public String getLevel() {
    return level;
  }

  public static ResultBuilder builder() {
    return new ResultBuilder();
  }

  public static final class ResultBuilder {
    private String ruleId;
    private String message;
    private LinkedHashSet<Location> locations;
    private String hash;
    private List<CodeFlow> codeFlows;
    private String level;

    private ResultBuilder() {
    }

    public ResultBuilder ruleId(String ruleId) {
      this.ruleId = ruleId;
      return this;
    }

    public ResultBuilder message(String message) {
      this.message = message;
      return this;
    }

    public ResultBuilder level(String level) {
      this.level = level;
      return this;
    }

    public ResultBuilder locations(Set<Location> locations) {
      this.locations = new LinkedHashSet<>(locations);
      return this;
    }

    public ResultBuilder hash(String hash) {
      this.hash = hash;
      return this;
    }

    public ResultBuilder codeFlows(List<CodeFlow> codeFlows) {
      this.codeFlows = codeFlows;
      return this;
    }

    public Result build() {
      return new Result(ruleId, message, locations, hash, codeFlows, level);
    }
  }
}
