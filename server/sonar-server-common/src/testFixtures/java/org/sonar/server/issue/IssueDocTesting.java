/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.issue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueScope;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.sonar.api.issue.Issue.STATUS_OPEN;

public class IssueDocTesting {

  public static IssueDoc newDoc(ComponentDto componentDto) {
    return newDoc(Uuids.createFast(), componentDto);
  }

  public static IssueDoc newDoc(String key, ComponentDto componentDto) {
    String mainBranchProjectUuid = componentDto.getMainBranchProjectUuid();
    return newDoc()
      .setKey(key)
      .setBranchUuid(componentDto.branchUuid())
      .setComponentUuid(componentDto.uuid())
      .setModuleUuidPath(componentDto.moduleUuidPath())
      .setProjectUuid(mainBranchProjectUuid == null ? componentDto.branchUuid() : mainBranchProjectUuid)
      // File path make no sens on modules and projects
      .setFilePath(!componentDto.scope().equals(Scopes.PROJECT) ? componentDto.path() : null)
      .setIsMainBranch(mainBranchProjectUuid == null)
      .setFuncCreationDate(Date.from(LocalDateTime.of(1970, 1, 1, 1, 1).toInstant(ZoneOffset.UTC)));
  }

  public static IssueDoc newDoc() {
    IssueDoc doc = new IssueDoc(new HashMap<>());
    doc.setKey(Uuids.createFast());
    doc.setRuleUuid(Uuids.createFast());
    doc.setType(RuleType.CODE_SMELL);
    doc.setAssigneeUuid("assignee_uuid_" + randomAlphabetic(26));
    doc.setAuthorLogin("author_" + randomAlphabetic(5));
    doc.setScope(IssueScope.MAIN);
    doc.setLanguage("language_" + randomAlphabetic(5));
    doc.setComponentUuid(Uuids.createFast());
    doc.setFilePath("filePath_" + randomAlphabetic(5));
    doc.setDirectoryPath("directory_" + randomAlphabetic(5));
    doc.setModuleUuidPath(Uuids.createFast());
    doc.setProjectUuid(Uuids.createFast());
    doc.setLine(nextInt(1_000) + 1);
    doc.setStatus(STATUS_OPEN);
    doc.setResolution(null);
    doc.setSeverity(Severity.ALL.get(nextInt(Severity.ALL.size())));
    doc.setEffort((long) nextInt(10));
    doc.setFuncCreationDate(new Date(System.currentTimeMillis() - 2_000));
    doc.setFuncUpdateDate(new Date(System.currentTimeMillis() - 1_000));
    doc.setFuncCloseDate(null);
    return doc;
  }
}
