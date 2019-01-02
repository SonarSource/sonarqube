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
package org.sonar.db.issue;

import java.util.Date;
import org.apache.commons.lang.math.RandomUtils;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.apache.commons.lang.math.RandomUtils.nextLong;

public class IssueTesting {

  private IssueTesting() {
    // only statics
  }

  public static IssueDto newIssue(RuleDefinitionDto rule, ComponentDto project, ComponentDto file) {
    checkArgument(project.qualifier().equals(Qualifiers.PROJECT), "Second parameter should be a project");
    checkArgument(file.projectUuid().equals(project.uuid()), "The file doesn't belong to the project");

    return new IssueDto()
      .setKee("uuid_" + randomAlphabetic(5))
      .setRule(rule)
      // exclude security hotspots
      .setType(RuleType.values()[nextInt(RuleType.values().length - 1)])
      .setProject(project)
      .setComponent(file)
      .setStatus(Issue.STATUS_OPEN)
      .setResolution(null)
      .setSeverity(Severity.ALL.get(nextInt(Severity.ALL.size())))
      .setEffort((long) RandomUtils.nextInt(10))
      .setAssigneeUuid("assignee-uuid_" + randomAlphabetic(26))
      .setAuthorLogin("author_" + randomAlphabetic(5))
      // Adding one to the generated random value in order to never get 0 (as it's a forbidden value)
      .setLine(nextInt(1_000) + 1)
      .setMessage("message_" + randomAlphabetic(5))
      .setChecksum("checksum_" + randomAlphabetic(5))
      .setTags(newHashSet("tag_" + randomAlphanumeric(5), "tag_" + randomAlphanumeric(5)))
      .setIssueCreationDate(new Date(System.currentTimeMillis() - 2_000))
      .setIssueUpdateDate(new Date(System.currentTimeMillis() - 1_500))
      .setCreatedAt(System.currentTimeMillis() - 1_000)
      .setUpdatedAt(System.currentTimeMillis() - 500);
  }

  public static IssueChangeDto newIssuechangeDto(IssueDto issue) {
    return new IssueChangeDto()
      .setKey(UuidFactoryFast.getInstance().create())
      .setIssueKey(issue.getKey())
      .setChangeData("data_" + randomAlphanumeric(40))
      .setChangeType(IssueChangeDto.TYPE_FIELD_CHANGE)
      .setUserUuid("userUuid_" + randomAlphanumeric(40))
      .setIssueChangeCreationDate(nextLong())
      .setCreatedAt(nextLong())
      .setUpdatedAt(nextLong());
  }

  /**
   * @deprecated use newIssue(...)
   */
  @Deprecated
  public static IssueDto newDto(RuleDto rule, ComponentDto file, ComponentDto project) {
    return new IssueDto()
      .setKee(Uuids.createFast())
      .setRule(rule.getDefinition())
      .setType(RuleType.CODE_SMELL)
      .setComponent(file)
      .setProject(project)
      .setStatus(Issue.STATUS_OPEN)
      .setResolution(null)
      .setSeverity(Severity.MAJOR)
      .setEffort(10L)
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setCreatedAt(1_400_000_000_000L)
      .setUpdatedAt(1_400_000_000_000L);
  }

}
