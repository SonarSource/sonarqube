/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Set;
import org.sonar.api.rule.RuleKey;

public class Result {
  @SerializedName("ruleId")
  private final String ruleId;
  @SerializedName("message")
  private final WrappedText message;
  @SerializedName("locations")
  private final Set<Location> locations;
  @SerializedName("partialFingerprints")
  private final PartialFingerprints partialFingerprints;
  @SerializedName("codeFlows")
  private final List<CodeFlow> codeFlows;


  private Result(RuleKey ruleKey, String message, Location location, String primaryLocationLineHash, List<CodeFlow> codeFlows) {
    this.ruleId = ruleKey.toString();
    this.message = WrappedText.of(message);
    this.locations = Set.of(location);
    this.partialFingerprints = new PartialFingerprints(primaryLocationLineHash);
    this.codeFlows = List.copyOf(codeFlows);
  }

  public String getRuleId() {
    return ruleId;
  }

  public WrappedText getMessage() {
    return message;
  }

  public Set<Location> getLocations() {
    return locations;
  }

  public PartialFingerprints getPartialFingerprints() {
    return partialFingerprints;
  }

  public List<CodeFlow> getCodeFlows() {
    return codeFlows;
  }

  public static ResultBuilder builder() {
    return new ResultBuilder();
  }

  public static final class ResultBuilder {
    private RuleKey ruleKey;
    private String message;
    private Location location;
    private String hash;
    private List<CodeFlow> codeFlows;

    private ResultBuilder() {
    }

    public ResultBuilder ruleKey(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public ResultBuilder message(String message) {
      this.message = message;
      return this;
    }

    public ResultBuilder locations(Location location) {
      this.location = location;
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
      return new Result(ruleKey, message, location, hash, codeFlows);
    }
  }
}
