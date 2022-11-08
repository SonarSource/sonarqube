/*
 * Copyright (C) 2017-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package org.sonar.core.sarif;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;
import org.sonar.api.rule.RuleKey;

public class Rule {
  @SerializedName("id")
  private final String id;
  @SerializedName("name")
  private final String name;
  @SerializedName("shortDescription")
  private final WrappedText shortDescription;
  @SerializedName("fullDescription")
  private final WrappedText fullDescription;
  @SerializedName("help")
  private final WrappedText help;
  @SerializedName("properties")
  private final PropertiesBag properties;

  public Rule(RuleKey ruleKey, String ruleName, String ruleDescription, PropertiesBag properties) {
    id = ruleKey.toString();
    name = ruleKey.toString();
    shortDescription = WrappedText.of(ruleName);
    fullDescription = WrappedText.of(ruleName);
    help = WrappedText.of(ruleDescription);
    this.properties = properties;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public WrappedText getShortDescription() {
    return shortDescription;
  }

  public WrappedText getFullDescription() {
    return fullDescription;
  }

  public WrappedText getHelp() {
    return help;
  }

  public PropertiesBag getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Rule rule = (Rule) o;
    return Objects.equals(id, rule.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

}
