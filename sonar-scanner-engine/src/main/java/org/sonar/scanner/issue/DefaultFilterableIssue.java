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
package org.sonar.scanner.issue;

import java.util.Date;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.fs.internal.DefaultTextRange;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.scanner.ProjectInfo;
import org.sonar.scanner.protocol.output.ScannerReport.Issue;

@ThreadSafe
public class DefaultFilterableIssue implements FilterableIssue {
  private final Issue rawIssue;
  private final InputComponent component;
  private final ProjectInfo projectInfo;
  private DefaultInputProject project;

  public DefaultFilterableIssue(DefaultInputProject project, ProjectInfo projectInfo, Issue rawIssue, InputComponent component) {
    this.project = project;
    this.projectInfo = projectInfo;
    this.rawIssue = rawIssue;
    this.component = component;
  }

  @Override
  public String componentKey() {
    return component.key();
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

  @Deprecated
  @Override
  public Integer line() {
    return rawIssue.hasTextRange() ? rawIssue.getTextRange().getStartLine() : null;
  }

  @Override
  public TextRange textRange() {
    if (!rawIssue.hasTextRange()) {
      return null;
    }

    return new DefaultTextRange(
      new DefaultTextPointer(rawIssue.getTextRange().getStartLine(), rawIssue.getTextRange().getStartOffset()),
      new DefaultTextPointer(rawIssue.getTextRange().getEndLine(), rawIssue.getTextRange().getEndOffset()));
  }

  @Override
  public Double gap() {
    return rawIssue.getGap() != 0 ? rawIssue.getGap() : null;
  }

  @Override
  public Date creationDate() {
    return projectInfo.getAnalysisDate();
  }

  @Override
  public String projectKey() {
    return project.key();
  }

  public InputComponent getComponent() {
    return component;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
