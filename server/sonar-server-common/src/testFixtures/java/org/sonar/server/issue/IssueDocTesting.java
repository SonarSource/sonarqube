/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import org.sonar.db.component.ComponentScopes;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueScope;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.sonar.api.issue.Issue.STATUS_OPEN;

public class IssueDocTesting {

  private static final Random RANDOM = new SecureRandom();

  public static IssueDoc newDoc(ComponentDto componentDto, String projectUuid) {
    return newDoc(Uuids.createFast(), projectUuid, componentDto);
  }

  public static IssueDoc newDocForProject(ComponentDto project) {
    return newDocForProject(Uuids.createFast(), project);
  }

  /**
   * main branch definition should not be done based on main branch uuid.
   * Use org.sonar.server.issue.IssueDocTesting#newDoc(java.lang.String, java.lang.String, boolean, org.sonar.db.component.ComponentDto) instead.
   */
  @Deprecated
  public static IssueDoc newDoc(String key, String projectUuid, ComponentDto componentDto) {
    return newDoc(key, projectUuid, componentDto.branchUuid().equals(projectUuid), componentDto);
  }

  public static IssueDoc newDoc(String key, String projectUuid, boolean isMainBranch, ComponentDto componentDto) {
    return newDoc()
      .setKey(key)
      .setBranchUuid(componentDto.branchUuid())
      .setComponentUuid(componentDto.uuid())
      .setProjectUuid(projectUuid)
      // File path make no sens on modules and projects
      .setFilePath(!componentDto.scope().equals(ComponentScopes.PROJECT) ? componentDto.path() : null)
      .setIsMainBranch(isMainBranch)
      .setFuncCreationDate(Date.from(LocalDateTime.of(1970, 1, 1, 1, 1).toInstant(ZoneOffset.UTC)));
  }

  public static IssueDoc newDocForProject(String key, ComponentDto project) {
    return newDoc()
      .setKey(key)
      .setBranchUuid(project.branchUuid())
      .setComponentUuid(project.uuid())
      .setProjectUuid(project.branchUuid())
      // File path make no sens on modules and projects
      .setFilePath(!project.scope().equals(ComponentScopes.PROJECT) ? project.path() : null)
      .setIsMainBranch(true)
      .setFuncCreationDate(Date.from(LocalDateTime.of(1970, 1, 1, 1, 1).toInstant(ZoneOffset.UTC)));
  }

  public static IssueDoc newDoc() {
    IssueDoc doc = new IssueDoc(new HashMap<>());
    doc.setKey(Uuids.createFast());
    doc.setRuleUuid(Uuids.createFast());
    doc.setType(RuleType.CODE_SMELL);
    doc.setAssigneeUuid("assignee_uuid_" + secure().nextAlphabetic(26));
    doc.setAuthorLogin("author_" + secure().nextAlphabetic(5));
    doc.setScope(IssueScope.MAIN);
    doc.setLanguage("language_" + secure().nextAlphabetic(5));
    doc.setComponentUuid(Uuids.createFast());
    doc.setFilePath("filePath_" + secure().nextAlphabetic(5));
    doc.setDirectoryPath("directory_" + secure().nextAlphabetic(5));
    doc.setProjectUuid(Uuids.createFast());
    doc.setLine(RANDOM.nextInt(1_000) + 1);
    doc.setStatus(STATUS_OPEN);
    doc.setResolution(null);
    doc.setSeverity(Severity.ALL.get(RANDOM.nextInt(Severity.ALL.size())));
    doc.setEffort((long) RANDOM.nextInt(10));
    doc.setFuncCreationDate(new Date(System.currentTimeMillis() - 2_000));
    doc.setFuncUpdateDate(new Date(System.currentTimeMillis() - 1_000));
    doc.setFuncCloseDate(null);
    return doc;
  }
}
