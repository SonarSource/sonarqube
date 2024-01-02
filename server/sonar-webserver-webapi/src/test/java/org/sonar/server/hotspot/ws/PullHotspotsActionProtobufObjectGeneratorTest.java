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
package org.sonar.server.hotspot.ws;

import java.util.Date;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonarqube.ws.Hotspots.HotspotLite;
import org.sonarqube.ws.Hotspots.HotspotPullQueryTimestamp;

import static org.assertj.core.api.Assertions.assertThat;

public class PullHotspotsActionProtobufObjectGeneratorTest {

  private final PullHotspotsActionProtobufObjectGenerator underTest = new PullHotspotsActionProtobufObjectGenerator();

  @Test
  public void generateTimestampMessage_shouldMapTimestamp() {
    long timestamp = System.currentTimeMillis();
    HotspotPullQueryTimestamp result = underTest.generateTimestampMessage(timestamp);
    assertThat(result.getQueryTimestamp()).isEqualTo(timestamp);
  }

  @Test
  public void generateIssueMessage_shouldMapDtoFields() {
    Date creationDate = new Date();
    IssueDto issueDto = new IssueDto()
      .setKee("key")
      .setFilePath("/home/src/Class.java")
      .setProjectKey("my-project-key")
      .setStatus("REVIEWED")
      .setResolution("FIXED")
      .setRuleKey("repo", "rule")
      .setRuleUuid("rule-uuid-1")
      .setMessage("Look at me, I'm the issue now!")
      .setAssigneeLogin("assignee-login")
      .setIssueCreationDate(creationDate);

    DbIssues.Locations locations = DbIssues.Locations.newBuilder()
      .setTextRange(range(2, 3))
      .build();
    issueDto.setLocations(locations);

    RuleDto ruleDto = new RuleDto()
      .setSecurityStandards(Set.of("cwe:489,cwe:570,cwe:571"));

    HotspotLite result = underTest.generateIssueMessage(issueDto, ruleDto);
    assertThat(result).extracting(
        HotspotLite::getKey,
        HotspotLite::getFilePath,
        HotspotLite::getVulnerabilityProbability,
        HotspotLite::getStatus,
        HotspotLite::getResolution,
        HotspotLite::getRuleKey,
        HotspotLite::getAssignee,
        HotspotLite::getCreationDate)
      .containsExactly("key", "/home/src/Class.java", "LOW", "REVIEWED", "FIXED", "repo:rule", "assignee-login",
        DateUtils.dateToLong(creationDate));
  }

  @Test
  public void generateClosedIssueMessage_shouldMapClosedHotspotFields() {
    HotspotLite result = underTest.generateClosedIssueMessage("uuid");
    assertThat(result).extracting(HotspotLite::getKey, HotspotLite::getClosed)
      .containsExactly("uuid", true);
  }

  private static org.sonar.db.protobuf.DbCommons.TextRange range(int startLine, int endLine) {
    return DbCommons.TextRange.newBuilder().setStartLine(startLine).setEndLine(endLine).build();
  }

}
