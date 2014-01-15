/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.rule;


public class RuleRuleTagDto {

  private Long id;
  private Integer ruleId;
  private Long tagId;
  private String tag;
  private String type;

  public Long getId() {
    return id;
  }

  public RuleRuleTagDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public RuleRuleTagDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public Long getTagId() {
    return tagId;
  }

  public RuleRuleTagDto setTagId(Long tagId) {
    this.tagId = tagId;
    return this;
  }

  public String getTag() {
    return tag;
  }

  public RuleRuleTagDto setTag(String tag) {
    this.tag = tag;
    return this;
  }

  public RuleTagType getType() {
    return RuleTagType.valueOf(type);
  }

  public RuleRuleTagDto setType(RuleTagType type) {
    this.type = type.name();
    return this;
  }

  @Override
  public String toString() {
    return String.format("RuleRuleTag[id=%d, ruleId=%d, tagId=%d, tag=%s, type=%s]",
        id, ruleId, tagId, tag, type);
  }
}
