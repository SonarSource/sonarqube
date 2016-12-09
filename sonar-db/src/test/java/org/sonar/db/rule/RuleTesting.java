/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Date;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.db.rule.RuleDto.Format;

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

  public static RuleDto newXooX1() {
    return newDto(XOO_X1).setLanguage("xoo");
  }

  public static RuleDto newXooX2() {
    return newDto(XOO_X2).setLanguage("xoo");
  }

  public static RuleDto newXooX3() {
    return newDto(XOO_X3).setLanguage("xoo");
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
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setDescriptionFormat(Format.HTML)
      .setStatus(RuleStatus.READY)
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setIsTemplate(false)
      .setTags(ImmutableSet.of("tag1", "tag2"))
      .setSystemTags(ImmutableSet.of("systag1", "systag2"))
      .setLanguage("js")
      .setRemediationFunction("LINEAR")
      .setDefaultRemediationFunction("LINEAR_OFFSET")
      .setRemediationGapMultiplier("1h")
      .setDefaultRemediationGapMultiplier("5d")
      .setDefaultRemediationBaseEffort("10h")
      .setGapDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix")
      .setType(RuleType.CODE_SMELL)
      .setCreatedAt(new Date().getTime())
      .setUpdatedAt(new Date().getTime());
  }

  public static RuleDto newRuleDto() {
    return newDto(RuleKey.of(randomAlphanumeric(30), randomAlphanumeric(30)));
  }

  public static RuleDto newTemplateRule(RuleKey ruleKey) {
    return newDto(ruleKey)
      .setIsTemplate(true);
  }

  public static RuleDto newCustomRule(RuleDto templateRule) {
    Preconditions.checkNotNull(templateRule.getId(), "The template rule need to be persisted before creating this custom rule.");
    return newDto(RuleKey.of(templateRule.getRepositoryKey(), templateRule.getRuleKey() + "_" + System.currentTimeMillis()))
      .setLanguage(templateRule.getLanguage())
      .setTemplateId(templateRule.getId())
      .setType(templateRule.getType());
  }

}
