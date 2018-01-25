/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleForIndexingDto;
import org.sonar.markdown.Markdown;
import org.sonar.server.es.BaseDoc;

/**
 * Implementation of Rule based on an Elasticsearch document
 */
public class RuleDoc extends BaseDoc {

  public RuleDoc(Map<String, Object> fields) {
    super(fields);
  }

  public RuleDoc() {
    super(Maps.newHashMapWithExpectedSize(15));
  }

  @Override
  public String getId() {
    return idAsString();
  }

  private String idAsString() {
    return getField(RuleIndexDefinition.FIELD_RULE_ID);
  }

  public RuleDoc setId(int ruleId) {
    setField(RuleIndexDefinition.FIELD_RULE_ID, String.valueOf(ruleId));
    return this;
  }

  @Override
  public String getRouting() {
    return idAsString();
  }

  @Override
  public String getParent() {
    return null;
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

  public RuleType type() {
    return RuleType.valueOf(getField(RuleIndexDefinition.FIELD_RULE_TYPE));
  }

  public RuleDoc setType(RuleType ruleType) {
    setField(RuleIndexDefinition.FIELD_RULE_TYPE, ruleType.name());
    return this;
  }

  public long createdAt() {
    return (Long) getField(RuleIndexDefinition.FIELD_RULE_CREATED_AT);
  }

  public RuleDoc setCreatedAt(@Nullable Long l) {
    setField(RuleIndexDefinition.FIELD_RULE_CREATED_AT, l);
    return this;
  }

  public long updatedAt() {
    return (Long) getField(RuleIndexDefinition.FIELD_RULE_UPDATED_AT);
  }

  public RuleDoc setUpdatedAt(@Nullable Long l) {
    setField(RuleIndexDefinition.FIELD_RULE_UPDATED_AT, l);
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  public static RuleDoc of(RuleForIndexingDto dto) {
    RuleDoc ruleDoc = new RuleDoc()
      .setId(dto.getId())
      .setKey(dto.getRuleKey().toString())
      .setRepository(dto.getRepository())
      .setInternalKey(dto.getInternalKey())
      .setIsTemplate(dto.isTemplate())
      .setLanguage(dto.getLanguage())
      .setName(dto.getName())
      .setRuleKey(dto.getPluginRuleKey())
      .setSeverity(dto.getSeverityAsString())
      .setStatus(dto.getStatus().toString())
      .setType(dto.getTypeAsRuleType())
      .setCreatedAt(dto.getCreatedAt())
      .setUpdatedAt(dto.getUpdatedAt());

    if (dto.getTemplateRuleKey() != null && dto.getTemplateRepository() != null) {
      ruleDoc.setTemplateKey(RuleKey.of(dto.getTemplateRepository(), dto.getTemplateRuleKey()).toString());
    } else {
      ruleDoc.setTemplateKey(null);
    }

    if (dto.getDescription() != null && dto.getDescriptionFormat() != null) {
      if (RuleDto.Format.HTML == dto.getDescriptionFormat()) {
        ruleDoc.setHtmlDescription(dto.getDescription());
      } else {
        ruleDoc.setHtmlDescription(Markdown.convertToHtml(dto.getDescription()));
      }
    }
    return ruleDoc;
  }
}
