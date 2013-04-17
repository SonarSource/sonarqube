/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.issue;

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueActions;
import org.sonar.core.issue.DefaultIssue;

import javax.annotation.Nullable;

public class ScanIssueActions implements IssueActions {

  @Override
  public Issue comment(Issue issue, String userLogin, String comment) {
    throw new UnsupportedOperationException("Not supported yet from batch");
  }

  @Override
  public Issue setSeverity(Issue issue, String severity) {
    DefaultIssue impl = (DefaultIssue) issue;
    impl.setSeverity(severity);
    return impl;
  }

  @Override
  public Issue setManualSeverity(Issue issue, String severity) {
    DefaultIssue impl = (DefaultIssue) issue;
    impl.setSeverity(severity);
    impl.setManualSeverity(true);
    return impl;
  }

  @Override
  public Issue setMessage(Issue issue, String message) {
    DefaultIssue impl = (DefaultIssue) issue;
    impl.setMessage(message);
    return impl;
  }

  @Override
  public Issue setCost(Issue issue, @Nullable Double cost) {
    DefaultIssue impl = (DefaultIssue) issue;
    impl.setCost(cost);
    return impl;
  }

  @Override
  public Issue setResolution(Issue issue, String resolution) {
    DefaultIssue impl = (DefaultIssue) issue;
    impl.setResolution(resolution);
    return impl;
  }

  @Override
  public Issue assign(Issue issue, String userLogin) {
    DefaultIssue impl = (DefaultIssue) issue;
    impl.setUserLogin(userLogin);
    return impl;
  }

  @Override
  public Issue setAttribute(Issue issue, String key, @Nullable String value) {
    DefaultIssue impl = (DefaultIssue) issue;
    impl.setAttribute(key, value);
    return impl;
  }
}
