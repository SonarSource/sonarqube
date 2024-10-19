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
package org.sonar.db.issue;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.Severity;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

public class IssueTesting {

  private static final Random RANDOM = new SecureRandom();

  private IssueTesting() {
    // only statics
  }

  public static IssueDto newIssue(RuleDto rule, ComponentDto branch, ComponentDto file) {
    checkArgument(branch.qualifier().equals(Qualifiers.PROJECT), "Second parameter should be a branch that belongs to a project");
    return newIssue(rule, branch.uuid(), branch.getKey(), file);
  }

  public static IssueDto newIssue(RuleDto rule, BranchDto branch, ComponentDto file) {
    return newIssue(rule, branch.getUuid(), branch.getKey(), file);
  }

  public static IssueDto newIssue(RuleDto rule, String branchUuid, String projectKey, ComponentDto file) {
    //checkArgument(file.branchUuid().equals(branchUuid), "The file doesn't belong to the project");

    return new IssueDto()
      .setKee(Uuids.createFast())
      .setRule(rule)
      .setType(rule.getType())
      .setProjectUuid(branchUuid)
      .setProjectKey(projectKey)
      .setComponent(file)
      .setStatus(Issue.STATUS_OPEN)
      .setResolution(null)
      .setSeverity(Severity.ALL.get(RANDOM.nextInt(Severity.ALL.size())))
      //TODO map to correct impact. Will be fixed with persistence of impacts on issues
      .addImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(org.sonar.api.issue.impact.Severity.HIGH))
      .setEffort((long) RANDOM.nextInt(10))
      .setAssigneeUuid("assignee-uuid_" + randomAlphabetic(26))
      .setAuthorLogin("author_" + randomAlphabetic(5))
      // Starting from 1 in order to never get 0 (as it's a forbidden value)
      .setLine(RANDOM.nextInt(1, 1_001))
      .setMessage("message_" + randomAlphabetic(5))
      .setChecksum("checksum_" + randomAlphabetic(5))
      .setTags(newHashSet("tag_" + randomAlphanumeric(5), "tag_" + randomAlphanumeric(5)))
      .setRuleDescriptionContextKey("context_" + randomAlphabetic(5))
      .setIssueCreationDate(new Date(System.currentTimeMillis() - 2_000))
      .setIssueUpdateDate(new Date(System.currentTimeMillis() - 1_500))
      .setCreatedAt(System.currentTimeMillis() - 1_000)
      .setUpdatedAt(System.currentTimeMillis() - 500);
  }

  public static IssueChangeDto newIssueChangeDto(IssueDto issue) {
    return new IssueChangeDto()
      .setUuid(UuidFactoryFast.getInstance().create())
      .setKey(UuidFactoryFast.getInstance().create())
      .setIssueKey(issue.getKey())
      .setChangeData("data_" + randomAlphanumeric(40))
      .setChangeType(IssueChangeDto.TYPE_FIELD_CHANGE)
      .setUserUuid("userUuid_" + randomAlphanumeric(40))
      .setProjectUuid(issue.getProjectUuid())
      .setIssueChangeCreationDate(RANDOM.nextLong(Long.MAX_VALUE))
      .setCreatedAt(RANDOM.nextLong(Long.MAX_VALUE))
      .setUpdatedAt(RANDOM.nextLong(Long.MAX_VALUE));
  }

  public static NewCodeReferenceIssueDto newCodeReferenceIssue(IssueDto issue) {
    return new NewCodeReferenceIssueDto()
      .setUuid(Uuids.createFast())
      .setIssueKey(issue.getKey())
      .setCreatedAt(1_400_000_000_000L);
  }

  public static List<IssueDto> generateIssues(int total, Function<Integer, IssueDto> issueGenerator) {
    return Stream.iterate(0, i -> i + 1)
      .map(issueGenerator)
      .limit(total)
      .toList();
  }

}
