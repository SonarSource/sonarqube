/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.rule.RuleDto.Scope;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.sonar.api.rule.RuleKey.EXTERNAL_RULE_REPO_PREFIX;
import static org.sonar.core.rule.RuleType.CODE_SMELL;
import static org.sonar.db.rule.RuleDescriptionSectionDto.createDefaultRuleDescriptionSection;

/**
 * Utility class for tests involving rules
 */
public class RuleTesting {

  public static final RuleKey EXTERNAL_XOO = RuleKey.of(EXTERNAL_RULE_REPO_PREFIX + "xoo", "x1");
  public static final RuleKey XOO_X1 = RuleKey.of("xoo", "x1");
  public static final RuleKey XOO_X2 = RuleKey.of("xoo", "x2");
  public static final RuleKey XOO_X3 = RuleKey.of("xoo", "x3");

  private static final AtomicLong nextRuleId = new AtomicLong(0);

  private static final Random RANDOM = new SecureRandom();

  private static final UuidFactory uuidFactory = UuidFactoryFast.getInstance();

  private RuleTesting() {
    // only static helpers
  }

  public static RuleDto newRule() {
    return newRule(RuleKey.of(secure().nextAlphanumeric(30), secure().nextAlphanumeric(30)));
  }

  public static RuleDto newRule(RuleDescriptionSectionDto... ruleDescriptionSectionDtos) {
    return newRule(randomRuleKey(), ruleDescriptionSectionDtos);
  }

  public static RuleDto newRule(RuleKey key, RuleDescriptionSectionDto... ruleDescriptionSectionDtos) {
    RuleDto ruleDto = newRuleWithoutDescriptionSection(key);
    if (ruleDescriptionSectionDtos.length == 0) {
      ruleDto.addRuleDescriptionSectionDto(createDefaultRuleDescriptionSection(uuidFactory.create(), "description_" + secure().nextAlphabetic(5)));
    } else {
      stream(ruleDescriptionSectionDtos).forEach(ruleDto::addRuleDescriptionSectionDto);
    }
    return ruleDto;
  }

  public static RuleDto newRuleWithoutDescriptionSection() {
    return newRuleWithoutDescriptionSection(randomRuleKey());
  }

  public static RuleDto newRuleWithoutDescriptionSection(RuleKey ruleKey) {
    long currentTimeMillis = System.currentTimeMillis();
    return new RuleDto()
      .setRepositoryKey(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setUuid("rule_uuid_" + secure().nextAlphanumeric(5))
      .setName("name_" + secure().nextAlphanumeric(5))
      .setDescriptionFormat(RuleDto.Format.HTML)
      .setType(CODE_SMELL)
      .setCleanCodeAttribute(CleanCodeAttribute.CLEAR)
      .addDefaultImpact(new ImpactDto()
        .setSoftwareQuality(SoftwareQuality.MAINTAINABILITY)
        .setSeverity(org.sonar.api.issue.impact.Severity.HIGH))
      .setStatus(RuleStatus.READY)
      .setConfigKey("configKey_" + ruleKey.rule())
      .setSeverity(Severity.ALL.get(RANDOM.nextInt(Severity.ALL.size())))
      .setIsTemplate(false)
      .setIsExternal(false)
      .setIsAdHoc(false)
      .setSystemTags(newHashSet("tag_" + secure().nextAlphanumeric(5), "tag_" + secure().nextAlphanumeric(5)))
      .setLanguage("lang_" + secure().nextAlphanumeric(3))
      .setGapDescription("gapDescription_" + secure().nextAlphanumeric(5))
      .setDefRemediationBaseEffort(RANDOM.nextInt(10) + "h")
      // voluntarily offset the remediation to be able to detect issues
      .setDefRemediationGapMultiplier((RANDOM.nextInt(10) + 10) + "h")
      .setDefRemediationFunction("LINEAR_OFFSET")
      .setRemediationBaseEffort(RANDOM.nextInt(10) + "h")
      .setRemediationGapMultiplier(RANDOM.nextInt(10) + "h")
      .setRemediationFunction("LINEAR_OFFSET")
      .setTags(newHashSet("tag_" + secure().nextAlphanumeric(5), "tag_" + secure().nextAlphanumeric(5)))
      .setNoteData("noteData_" + secure().nextAlphanumeric(5))
      .setNoteUserUuid("noteUserUuid_" + secure().nextAlphanumeric(5))
      .setNoteCreatedAt(System.currentTimeMillis() - 200)
      .setNoteUpdatedAt(System.currentTimeMillis() - 150)
      .setAdHocName("adHocName_" + secure().nextAlphanumeric(5))
      .setAdHocDescription("adHocDescription_" + secure().nextAlphanumeric(5))
      .setAdHocSeverity(Severity.ALL.get(RANDOM.nextInt(Severity.ALL.size())))
      .setAdHocType(RuleType.values()[RANDOM.nextInt(RuleType.values().length - 1)])
      .setCreatedAt(currentTimeMillis)
      .setUpdatedAt(currentTimeMillis + 5)
      .setScope(Scope.MAIN)
      .setEducationPrinciples(Set.of(secure().nextAlphanumeric(5), secure().nextAlphanumeric(5)));
  }

  public static RuleParamDto newRuleParam(RuleDto rule) {
    return new RuleParamDto()
      .setRuleUuid(rule.getUuid())
      .setName("name_" + secure().nextAlphabetic(5))
      .setDefaultValue("default_" + secure().nextAlphabetic(5))
      .setDescription("description_" + secure().nextAlphabetic(5))
      .setType(RuleParamType.STRING.type());
  }

  public static DeprecatedRuleKeyDto newDeprecatedRuleKey() {
    return new DeprecatedRuleKeyDto()
      .setUuid(uuidFactory.create())
      .setOldRepositoryKey(secure().nextAlphanumeric(50))
      .setOldRuleKey(secure().nextAlphanumeric(50))
      .setRuleUuid(secure().nextAlphanumeric(40))
      .setCreatedAt(System.currentTimeMillis());
  }

  public static RuleDto newXooX1() {
    return newRule(XOO_X1).setLanguage("xoo");
  }

  public static RuleDto newXooX2() {
    return newRule(XOO_X2).setLanguage("xoo");
  }

  public static RuleDto newTemplateRule(RuleKey ruleKey) {
    return newRule(ruleKey)
      .setIsTemplate(true);
  }

  public static RuleDto newCustomRule(RuleDto templateRule) {
    return newCustomRule(templateRule, "description_" + secure().nextAlphabetic(5));
  }

  public static RuleDto newCustomRule(RuleDto templateRule, String description) {
    checkNotNull(templateRule.getUuid(), "The template rule need to be persisted before creating this custom rule.");
    RuleDescriptionSectionDto defaultRuleDescriptionSection = createDefaultRuleDescriptionSection(uuidFactory.create(), description);
    return newRule(RuleKey.of(templateRule.getRepositoryKey(), templateRule.getRuleKey() + "_" + System.currentTimeMillis()), defaultRuleDescriptionSection)
      .setLanguage(templateRule.getLanguage())
      .setTemplateUuid(templateRule.getUuid())
      .setType(templateRule.getType());
  }

  public static RuleKey randomRuleKey() {
    return RuleKey.of("repo_" + getNextUniqueId(), "rule_" + getNextUniqueId());
  }

  private static String getNextUniqueId() {
    return String.format("%010d", nextRuleId.getAndIncrement());
  }

  public static Consumer<RuleDto> setRepositoryKey(String repositoryKey) {
    return rule -> rule.setRepositoryKey(repositoryKey);
  }

  public static Consumer<RuleDto> setCreatedAt(long createdAt) {
    return rule -> rule.setCreatedAt(createdAt);
  }

  public static Consumer<RuleDto> setUpdatedAt(long updatedtAt) {
    return rule -> rule.setUpdatedAt(updatedtAt);
  }

  public static Consumer<RuleDto> setRuleKey(String ruleKey) {
    return rule -> rule.setRuleKey(ruleKey);
  }

  public static Consumer<RuleDto> setName(String name) {
    return rule -> rule.setName(name);
  }

  public static Consumer<RuleDto> setLanguage(String language) {
    return rule -> rule.setLanguage(language);
  }

  public static Consumer<RuleDto> setSeverity(String severity) {
    return rule -> rule.setSeverity(severity);
  }

  public static Consumer<RuleDto> setStatus(RuleStatus status) {
    return rule -> rule.setStatus(status);
  }

  public static Consumer<RuleDto> setType(RuleType type) {
    return rule -> rule.setType(type);
  }

  public static Consumer<RuleDto> setIsExternal(boolean isExternal) {
    return rule -> rule.setIsExternal(isExternal);
  }

  public static Consumer<RuleDto> setSecurityStandards(Set<String> securityStandards) {
    return rule -> rule.setSecurityStandards(securityStandards);
  }

  public static Consumer<RuleDto> setIsTemplate(boolean isTemplate) {
    return rule -> rule.setIsTemplate(isTemplate);
  }

  public static Consumer<RuleDto> setTemplateId(@Nullable String templateUuid) {
    return rule -> rule.setTemplateUuid(templateUuid);
  }

  public static Consumer<RuleDto> setSystemTags(String... tags) {
    return rule -> rule.setSystemTags(copyOf(tags));
  }

  public static Consumer<RuleDto> setTags(String... tags) {
    return rule -> rule.setTags(copyOf(tags));
  }

  public static Consumer<RuleDto> setCleanCodeAttribute(CleanCodeAttribute cleanCodeAttribute) {
    return rule -> rule.setCleanCodeAttribute(cleanCodeAttribute);
  }

  public static Consumer<RuleDto> setImpacts(Collection<ImpactDto> impacts) {
    return rule -> rule.replaceAllDefaultImpacts(impacts);
  }

}
