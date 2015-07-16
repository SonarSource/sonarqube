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
package org.sonar.server.computation.issue.commonrule;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.qualityprofile.ActiveRule;
import org.sonar.server.computation.qualityprofile.ActiveRulesHolder;

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
        issue.setEffortToFix(cri.effortToFix);
        issue.setMessage(cri.message);
        issue.setRuleKey(ruleKey);
        issue.setSeverity(activeRule.get().getSeverity());
        issue.setLine(null);
        issue.setChecksum("");
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
}
