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
package org.sonar.server.issue;

import com.google.common.collect.Maps;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.issue.index.IssueDoc;

public class IssueDocTesting {

  public static IssueDoc newDoc() {
    IssueDoc doc = new IssueDoc(Maps.<String, Object>newHashMap());
    doc.setKey("ABC");
    doc.setRuleKey(RuleTesting.XOO_X1.toString());
    doc.setType(RuleType.CODE_SMELL);
    doc.setAssignee("steve");
    doc.setAuthorLogin("roger");
    doc.setLanguage("xoo");
    doc.setComponentUuid("FILE_1");
    doc.setGap(3.14);
    doc.setFilePath("src/Foo.xoo");
    doc.setDirectoryPath("/src");
    doc.setMessage("the message");
    doc.setModuleUuid("MODULE_1");
    doc.setModuleUuidPath("MODULE_1");
    doc.setProjectUuid("PROJECT_1");
    doc.setLine(42);
    doc.setAttributes(null);
    doc.setStatus(Issue.STATUS_OPEN);
    doc.setResolution(null);
    doc.setSeverity(Severity.MAJOR);
    doc.setManualSeverity(true);
    doc.setEffort(10L);
    doc.setChecksum("12345");
    doc.setFuncCreationDate(DateUtils.parseDate("2014-09-04"));
    doc.setFuncUpdateDate(DateUtils.parseDate("2014-12-04"));
    doc.setFuncCloseDate(null);
    doc.setTechnicalUpdateDate(DateUtils.parseDate("2014-12-04"));
    return doc;
  }

  public static IssueDoc newDoc(String key, ComponentDto componentDto) {
    return newDoc()
      .setKey(key)
      .setComponentUuid(componentDto.uuid())
      .setModuleUuid(!componentDto.scope().equals(Scopes.PROJECT) ? componentDto.moduleUuid() : componentDto.uuid())
      .setModuleUuidPath(componentDto.moduleUuidPath())
      .setProjectUuid(componentDto.projectUuid())
      // File path make no sens on modules and projects
      .setFilePath(!componentDto.scope().equals(Scopes.PROJECT) ? componentDto.path() : null);
  }
}
