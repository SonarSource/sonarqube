/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.db.qualityprofile;

import com.google.common.base.Preconditions;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.core.rule.SeverityUtil;
import org.sonar.db.Dto;
import org.sonar.db.rule.RuleDto;

public class ActiveRuleDto extends Dto<ActiveRuleKey> {

  public static final String INHERITED = ActiveRule.INHERITED;
  public static final String OVERRIDES = ActiveRule.OVERRIDES;

  private String repository;
  private String ruleField;
  private String profileKey;

  private Integer id;
  private Integer profileId;
  private Integer ruleId;
  private Integer severity;
  private String inheritance;

  /**
   * @deprecated for internal use, should be private
   */
  @Deprecated
  public ActiveRuleDto setKey(ActiveRuleKey key) {
    this.repository = key.ruleKey().repository();
    this.ruleField = key.ruleKey().rule();
    this.profileKey = key.qProfile();
    return this;
  }

  @Override
  public ActiveRuleKey getKey() {
    return ActiveRuleKey.of(profileKey, RuleKey.of(repository, ruleField));
  }

  // This field do not exists in db, it's only retrieve by joins
  private Integer parentId;

  public Integer getId() {
    return id;
  }

  public ActiveRuleDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getProfileId() {
    return profileId;
  }

  // TODO mark as private
  public ActiveRuleDto setProfileId(Integer profileId) {
    this.profileId = profileId;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  // TODO mark as private
  public ActiveRuleDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public Integer getSeverity() {
    return severity;
  }

  public String getSeverityString() {
    return SeverityUtil.getSeverityFromOrdinal(severity);
  }

  public ActiveRuleDto setSeverity(Integer severity) {
    this.severity = severity;
    return this;
  }

  public ActiveRuleDto setSeverity(String severity) {
    this.severity = SeverityUtil.getOrdinalFromSeverity(severity);
    return this;
  }

  @CheckForNull
  public String getInheritance() {
    return inheritance;
  }

  public ActiveRuleDto setInheritance(@Nullable String inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  @CheckForNull
  public Integer getParentId() {
    return parentId;
  }

  public ActiveRuleDto setParentId(@Nullable Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  public boolean isInherited() {
    return StringUtils.equals(INHERITED, inheritance);
  }

  public boolean doesOverride() {
    return StringUtils.equals(OVERRIDES, inheritance);
  }

  public static ActiveRuleDto createFor(QualityProfileDto profileDto, RuleDto ruleDto) {
    Preconditions.checkNotNull(profileDto.getId(), "Profile is not persisted");
    Preconditions.checkNotNull(ruleDto.getId(), "Rule is not persisted");
    ActiveRuleDto dto = new ActiveRuleDto();
    dto.setProfileId(profileDto.getId());
    dto.setRuleId(ruleDto.getId());
    dto.setKey(ActiveRuleKey.of(profileDto.getKee(), ruleDto.getKey()));
    return dto;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

}
