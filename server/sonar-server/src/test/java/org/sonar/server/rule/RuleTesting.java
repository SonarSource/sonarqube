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
package org.sonar.server.rule;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleDto.Format;

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
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix");
  }

  public static RuleDto newTemplateRule(RuleKey ruleKey){
    return newDto(ruleKey)
      .setIsTemplate(true);
  }

  public static RuleDto newCustomRule(RuleDto templateRule){
    Preconditions.checkNotNull(templateRule.getId(), "The template rule need to be persisted before creating this custom rule.");
    return newDto(RuleKey.of(templateRule.getRepositoryKey(), templateRule.getRuleKey() + "_" + System.currentTimeMillis()))
      .setLanguage(templateRule.getLanguage())
      .setTemplateId(templateRule.getId());
  }

  public static RuleDto newManualRule(String manualKey){
    return new RuleDto().setRuleKey(manualKey)
      .setName("Name " + manualKey)
      .setRepositoryKey(RuleKey.MANUAL_REPOSITORY_KEY)
      .setDescription("Description " + manualKey)
      .setStatus(RuleStatus.READY);
  }

}
