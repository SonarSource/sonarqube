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
package org.sonar.db.rule;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;
import static org.sonar.db.rule.RuleDescriptionSectionDto.DEFAULT_KEY;

public class RuleDto {

  static final String ERROR_MESSAGE_SECTION_ALREADY_EXISTS = "A section with key '%s' and context key '%s' already exists";

  public enum Format {
    HTML, MARKDOWN
  }

  public enum Scope {
    MAIN, TEST, ALL
  }

  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private String uuid = null;
  private String repositoryKey = null;
  private String ruleKey = null;

  private final Set<RuleDescriptionSectionDto> ruleDescriptionSectionDtos = new HashSet<>();
  private String educationPrinciplesField = null;

  /**
   * Description format can be null on external rule, otherwise it should never be null
   */
  private RuleDto.Format descriptionFormat = null;
  private RuleStatus status = null;
  private String name = null;
  private String configKey = null;

  /**
   * Severity can be null on external rule, otherwise it should never be null
   */
  private Integer severity = null;

  private boolean isTemplate = false;

  /**
   * This flag specify that this is an external rule, meaning that generated issues from this rule will be provided by the analyzer without being activated on a quality profile.
   */
  private boolean isExternal = false;

  /**
   * When an external rule is defined as ad hoc, it means that it's not defined using {@link org.sonar.api.server.rule.RulesDefinition.Context#createExternalRepository(String, String)}.
   * As the opposite, an external rule not being defined as ad hoc is declared by using {@link org.sonar.api.server.rule.RulesDefinition.Context#createExternalRepository(String, String)}.
   * This flag is only used for external rules (it can only be set to true for when {@link #isExternal()} is true)
   */
  private boolean isAdHoc = false;

  private String language = null;
  private String templateUuid = null;
  private String defRemediationFunction = null;
  private String defRemediationGapMultiplier = null;
  private String defRemediationBaseEffort = null;
  private String gapDescription = null;
  private String systemTagsField = null;
  private String securityStandardsField = null;
  private int type = 0;
  private Scope scope = null;

  private RuleKey key = null;

  private String pluginKey = null;
  private String noteData = null;
  private String noteUserUuid = null;
  private Long noteCreatedAt = null;
  private Long noteUpdatedAt = null;
  private String remediationFunction = null;
  private String remediationGapMultiplier = null;
  private String remediationBaseEffort = null;
  private String tags = null;

  /**
   * Name of on ad hoc rule.
   */
  private String adHocName = null;

  /**
   * Optional description of on ad hoc rule.
   */
  private String adHocDescription = null;

  /**
   * Severity of on ad hoc rule.
   * When {@link RuleDto#isAdHoc()} is true, this field should always be set
   */
  private String adHocSeverity = null;

  /**
   * Type of on ad hoc rule.
   * When {@link RuleDto#isAdHoc()} is true, this field should always be set
   */
  private Integer adHocType = null;
  private long createdAt = 0;
  private long updatedAt = 0;

  public RuleKey getKey() {
    if (key == null) {
      key = RuleKey.of(getRepositoryKey(), getRuleKey());
    }
    return key;
  }

  RuleDto setKey(RuleKey key) {
    this.key = key;
    setRepositoryKey(key.repository());
    setRuleKey(key.rule());
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public RuleDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public RuleDto setRepositoryKey(String repositoryKey) {
    checkArgument(repositoryKey.length() <= 255, "Rule repository is too long: %s", repositoryKey);
    this.repositoryKey = repositoryKey;
    return this;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public RuleDto setRuleKey(String ruleKey) {
    checkArgument(ruleKey.length() <= 200, "Rule key is too long: %s", ruleKey);
    this.ruleKey = ruleKey;
    return this;
  }

  public RuleDto setRuleKey(RuleKey ruleKey) {
    this.repositoryKey = ruleKey.repository();
    this.ruleKey = ruleKey.rule();
    this.key = ruleKey;
    return this;
  }

  @CheckForNull
  public String getPluginKey() {
    return pluginKey;
  }

  public RuleDto setPluginKey(@Nullable String pluginKey) {
    this.pluginKey = pluginKey;
    return this;
  }

  public Set<RuleDescriptionSectionDto> getRuleDescriptionSectionDtos() {
    return ruleDescriptionSectionDtos;
  }

  @CheckForNull
  public RuleDescriptionSectionDto getDefaultRuleDescriptionSection() {
    return ruleDescriptionSectionDtos.stream()
      .filter(ruleDesc -> ruleDesc.getKey().equals(DEFAULT_KEY))
      .findAny()
      .orElse(null);
  }

  public RuleDto replaceRuleDescriptionSectionDtos(Collection<RuleDescriptionSectionDto> ruleDescriptionSectionDtos) {
    this.ruleDescriptionSectionDtos.clear();
    ruleDescriptionSectionDtos.forEach(this::addRuleDescriptionSectionDto);
    return this;
  }

  public RuleDto addRuleDescriptionSectionDto(RuleDescriptionSectionDto ruleDescriptionSectionDto) {
    checkArgument(!hasDescriptionSectionWithSameKeyAndContext(ruleDescriptionSectionDto),
      ERROR_MESSAGE_SECTION_ALREADY_EXISTS, ruleDescriptionSectionDto.getKey(),
      Optional.ofNullable(ruleDescriptionSectionDto.getContext()).map(RuleDescriptionSectionContextDto::getKey).orElse(null));
    ruleDescriptionSectionDtos.add(ruleDescriptionSectionDto);
    return this;
  }

  private boolean hasDescriptionSectionWithSameKeyAndContext(RuleDescriptionSectionDto ruleDescriptionSectionDto) {
    return ruleDescriptionSectionDtos.stream()
      .anyMatch(ruleDesc -> hasSameKeyAndContextKey(ruleDescriptionSectionDto, ruleDesc));
  }

  private static boolean hasSameKeyAndContextKey(RuleDescriptionSectionDto ruleDescriptionSectionDto, RuleDescriptionSectionDto other) {
    if (!ruleDescriptionSectionDto.getKey().equals(other.getKey())) {
      return false;
    }

    String contextKey = ofNullable(ruleDescriptionSectionDto.getContext()).map(RuleDescriptionSectionContextDto::getKey).orElse(null);
    String otherContextKey = ofNullable(other.getContext()).map(RuleDescriptionSectionContextDto::getKey).orElse(null);
    return Objects.equals(contextKey, otherContextKey);
  }

  public Set<String> getEducationPrinciples() {
    return deserializeStringSet(educationPrinciplesField);
  }

  public RuleDto setEducationPrinciples(Set<String> educationPrinciples){
    this.educationPrinciplesField = serializeStringSet(educationPrinciples);
    return this;
  }

  @CheckForNull
  public Format getDescriptionFormat() {
    return descriptionFormat;
  }

  public RuleDto setDescriptionFormat(Format descriptionFormat) {
    this.descriptionFormat = descriptionFormat;
    return this;
  }

  public RuleStatus getStatus() {
    return status;
  }

  public RuleDto setStatus(@Nullable RuleStatus status) {
    this.status = status;
    return this;
  }

  public String getName() {
    return name;
  }

  public RuleDto setName(@Nullable String name) {
    checkArgument(name == null || name.length() <= 255, "Rule name is too long: %s", name);
    this.name = name;
    return this;
  }

  public String getConfigKey() {
    return configKey;
  }

  public RuleDto setConfigKey(@Nullable String configKey) {
    this.configKey = configKey;
    return this;
  }

  public Scope getScope() {
    return scope;
  }

  public RuleDto setScope(Scope scope) {
    this.scope = scope;
    return this;
  }

  @CheckForNull
  public Integer getSeverity() {
    return severity;
  }

  @CheckForNull
  public String getSeverityString() {
    return severity != null ? SeverityUtil.getSeverityFromOrdinal(severity) : null;
  }

  public RuleDto setSeverity(@Nullable String severity) {
    return this.setSeverity(severity != null ? SeverityUtil.getOrdinalFromSeverity(severity) : null);
  }

  public RuleDto setSeverity(@Nullable Integer severity) {
    this.severity = severity;
    return this;
  }

  public boolean isExternal() {
    return isExternal;
  }

  public RuleDto setIsExternal(boolean isExternal) {
    this.isExternal = isExternal;
    return this;
  }

  public boolean isAdHoc() {
    return isAdHoc;
  }

  public RuleDto setIsAdHoc(boolean isAdHoc) {
    this.isAdHoc = isAdHoc;
    return this;
  }

  public boolean isTemplate() {
    return isTemplate;
  }

  public RuleDto setIsTemplate(boolean isTemplate) {
    this.isTemplate = isTemplate;
    return this;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  public RuleDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  @CheckForNull
  public String getTemplateUuid() {
    return templateUuid;
  }

  public RuleDto setTemplateUuid(@Nullable String templateUuid) {
    this.templateUuid = templateUuid;
    return this;
  }

  public boolean isCustomRule() {
    return getTemplateUuid() != null;
  }

  public RuleDto setSystemTags(Set<String> tags) {
    this.systemTagsField = serializeStringSet(tags);
    return this;
  }

  public RuleDto setSecurityStandards(Set<String> standards) {
    this.securityStandardsField = serializeStringSet(standards);
    return this;
  }

  public Set<String> getSystemTags() {
    return deserializeTagsString(systemTagsField);
  }

  public Set<String> getSecurityStandards() {
    return deserializeSecurityStandardsString(securityStandardsField);
  }

  public int getType() {
    return type;
  }

  public RuleDto setType(int type) {
    this.type = type;
    return this;
  }

  public RuleDto setType(RuleType type) {
    this.type = type.getDbConstant();
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public RuleDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public RuleDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }


  @CheckForNull
  public String getDefRemediationFunction() {
    return defRemediationFunction;
  }

  public RuleDto setDefRemediationFunction(@Nullable String defaultRemediationFunction) {
    this.defRemediationFunction = defaultRemediationFunction;
    return this;
  }

  @CheckForNull
  public String getDefRemediationGapMultiplier() {
    return defRemediationGapMultiplier;
  }

  public RuleDto setDefRemediationGapMultiplier(@Nullable String defaultRemediationGapMultiplier) {
    this.defRemediationGapMultiplier = defaultRemediationGapMultiplier;
    return this;
  }

  @CheckForNull
  public String getDefRemediationBaseEffort() {
    return defRemediationBaseEffort;
  }

  public RuleDto setDefRemediationBaseEffort(@Nullable String defaultRemediationBaseEffort) {
    this.defRemediationBaseEffort = defaultRemediationBaseEffort;
    return this;
  }

  @CheckForNull
  public String getGapDescription() {
    return gapDescription;
  }

  public RuleDto setGapDescription(@Nullable String s) {
    this.gapDescription = s;
    return this;
  }

  public static Set<String> deserializeTagsString(@Nullable String tags) {
    return deserializeStringSet(tags);
  }

  public static Set<String> deserializeSecurityStandardsString(@Nullable String securityStandards) {
    return deserializeStringSet(securityStandards);
  }

  private static Set<String> deserializeStringSet(@Nullable String str) {
    if (str == null || str.isEmpty()) {
      return emptySet();
    }

    return ImmutableSet.copyOf(SPLITTER.split(str));
  }

  @CheckForNull
  public String getNoteData() {
    return noteData;
  }

  public RuleDto setNoteData(@Nullable String s) {
    this.noteData = s;
    return this;
  }

  @CheckForNull
  public String getNoteUserUuid() {
    return noteUserUuid;
  }

  public RuleDto setNoteUserUuid(@Nullable String noteUserUuid) {
    this.noteUserUuid = noteUserUuid;
    return this;
  }

  @CheckForNull
  public Long getNoteCreatedAt() {
    return noteCreatedAt;
  }

  public RuleDto setNoteCreatedAt(@Nullable Long noteCreatedAt) {
    this.noteCreatedAt = noteCreatedAt;
    return this;
  }

  @CheckForNull
  public Long getNoteUpdatedAt() {
    return noteUpdatedAt;
  }

  public RuleDto setNoteUpdatedAt(@Nullable Long noteUpdatedAt) {
    this.noteUpdatedAt = noteUpdatedAt;
    return this;
  }

  @CheckForNull
  public String getRemediationFunction() {
    return remediationFunction;
  }

  public RuleDto setRemediationFunction(@Nullable String remediationFunction) {
    this.remediationFunction = remediationFunction;
    return this;
  }

  @CheckForNull
  public String getRemediationGapMultiplier() {
    return remediationGapMultiplier;
  }

  public RuleDto setRemediationGapMultiplier(@Nullable String remediationGapMultiplier) {
    this.remediationGapMultiplier = remediationGapMultiplier;
    return this;
  }

  @CheckForNull
  public String getRemediationBaseEffort() {
    return remediationBaseEffort;
  }

  public RuleDto setRemediationBaseEffort(@Nullable String remediationBaseEffort) {
    this.remediationBaseEffort = remediationBaseEffort;
    return this;
  }

  public Set<String> getTags() {
    return tags == null ? new HashSet<>() : new TreeSet<>(asList(StringUtils.split(tags, ',')));
  }

  String getTagsAsString() {
    return tags;
  }

  public RuleDto setTags(Set<String> tags) {
    String raw = tags.isEmpty() ? null : String.join(",", tags);
    checkArgument(raw == null || raw.length() <= 4000, "Rule tags are too long: %s", raw);
    this.tags = raw;
    return this;
  }

  private String getTagsField() {
    return tags;
  }

  void setTagsField(String s) {
    tags = s;
  }

  @CheckForNull
  public String getAdHocName() {
    return adHocName;
  }

  public RuleDto setAdHocName(@Nullable String adHocName) {
    this.adHocName = adHocName;
    return this;
  }

  @CheckForNull
  public String getAdHocDescription() {
    return adHocDescription;
  }

  public RuleDto setAdHocDescription(@Nullable String adHocDescription) {
    this.adHocDescription = adHocDescription;
    return this;
  }

  @CheckForNull
  public String getAdHocSeverity() {
    return adHocSeverity;
  }

  public RuleDto setAdHocSeverity(@Nullable String adHocSeverity) {
    this.adHocSeverity = adHocSeverity;
    return this;
  }

  @CheckForNull
  public Integer getAdHocType() {
    return adHocType;
  }

  public RuleDto setAdHocType(@Nullable Integer adHocType) {
    this.adHocType = adHocType;
    return this;
  }

  public RuleDto setAdHocType(@Nullable RuleType adHocType) {
    setAdHocType(adHocType != null ? adHocType.getDbConstant() : null);
    return this;
  }

  private static String serializeStringSet(@Nullable Set<String> strings) {
    return strings == null || strings.isEmpty() ? null : String.join(",", strings);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RuleDto)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RuleDto other = (RuleDto) obj;
    return Objects.equals(this.uuid, other.uuid);
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
      .append(this.uuid)
      .toHashCode();
  }

}
