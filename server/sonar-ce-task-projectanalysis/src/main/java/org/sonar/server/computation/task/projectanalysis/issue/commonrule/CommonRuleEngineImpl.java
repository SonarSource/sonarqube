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
package org.sonar.server.computation.task.projectanalysis.issue.commonrule;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.task.projectanalysis.component.Component;

public class CommonRuleEngineImpl implements CommonRuleEngine {

  private final CommonRule[] commonRules;

  public CommonRuleEngineImpl(CommonRule... commonRules) {
    this.commonRules = commonRules;
  }

  @Override
  public Collection<DefaultIssue> process(Component component) {
    Collection<DefaultIssue> result = new ArrayList<>();
    String fileLanguage = getFileLanguage(component);
    if (fileLanguage != null) {
      for (CommonRule commonRule : commonRules) {
        DefaultIssue issue = commonRule.processFile(component, fileLanguage);
        if (issue != null) {
          result.add(issue);
        }
      }
    }
    return result;
  }

  @CheckForNull
  private static String getFileLanguage(Component component) {
    if (component.getType() == Component.Type.FILE) {
      return component.getFileAttributes().getLanguageKey();
    }
    return null;
  }
}
