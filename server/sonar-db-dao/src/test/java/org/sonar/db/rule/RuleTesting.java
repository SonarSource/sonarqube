/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto.Format;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

/**
 * Utility class for tests involving rules
 */
public class RuleTesting {

  public static final RuleKey XOO_X1 = RuleKey.of("xoo", "x1");
  public static final RuleKey XOO_X2 = RuleKey.of("xoo", "x2");
  public static final RuleKey XOO_X3 = RuleKey.of("xoo", "x3");

  private RuleTesting() {
    // only static helpers
  }

  /**
   * Create a RuleDto representing the definition of the rule Xoo1 of language Xoo.
   */
  public static RuleDto newXooX1() {
    return newDto(XOO_X1).setLanguage("xoo");
  }

  /**
   * Create a RuleDto representing the definition of the rule Xoo1 of language Xoo with some user defined fields.
   */
  public static RuleDto newXooX1(OrganizationDto organization) {
    return newDto(XOO_X1, requireNonNull(organization, "organization can't be null")).setLanguage("xoo");
  }

  /**
   * Create a RuleDto representing the definition of the rule Xoo2 of language Xoo.
   */
  public static RuleDto newXooX2() {
    return newDto(XOO_X2).setLanguage("xoo");
  }

  /**
   * Create a RuleDto representing the definition of the rule Xoo2 of language Xoo with some user defined fields.
   */
  public static RuleDto newXooX2(OrganizationDto organization) {
    return newDto(XOO_X2, requireNonNull(organization, "organization can't be null")).setLanguage("xoo");
  }

  /**
   * Create a RuleDto representing the definition of the rule Xoo3 of language Xoo.
   */
  public static RuleDto newXooX3() {
    return newDto(XOO_X3).setLanguage("xoo");
  }

  /**
   * Create a RuleDto representing the definition of the rule Xoo3 of language Xoo with some user defined fields.
   */
  public static RuleDto newXooX3(OrganizationDto organization) {
    return newDto(XOO_X3, requireNonNull(organization, "organization can't be null")).setLanguage("xoo");
  }

  /**
   * Full RuleDto used to feed database with fake data. Tests must not rely on the
   * field contents declared here. They should override the fields they need to test,
   * for example:
   * <pre>
   *   ruleDao.insert(dbSession, RuleTesting.newDto(key).setStatus(RuleStatus.BETA));
   * </pre>
   */
  public static RuleDto newDto(RuleKey ruleKey) {
    return newDto(ruleKey, null);
  }

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
      .setUpdatedAt(new Date().getTime());
    if (organization != null) {
      res
        .setOrganizationUuid(organization.getUuid())
        .setTags(ImmutableSet.of("tag1", "tag2"))
        .setRemediationFunction("LINEAR")
        .setRemediationGapMultiplier("1h");
    }
    return res;
  }

  public static RuleDto newRuleDto() {
    return newDto(RuleKey.of(randomAlphanumeric(30), randomAlphanumeric(30)));
  }


  public static RuleDto newRuleDto(OrganizationDto organization) {
    return newDto(RuleKey.of(randomAlphanumeric(30), randomAlphanumeric(30)), organization);
  }

  /**
   * Creates a new rule to be used as a template for a custom rule.
   */
  public static RuleDto newTemplateRule(RuleKey ruleKey) {
    return newDto(ruleKey)
      .setIsTemplate(true);
  }

  /**
   * Creates a new rule to be used as a template for a custom rule with some user defined fields.
   */
  public static RuleDto newTemplateRule(RuleKey ruleKey, OrganizationDto organization) {
    return newDto(ruleKey, organization)
      .setIsTemplate(true);
  }

  public static RuleDto newCustomRule(RuleDto templateRule) {
    checkNotNull(templateRule.getId(), "The template rule need to be persisted before creating this custom rule.");
    return newDto(RuleKey.of(templateRule.getRepositoryKey(), templateRule.getRuleKey() + "_" + System.currentTimeMillis()))
      .setLanguage(templateRule.getLanguage())
      .setTemplateId(templateRule.getId())
      .setType(templateRule.getType());
  }

  public static RuleKey randomRuleKey() {
    return RuleKey.of(randomAlphanumeric(5), randomAlphanumeric(5));
  }
}
