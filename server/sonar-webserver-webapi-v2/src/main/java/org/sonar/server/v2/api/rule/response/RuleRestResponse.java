/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.api.rule.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.server.v2.api.rule.enums.CleanCodeAttributeCategoryRestEnum;
import org.sonar.server.v2.api.rule.enums.CleanCodeAttributeRestEnum;
import org.sonar.server.v2.api.rule.enums.RuleStatusRestEnum;
import org.sonar.server.v2.api.rule.enums.RuleTypeRestEnum;
import org.sonar.server.v2.api.rule.ressource.Parameter;

@Schema(accessMode = Schema.AccessMode.READ_ONLY)
public record RuleRestResponse(
  String id,
  String key,
  String repositoryKey,
  String name,
  @Nullable
  String severity,
  RuleTypeRestEnum type,
  List<RuleImpactRestResponse> impacts,
  @Nullable
  CleanCodeAttributeRestEnum cleanCodeAttribute,
  @Nullable
  CleanCodeAttributeCategoryRestEnum cleanCodeAttributeCategory,
  @Nullable
  RuleStatusRestEnum status,
  boolean external,
  @Nullable
  String createdAt,
  List<RuleDescriptionSectionRestResponse> descriptionSections,
  @Schema(accessMode = Schema.AccessMode.READ_WRITE)
  String markdownDescription,
  @Nullable
  String gapDescription,
  @Nullable
  String htmlNote,
  @Nullable
  String markdownNote,
  List<String> educationPrinciples,
  boolean template,
  @Nullable
  String templateId,
  @Schema(accessMode = Schema.AccessMode.READ_WRITE)
  List<String> tags,
  List<String> systemTags,
  @Nullable
  String languageKey,
  @Nullable
  String languageName,
  List<Parameter> parameters,
  String remediationFunctionType,
  String remediationFunctionGapMultiplier,
  String remediationFunctionBaseEffort
) {


  public static final class Builder {
    private String id;
    private String key;
    private String repositoryKey;
    private String name;
    private String severity;
    private RuleTypeRestEnum type;
    private List<RuleImpactRestResponse> impacts;
    private CleanCodeAttributeRestEnum cleanCodeAttribute;
    private CleanCodeAttributeCategoryRestEnum cleanCodeAttributeCategory;
    private RuleStatusRestEnum status;
    private boolean external;
    private String createdAt;
    private List<RuleDescriptionSectionRestResponse> descriptionSections;
    private String markdownDescription;
    private String gapDescription;
    private String htmlNote;
    private String markdownNote;
    private List<String> educationPrinciples;
    private boolean template;
    private String templateId;
    private List<String> tags;
    private List<String> systemTags;
    private String languageKey;
    private String languageName;
    private List<Parameter> parameters;
    private String remediationFunctionType;
    private String remediationFunctionGapMultiplier;
    private String remediationFunctionBaseEffort;

    private Builder() {
    }

    public static Builder builder() {
      return new Builder();
    }

    public Builder setId(String id) {
      this.id = id;
      return this;
    }

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setRepositoryKey(String repositoryKey) {
      this.repositoryKey = repositoryKey;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setSeverity(@Nullable String severity) {
      this.severity = severity;
      return this;
    }

    public Builder setType(RuleTypeRestEnum type) {
      this.type = type;
      return this;
    }

    public Builder setImpacts(List<RuleImpactRestResponse> impacts) {
      this.impacts = impacts;
      return this;
    }

    public Builder setCleanCodeAttribute(@Nullable CleanCodeAttributeRestEnum cleanCodeAttribute) {
      this.cleanCodeAttribute = cleanCodeAttribute;
      return this;
    }

    public Builder setCleanCodeAttributeCategory(@Nullable CleanCodeAttributeCategoryRestEnum cleanCodeAttributeCategory) {
      this.cleanCodeAttributeCategory = cleanCodeAttributeCategory;
      return this;
    }

    public Builder setStatus(RuleStatusRestEnum status) {
      this.status = status;
      return this;
    }

    public Builder setExternal(boolean external) {
      this.external = external;
      return this;
    }

    public Builder setCreatedAt(@Nullable String createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder setDescriptionSections(List<RuleDescriptionSectionRestResponse> descriptionSections) {
      this.descriptionSections = descriptionSections;
      return this;
    }

    public Builder setMarkdownDescription(String markdownDescription) {
      this.markdownDescription = markdownDescription;
      return this;
    }

    public Builder setGapDescription(@Nullable String gapDescription) {
      this.gapDescription = gapDescription;
      return this;
    }

    public Builder setHtmlNote(@Nullable String htmlNote) {
      this.htmlNote = htmlNote;
      return this;
    }

    public Builder setMarkdownNote(@Nullable String markdownNote) {
      this.markdownNote = markdownNote;
      return this;
    }

    public Builder setEducationPrinciples(List<String> educationPrinciples) {
      this.educationPrinciples = educationPrinciples;
      return this;
    }

    public Builder setTemplate(boolean template) {
      this.template = template;
      return this;
    }

    public Builder setTemplateId(@Nullable String templateId) {
      this.templateId = templateId;
      return this;
    }

    public Builder setTags(List<String> tags) {
      this.tags = tags;
      return this;
    }

    public Builder setSystemTags(List<String> systemTags) {
      this.systemTags = systemTags;
      return this;
    }

    public Builder setLanguageKey(@Nullable String languageKey) {
      this.languageKey = languageKey;
      return this;
    }

    public Builder setLanguageName(@Nullable String languageName) {
      this.languageName = languageName;
      return this;
    }

    public Builder setParameters(List<Parameter> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder setRemediationFunctionType(@Nullable String remediationFunctionType) {
      this.remediationFunctionType = remediationFunctionType;
      return this;
    }

    public Builder setRemediationFunctionGapMultiplier(@Nullable String remediationFunctionGapMultiplier) {
      this.remediationFunctionGapMultiplier = remediationFunctionGapMultiplier;
      return this;
    }

    public Builder setRemediationFunctionBaseEffort(@Nullable String remediationFunctionBaseEffort) {
      this.remediationFunctionBaseEffort = remediationFunctionBaseEffort;
      return this;
    }

    public RuleRestResponse build() {
      return new RuleRestResponse(id, key, repositoryKey, name, severity, type, impacts, cleanCodeAttribute, cleanCodeAttributeCategory,
        status, external, createdAt, descriptionSections, markdownDescription, gapDescription, htmlNote, markdownNote,
        educationPrinciples, template, templateId, tags, systemTags, languageKey, languageName, parameters, remediationFunctionType,
        remediationFunctionGapMultiplier, remediationFunctionBaseEffort);
    }
  }
}
