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
package org.sonar.ce.task.projectanalysis.issue.commonrule;

import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.core.issue.DefaultIssue;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.server.rule.CommonRuleKeys.commonRepositoryForLang;

public abstract class CommonRule {

  private final ActiveRulesHolder activeRulesHolder;
  private final String key;

  public CommonRule(ActiveRulesHolder activeRulesHolder, String key) {
    this.activeRulesHolder = activeRulesHolder;
    this.key = key;
  }

  @CheckForNull
  public DefaultIssue processFile(Component file, String fileLanguage) {
    DefaultIssue issue = null;
    RuleKey ruleKey = RuleKey.of(commonRepositoryForLang(fileLanguage), key);
    Optional<ActiveRule> activeRule = activeRulesHolder.get(ruleKey);
    if (activeRule.isPresent()) {
      CommonRuleIssue cri = doProcessFile(file, activeRule.get());
      if (cri != null) {
        issue = new DefaultIssue();
        issue.setGap(cri.effortToFix);
        issue.setMessage(cri.message);
        issue.setRuleKey(ruleKey);
        issue.setSeverity(activeRule.get().getSeverity());
        issue.setLine(null);
        issue.setChecksum("");
        issue.setIsFromExternalRuleEngine(false);
      }
    }
    return issue;
  }

  /**
   * Implementations must only set the effort to fix and message, so 
   * using the lite intermediary object {@link CommonRuleIssue}
   */
  @CheckForNull
  protected abstract CommonRuleIssue doProcessFile(Component file, ActiveRule activeRule);

  protected static class CommonRuleIssue {
    private final double effortToFix;
    // FIXME which format ? HTML or MD ?
    private final String message;

    public CommonRuleIssue(double effortToFix, String message) {
      this.effortToFix = effortToFix;
      this.message = message;
    }
  }

  protected static double getMinDensityParam(ActiveRule activeRule, String paramKey) {
    String s = activeRule.getParams().get(paramKey);
    if (isNotBlank(s)) {
      double d = Double.parseDouble(s);
      if (d < 0.0 || d > 100.0) {
        throw new IllegalStateException(String.format("Minimum density of rule [%s] is incorrect. Got [%s] but must be between 0 and 100.", activeRule.getRuleKey(), s));
      }
      return d;
    }
    throw new IllegalStateException(String.format("Required parameter [%s] is missing on rule [%s]", paramKey, activeRule.getRuleKey()));
  }
}
