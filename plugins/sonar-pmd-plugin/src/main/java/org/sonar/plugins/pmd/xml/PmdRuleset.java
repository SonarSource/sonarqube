/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.pmd.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.util.ArrayList;
import java.util.List;

@XStreamAlias("ruleset")
public class PmdRuleset {

  private String description;

  @XStreamImplicit
  private List<PmdRule> rules = new ArrayList<PmdRule>();

  @XStreamOmitField
  @XStreamAlias(value = "exclude-pattern")
  private String excludePattern;//NOSONAR unused private field

  @XStreamOmitField
  @XStreamAlias(value = "include-pattern")
  private String includePattern;//NOSONAR unused private field

  public PmdRuleset() {
  }

  public PmdRuleset(String description) {
    this.description = description;
  }

  public List<PmdRule> getPmdRules() {
    return rules;
  }

  public void setRules(List<PmdRule> rules) {
    this.rules = rules;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void addRule(PmdRule rule) {
    rules.add(rule);
  }

}
