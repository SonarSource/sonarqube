/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import java.util.Date;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto.Format;
import org.sonar.db.rule.RuleDto.Scope;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.sonar.api.rule.RuleKey.EXTERNAL_RULE_REPO_PREFIX;

/**
 * Utility class for tests involving rules
 */
public class RuleTesting {

  public static final RuleKey EXTERNAL_XOO = RuleKey.of(EXTERNAL_RULE_REPO_PREFIX + "xoo", "x1");
  public static final RuleKey XOO_X1 = RuleKey.of("xoo", "x1");
  public static final RuleKey XOO_X2 = RuleKey.of("xoo", "x2");
  public static final RuleKey XOO_X3 = RuleKey.of("xoo", "x3");

  private static final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private RuleTesting() {
    // only static helpers
  }

  public static RuleDefinitionDto newRule() {
    return newRule(randomRuleKey());
  }

  public static RuleDefinitionDto newRule(RuleKey key) {
    return new RuleDefinitionDto()
      .setRepositoryKey(key.repository())
      .setRuleKey(key.rule())
      .setId(nextInt(100_000))
      .setName("name_" + randomAlphanumeric(5))
      .setDescription("description_" + randomAlphanumeric(5))
      .setDescriptionFormat(Format.HTML)
      .setType(RuleType.values()[nextInt(RuleType.values().length)])
      .setStatus(RuleStatus.READY)
      .setConfigKey("configKey_" + randomAlphanumeric(5))
      .setSeverity(Severity.ALL.get(nextInt(Severity.ALL.size())))
      .setIsTemplate(false)
      .setIsExternal(false)
      .setIsAdHoc(false)
      .setSystemTags(newHashSet("tag_" + randomAlphanumeric(5), "tag_" + randomAlphanumeric(5)))
      .setLanguage("lang_" + randomAlphanumeric(3))
      .setGapDescription("gapDescription_" + randomAlphanumeric(5))
      .setDefRemediationBaseEffort(nextInt(10) + "h")
      .setDefRemediationGapMultiplier(nextInt(10) + "h")
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setCreatedAt(System.currentTimeMillis())
      .setUpdatedAt(System.currentTimeMillis())
      .setScope(Scope.MAIN);
  }

  public static RuleMetadataDto newRuleMetadata() {
    return new RuleMetadataDto()
      .setRuleId(nextInt(100_000))
      .setOrganizationUuid("org_" + randomAlphanumeric(5))
      .setRemediationBaseEffort(nextInt(10) + "h")
      .setRemediationGapMultiplier(nextInt(10) + "h")
      .setRemediationFunction("LINEAR_OFFSET")
      .setTags(newHashSet("tag_" + randomAlphanumeric(5), "tag_" + randomAlphanumeric(5)))
      .setNoteData("noteData_" + randomAlphanumeric(5))
      .setNoteUserUuid("noteUserUuid_" + randomAlphanumeric(5))
      .setNoteCreatedAt(System.currentTimeMillis() - 200)
      .setNoteUpdatedAt(System.currentTimeMillis() - 150)
      .setAdHocName("adHocName_" + randomAlphanumeric(5))
      .setAdHocDescription("adHocDescription_" + randomAlphanumeric(5))
      .setAdHocSeverity(Severity.ALL.get(nextInt(Severity.ALL.size())))
      .setAdHocType(RuleType.values()[nextInt(RuleType.values().length - 1)])
      .setCreatedAt(System.currentTimeMillis() - 100)
      .setUpdatedAt(System.currentTimeMillis() - 50);
  }

  public static RuleMetadataDto newRuleMetadata(RuleDefinitionDto rule, OrganizationDto organization) {
    return newRuleMetadata()
      .setRuleId(rule.getId())
      .setOrganizationUuid(organization.getUuid());
  }

  public static RuleMetadataDto newRuleMetadata(RuleDefinitionDto rule, UserDto noteUser, OrganizationDto organization) {
    return newRuleMetadata(rule, organization)
      .setNoteUserUuid(noteUser.getUuid());
  }

  public static RuleParamDto newRuleParam(RuleDefinitionDto rule) {
    return new RuleParamDto()
      .setRuleId(rule.getId())
      .setName("name_" + randomAlphabetic(5))
      .setDefaultValue("default_" + randomAlphabetic(5))
      .setDescription("description_" + randomAlphabetic(5))
      .setType(RuleParamType.STRING.type());
  }

  public static DeprecatedRuleKeyDto newDeprecatedRuleKey() {
    return new DeprecatedRuleKeyDto()
      .setUuid(uuidFactory.create())
      .setOldRepositoryKey(randomAlphanumeric(50))
      .setOldRuleKey(randomAlphanumeric(50))
      .setRuleId(nextInt(100_000))
      .setCreatedAt(System.currentTimeMillis());
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newXooX1() {
    return newDto(XOO_X1).setLanguage("xoo");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newXooX1(OrganizationDto organization) {
    return newDto(XOO_X1, requireNonNull(organization, "organization can't be null")).setLanguage("xoo");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newXooX2() {
    return newDto(XOO_X2).setLanguage("xoo");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newXooX2(OrganizationDto organization) {
    return newDto(XOO_X2, requireNonNull(organization, "organization can't be null")).setLanguage("xoo");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newXooX3() {
    return newDto(XOO_X3).setLanguage("xoo");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newXooX3(OrganizationDto organization) {
    return newDto(XOO_X3, requireNonNull(organization, "organization can't be null")).setLanguage("xoo");
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newDto(RuleKey ruleKey) {
    return newDto(ruleKey, null);
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newDto(RuleKey ruleKey, @Nullable OrganizationDto organization) {
    RuleDto res = new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setDescriptionFormat(Format.HTML)
      .setStatus(RuleStatus.READY)
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setIsTemplate(false)
      .setSystemTags(ImmutableSet.of("systag1", "systag2"))
      .setLanguage("js")
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setDefRemediationGapMultiplier("5d")
      .setDefRemediationBaseEffort("10h")
      .setGapDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix")
      .setType(RuleType.CODE_SMELL)
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime())
      .setScope(Scope.MAIN);
    if (organization != null) {
      res
        .setOrganizationUuid(organization.getUuid())
        .setTags(ImmutableSet.of("tag1", "tag2"))
        .setRemediationFunction("LINEAR")
        .setRemediationGapMultiplier("1h");
    }
    return res;
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newRuleDto() {
    return newDto(RuleKey.of(randomAlphanumeric(30), randomAlphanumeric(30)));
  }

  /**
   * @deprecated use newRule(...)
   */
  @Deprecated
  public static RuleDto newRuleDto(OrganizationDto organization) {
    return newDto(RuleKey.of(randomAlphanumeric(30), randomAlphanumeric(30)), organization);
  }

  public static RuleDto newTemplateRule(RuleKey ruleKey) {
    return newDto(ruleKey)
      .setIsTemplate(true);
  }

  public static RuleDto newTemplateRule(RuleKey ruleKey, OrganizationDto organization) {
    return newDto(ruleKey, organization)
      .setIsTemplate(true);
  }

  /**
   * @deprecated use {@link #newCustomRule(RuleDefinitionDto)}
   */
  @Deprecated
  public static RuleDto newCustomRule(RuleDto templateRule) {
    checkNotNull(templateRule.getId(), "The template rule need to be persisted before creating this custom rule.");
    return newDto(RuleKey.of(templateRule.getRepositoryKey(), templateRule.getRuleKey() + "_" + System.currentTimeMillis()))
      .setLanguage(templateRule.getLanguage())
      .setTemplateId(templateRule.getId())
      .setType(templateRule.getType());
  }

  public static RuleDefinitionDto newCustomRule(RuleDefinitionDto templateRule) {
    checkNotNull(templateRule.getId(), "The template rule need to be persisted before creating this custom rule.");
    return newRule(RuleKey.of(templateRule.getRepositoryKey(), templateRule.getRuleKey() + "_" + System.currentTimeMillis()))
      .setLanguage(templateRule.getLanguage())
      .setTemplateId(templateRule.getId())
      .setType(templateRule.getType());
  }

  public static RuleKey randomRuleKey() {
    return RuleKey.of("repo_" + randomAlphanumeric(3), "rule_" + randomAlphanumeric(3));
  }

  public static RuleKey randomRuleKeyOfMaximumLength() {
    return RuleKey.of(randomAlphabetic(255), randomAlphabetic(200));
  }

  public static Consumer<RuleDefinitionDto> setRepositoryKey(String repositoryKey) {
    return rule -> rule.setRepositoryKey(repositoryKey);
  }

  public static Consumer<RuleDefinitionDto> setCreatedAt(long createdAt) {
    return rule -> rule.setCreatedAt(createdAt);
  }

  public static Consumer<RuleDefinitionDto> setUpdatedAt(long updatedtAt) {
    return rule -> rule.setUpdatedAt(updatedtAt);
  }

  public static Consumer<RuleDefinitionDto> setRuleKey(String ruleKey) {
    return rule -> rule.setRuleKey(ruleKey);
  }

  public static Consumer<RuleDefinitionDto> setName(String name) {
    return rule -> rule.setName(name);
  }

  public static Consumer<RuleDefinitionDto> setLanguage(String language) {
    return rule -> rule.setLanguage(language);
  }

  public static Consumer<RuleDefinitionDto> setSeverity(String severity) {
    return rule -> rule.setSeverity(severity);
  }

  public static Consumer<RuleDefinitionDto> setStatus(RuleStatus status) {
    return rule -> rule.setStatus(status);
  }

  public static Consumer<RuleDefinitionDto> setType(RuleType type) {
    return rule -> rule.setType(type);
  }

  public static Consumer<RuleDefinitionDto> setIsExternal(boolean isExternal) {
    return rule -> rule.setIsExternal(isExternal);
  }

  public static Consumer<RuleDefinitionDto> setSecurityStandards(Set<String> securityStandards) {
    return rule -> rule.setSecurityStandards(securityStandards);
  }

  public static Consumer<RuleDefinitionDto> setIsTemplate(boolean isTemplate) {
    return rule -> rule.setIsTemplate(isTemplate);
  }

  public static Consumer<RuleDefinitionDto> setTemplateId(@Nullable Integer templateId) {
    return rule -> rule.setTemplateId(templateId);
  }

  public static Consumer<RuleDefinitionDto> setSystemTags(String... tags) {
    return rule -> rule.setSystemTags(copyOf(tags));
  }

  public static Consumer<RuleMetadataDto> setOrganization(OrganizationDto organization) {
    return rule -> rule.setOrganizationUuid(organization.getUuid());
  }

  public static Consumer<RuleMetadataDto> setTags(String... tags) {
    return rule -> rule.setTags(copyOf(tags));
  }

}
