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
import java.util.Objects;

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
  @SerializedName("defaultConfiguration")
  private DefaultConfiguration defaultConfiguration;

  private Rule(String id, String name, WrappedText shortDescription, WrappedText fullDescription, WrappedText help, PropertiesBag properties) {
    this.id = id;
    this.name = name;
    this.shortDescription = shortDescription;
    this.fullDescription = fullDescription;
    this.help = help;
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

  public DefaultConfiguration getDefaultConfiguration() {
    return defaultConfiguration;
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

  public static RuleBuilder builder() {
    return new RuleBuilder();
  }

  public static final class RuleBuilder {
    private String id;
    private String name;
    private WrappedText shortDescription;
    private WrappedText fullDescription;
    private WrappedText help;
    private PropertiesBag properties;

    private RuleBuilder() {
    }

    public RuleBuilder id(String id) {
      this.id = id;
      return this;
    }

    public RuleBuilder name(String name) {
      this.name = name;
      return this;
    }

    public RuleBuilder shortDescription(String shortDescription) {
      this.shortDescription = WrappedText.of(shortDescription);
      return this;
    }

    public RuleBuilder fullDescription(String fullDescription) {
      this.fullDescription = WrappedText.of(fullDescription);
      return this;
    }

    public RuleBuilder help(String help) {
      this.help = WrappedText.of(help);
      return this;
    }

    public RuleBuilder properties(PropertiesBag properties) {
      this.properties = properties;
      return this;
    }

    public Rule build() {
      return new Rule(id, name, shortDescription, fullDescription, help, properties);
    }
  }
}
