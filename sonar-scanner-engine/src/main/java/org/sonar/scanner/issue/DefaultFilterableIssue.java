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
package org.sonar.scanner.issue;

import java.util.Date;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.protocol.output.ScannerReport.Issue;

@ThreadSafe
public class DefaultFilterableIssue implements FilterableIssue {
  private final Issue rawIssue;
  private final ProjectAnalysisInfo projectAnalysisInfo;
  private final String componentKey;
  private DefaultInputModule module;

  public DefaultFilterableIssue(InputModule module, ProjectAnalysisInfo projectAnalysisInfo, Issue rawIssue, String componentKey) {
    this.module = (DefaultInputModule) module;
    this.projectAnalysisInfo = projectAnalysisInfo;
    this.rawIssue = rawIssue;
    this.componentKey = componentKey;
  }

  @Override
  public String componentKey() {
    return componentKey;
  }

  @Override
  public RuleKey ruleKey() {
    return RuleKey.of(rawIssue.getRuleRepository(), rawIssue.getRuleKey());
  }

  @Override
  public String severity() {
    return rawIssue.getSeverity().name();
  }

  @Override
  public String message() {
    return rawIssue.getMsg();
  }

  @Override
  public Integer line() {
    return rawIssue.hasTextRange() ? rawIssue.getTextRange().getStartLine() : null;
  }

  @Override
  public Double gap() {
    return rawIssue.getGap() != 0 ? rawIssue.getGap() : null;
  }

  @Override
  public Double effortToFix() {
    return gap();
  }

  @Override
  public Date creationDate() {
    return projectAnalysisInfo.analysisDate();
  }

  @Override
  public String projectKey() {
    return module.key();
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
