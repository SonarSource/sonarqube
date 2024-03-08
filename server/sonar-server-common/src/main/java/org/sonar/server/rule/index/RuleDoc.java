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
package org.sonar.server.rule.index;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDescriptionSectionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleForIndexingDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.es.BaseDoc;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.SQCategory;

import static java.util.stream.Collectors.joining;
import static org.sonar.server.rule.index.RuleIndexDefinition.SUB_FIELD_SEVERITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.SUB_FIELD_SOFTWARE_QUALITY;
import static org.sonar.server.rule.index.RuleIndexDefinition.TYPE_RULE;

/**
 * Implementation of Rule based on an Elasticsearch document
 */
public class RuleDoc extends BaseDoc {

  @VisibleForTesting
  public RuleDoc(Map<String, Object> fields) {
    super(TYPE_RULE, new HashMap<>(fields));
  }

  public RuleDoc() {
    super(TYPE_RULE, Maps.newHashMapWithExpectedSize(16));
  }

  @Override
  public String getId() {
    return idAsString();
  }

  private String idAsString() {
    return getField(RuleIndexDefinition.FIELD_RULE_UUID);
  }

  public RuleDoc setUuid(String ruleUuid) {
    setField(RuleIndexDefinition.FIELD_RULE_UUID, ruleUuid);
    return this;
  }

  @Override
  protected Optional<String> getSimpleMainTypeRouting() {
    return Optional.of(idAsString());
  }

  public RuleKey key() {
    return RuleKey.parse(keyAsString());
  }

  private String keyAsString() {
    return getField(RuleIndexDefinition.FIELD_RULE_KEY);
  }

  public RuleDoc setKey(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_KEY, s);
    return this;
  }

  @CheckForNull
  public String ruleKey() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_RULE_KEY);
  }

  public RuleDoc setRuleKey(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_RULE_KEY, s);
    return this;
  }

  @CheckForNull
  public String repository() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_REPOSITORY);
  }

  public RuleDoc setRepository(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_REPOSITORY, s);
    return this;
  }

  @CheckForNull
  public String internalKey() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_INTERNAL_KEY);
  }

  public RuleDoc setInternalKey(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_INTERNAL_KEY, s);
    return this;
  }

  @CheckForNull
  public String language() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_LANGUAGE);
  }

  public RuleDoc setLanguage(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_LANGUAGE, s);
    return this;
  }

  public String name() {
    return getField(RuleIndexDefinition.FIELD_RULE_NAME);
  }

  public RuleDoc setName(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_NAME, s);
    return this;
  }

  @CheckForNull
  public String htmlDescription() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_HTML_DESCRIPTION);
  }

  public RuleDoc setHtmlDescription(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_HTML_DESCRIPTION, s);
    return this;
  }

  @CheckForNull
  public String severity() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_SEVERITY);
  }

  public RuleDoc setSeverity(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_SEVERITY, s);
    return this;
  }

  @CheckForNull
  public Collection<String> getOwaspTop10() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_OWASP_TOP_10);
  }

  public RuleDoc setOwaspTop10(@Nullable Collection<String> o) {
    setField(RuleIndexDefinition.FIELD_RULE_OWASP_TOP_10, o);
    return this;
  }

  @CheckForNull
  public Collection<String> getOwaspTop10For2021() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_OWASP_TOP_10_2021);
  }

  public RuleDoc setOwaspTop10For2021(@Nullable Collection<String> o) {
    setField(RuleIndexDefinition.FIELD_RULE_OWASP_TOP_10_2021, o);
    return this;
  }

  @CheckForNull
  public Collection<String> getSansTop25() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_SANS_TOP_25);
  }

  public RuleDoc setSansTop25(@Nullable Collection<String> s) {
    setField(RuleIndexDefinition.FIELD_RULE_SANS_TOP_25, s);
    return this;
  }

  @CheckForNull
  public Collection<String> getCwe() {
    return getNullableField(RuleIndexDefinition.FIELD_RULE_CWE);
  }

  public RuleDoc setCwe(@Nullable Collection<String> c) {
    setField(RuleIndexDefinition.FIELD_RULE_CWE, c);
    return this;
  }

  @CheckForNull
  public SQCategory getSonarSourceSecurityCategory() {
    String key = getNullableField(RuleIndexDefinition.FIELD_RULE_SONARSOURCE_SECURITY);
    return SQCategory.fromKey(key).orElse(null);
  }

  public RuleDoc setSonarSourceSecurityCategory(@Nullable SQCategory sqCategory) {
    setField(RuleIndexDefinition.FIELD_RULE_SONARSOURCE_SECURITY, sqCategory == null ? null : sqCategory.getKey());
    return this;
  }

  public Set<String> getTags() {
    return getField(RuleIndexDefinition.FIELD_RULE_TAGS);
  }

  public RuleDoc setTags(Set<String> tags) {
    setField(RuleIndexDefinition.FIELD_RULE_TAGS, tags);
    return this;
  }

  @CheckForNull
  public RuleStatus status() {
    return RuleStatus.valueOf(getField(RuleIndexDefinition.FIELD_RULE_STATUS));
  }

  public RuleDoc setStatus(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_STATUS, s);
    return this;
  }

  @CheckForNull
  public RuleKey templateKey() {
    String templateKey = getNullableField(RuleIndexDefinition.FIELD_RULE_TEMPLATE_KEY);
    return templateKey != null ? RuleKey.parse(templateKey) : null;
  }

  public RuleDoc setTemplateKey(@Nullable String s) {
    setField(RuleIndexDefinition.FIELD_RULE_TEMPLATE_KEY, s);
    return this;
  }

  public boolean isTemplate() {
    return getField(RuleIndexDefinition.FIELD_RULE_IS_TEMPLATE);
  }

  public RuleDoc setIsTemplate(@Nullable Boolean b) {
    setField(RuleIndexDefinition.FIELD_RULE_IS_TEMPLATE, b);
    return this;
  }

  public boolean isExternal() {
    return getField(RuleIndexDefinition.FIELD_RULE_IS_EXTERNAL);
  }

  public RuleDoc setIsExternal(boolean b) {
    setField(RuleIndexDefinition.FIELD_RULE_IS_EXTERNAL, b);
    return this;
  }

  @CheckForNull
  public RuleType type() {
    String type = getNullableField(RuleIndexDefinition.FIELD_RULE_TYPE);
    if (type == null) {
      return null;
    }
    return RuleType.valueOf(type);
  }

  public RuleDoc setType(@Nullable RuleType ruleType) {
    setField(RuleIndexDefinition.FIELD_RULE_TYPE, ruleType == null ? null : ruleType.name());
    return this;
  }

  public long createdAt() {
    return getField(RuleIndexDefinition.FIELD_RULE_CREATED_AT);
  }

  public RuleDoc setCreatedAt(@Nullable Long l) {
    setField(RuleIndexDefinition.FIELD_RULE_CREATED_AT, l);
    return this;
  }

  public long updatedAt() {
    return getField(RuleIndexDefinition.FIELD_RULE_UPDATED_AT);
  }

  public RuleDoc setUpdatedAt(@Nullable Long l) {
    setField(RuleIndexDefinition.FIELD_RULE_UPDATED_AT, l);
    return this;
  }

  public RuleDoc setCleanCodeAttributeCategory(@Nullable String cleanCodeAttributeCategory) {
    setField(RuleIndexDefinition.FIELD_RULE_CLEAN_CODE_ATTRIBUTE_CATEGORY, cleanCodeAttributeCategory);
    return this;
  }

  public RuleDoc setImpacts(Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts) {
    List<Map<String, String>> convertedMap = impacts
      .entrySet()
      .stream()
      .map(entry -> Map.of(
        SUB_FIELD_SOFTWARE_QUALITY, entry.getKey().name(),
        SUB_FIELD_SEVERITY, entry.getValue().name()))
      .toList();
    setField(RuleIndexDefinition.FIELD_RULE_IMPACTS, convertedMap);
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  public static RuleDoc createFrom(RuleForIndexingDto dto, SecurityStandards securityStandards) {
    return new RuleDoc()
      .setUuid(dto.getUuid())
      .setKey(dto.getRuleKey().toString())
      .setRepository(dto.getRepository())
      .setInternalKey(dto.getInternalKey())
      .setIsTemplate(dto.isTemplate())
      .setIsExternal(dto.isExternal())
      .setLanguage(dto.getLanguage())
      .setCwe(securityStandards.getCwe())
      .setOwaspTop10(securityStandards.getOwaspTop10())
      .setOwaspTop10For2021(securityStandards.getOwaspTop10For2021())
      .setSansTop25(securityStandards.getSansTop25())
      .setSonarSourceSecurityCategory(securityStandards.getSqCategory())
      .setName(dto.getName())
      .setRuleKey(dto.getPluginRuleKey())
      .setSeverity(dto.getSeverityAsString())
      .setStatus(dto.getStatus().toString())
      .setType(getType(dto))
      .setCreatedAt(dto.getCreatedAt())
      .setTags(Sets.union(dto.getTags(), dto.getSystemTags()))
      .setUpdatedAt(dto.getUpdatedAt())
      .setHtmlDescription(getConcatenatedSectionsInHtml(dto))
      .setTemplateKey(getRuleKey(dto))
      .setCleanCodeAttributeCategory(dto.getTypeAsRuleType() != RuleType.SECURITY_HOTSPOT ? dto.getCleanCodeAttributeCategory() : null)
      .setImpacts(dto.getImpacts().stream().collect(Collectors.toMap(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)));
  }

  @CheckForNull
  private static RuleType getType(RuleForIndexingDto dto) {
    if (dto.isAdHoc() && dto.getAdHocType() != null) {
      return RuleType.valueOf(dto.getAdHocType());
    }
    return dto.getTypeAsRuleType();
  }

  @CheckForNull
  private static String getRuleKey(RuleForIndexingDto dto) {
    if (dto.getTemplateRuleKey() != null && dto.getTemplateRepository() != null) {
      return RuleKey.of(dto.getTemplateRepository(), dto.getTemplateRuleKey()).toString();
    }
    return null;
  }

  private static String getConcatenatedSectionsInHtml(RuleForIndexingDto dto) {
    return dto.getRuleDescriptionSectionsDtos().stream()
      .map(RuleDescriptionSectionDto::getContent)
      .map(content -> convertToHtmlIfNecessary(dto.getDescriptionFormat(), content))
      .collect(joining(" "));
  }

  private static String convertToHtmlIfNecessary(RuleDto.Format format, String content) {
    if (RuleDto.Format.MARKDOWN.equals(format)) {
      return Markdown.convertToHtml(content);
    }
    return content;
  }
}
