/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.issue.LinkedTicketStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;

class IssueWithoutRuleInfoDtoTest {

  @Test
  void set_tags() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();
    assertThat(dto.getTags()).isEmpty();
    assertThat(dto.getTagsString()).isNull();

    dto.setTags(Arrays.asList("tag1", "tag2", "tag3"));
    assertThat(dto.getTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(dto.getTagsString()).isEqualTo("tag1,tag2,tag3");

    dto.setTags(List.of());
    assertThat(dto.getTags()).isEmpty();

    dto.setTagsString("tag1, tag2 ,,tag3");
    assertThat(dto.getTags()).containsOnly("tag1", "tag2", "tag3");

    dto.setTagsString(null);
    assertThat(dto.getTags()).isEmpty();

    dto.setTagsString("");
    assertThat(dto.getTags()).isEmpty();
  }

  @Test
  void addImpact_whenSoftwareQualityAlreadyDefined_shouldThrowISE() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();
    dto.addImpact(newImpactDto(MAINTAINABILITY, LOW));

    ImpactDto duplicatedImpact = newImpactDto(MAINTAINABILITY, HIGH);

    assertThatThrownBy(() -> dto.addImpact(duplicatedImpact))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Impact already defined on issue for Software Quality [MAINTAINABILITY]");
  }

  @Test
  void replaceAllImpacts_whenSoftwareQualityAlreadyDuplicated_shouldThrowISE() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();
    dto.addImpact(newImpactDto(MAINTAINABILITY, MEDIUM));
    dto.addImpact(newImpactDto(SECURITY, HIGH));
    dto.addImpact(newImpactDto(RELIABILITY, LOW));

    Set<ImpactDto> duplicatedImpacts = Set.of(
      newImpactDto(MAINTAINABILITY, HIGH),
      newImpactDto(MAINTAINABILITY, LOW));
    assertThatThrownBy(() -> dto.replaceAllImpacts(duplicatedImpacts))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Impacts must have unique Software Quality values");
  }

  @Test
  void replaceAllImpacts_shouldReplaceExistingImpacts() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();
    dto.addImpact(newImpactDto(MAINTAINABILITY, MEDIUM));
    dto.addImpact(newImpactDto(SECURITY, HIGH));
    dto.addImpact(newImpactDto(RELIABILITY, LOW));

    Set<ImpactDto> duplicatedImpacts = Set.of(
      newImpactDto(MAINTAINABILITY, HIGH),
      newImpactDto(SECURITY, LOW));

    dto.replaceAllImpacts(duplicatedImpacts);

    assertThat(dto.getImpacts())
      .extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(
        tuple(MAINTAINABILITY, HIGH),
        tuple(SECURITY, LOW));

  }

  @Test
  void setCodeVariants_shouldReturnCodeVariants() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();
    assertThat(dto.getCodeVariants()).isEmpty();
    assertThat(dto.getCodeVariantsString()).isNull();

    dto.setCodeVariants(Arrays.asList("variant1", "variant2", "variant3"));
    assertThat(dto.getCodeVariants()).containsOnly("variant1", "variant2", "variant3");
    assertThat(dto.getCodeVariantsString()).isEqualTo("variant1,variant2,variant3");

    dto.setCodeVariants(null);
    assertThat(dto.getCodeVariants()).isEmpty();
    assertThat(dto.getCodeVariantsString()).isNull();

    dto.setCodeVariants(List.of());
    assertThat(dto.getCodeVariants()).isEmpty();
    assertThat(dto.getCodeVariantsString()).isNull();
  }

  @Test
  void setCodeVariantsString_shouldReturnCodeVariants() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();

    dto.setCodeVariantsString("variant1, variant2 ,,variant4");
    assertThat(dto.getCodeVariants()).containsOnly("variant1", "variant2", "variant4");

    dto.setCodeVariantsString(null);
    assertThat(dto.getCodeVariants()).isEmpty();

    dto.setCodeVariantsString("");
    assertThat(dto.getCodeVariants()).isEmpty();
  }

  @Test
  void setInternalTags_shouldReturnInternalTags() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();
    assertThat(dto.getInternalTags()).isEmpty();
    assertThat(dto.getInternalTagsString()).isNull();

    dto.setInternalTags(Arrays.asList("tag1", "tag2", "tag3"));
    assertThat(dto.getInternalTags()).containsOnly("tag1", "tag2", "tag3");
    assertThat(dto.getInternalTagsString()).isEqualTo("tag1,tag2,tag3");

    dto.setInternalTags(null);
    assertThat(dto.getInternalTags()).isEmpty();
    assertThat(dto.getInternalTagsString()).isNull();

    dto.setInternalTags(List.of());
    assertThat(dto.getInternalTags()).isEmpty();
    assertThat(dto.getInternalTagsString()).isNull();
  }

  @Test
  void setInternalTagsString_shouldReturnInternalTags() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();

    dto.setInternalTagsString("tag1, tag2 ,,tag4");
    assertThat(dto.getInternalTags()).containsOnly("tag1", "tag2", "tag4");

    dto.setInternalTagsString(null);
    assertThat(dto.getInternalTags()).isEmpty();

    dto.setInternalTagsString("");
    assertThat(dto.getInternalTags()).isEmpty();
  }

  @Test
  void getIssueStatus_shouldReturnExpectedValueFromStatusAndResolution() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();
    dto.setStatus(Issue.STATUS_CLOSED);
    assertThat(dto.getIssueStatus()).isEqualTo(IssueStatus.FIXED);

    dto.setStatus(Issue.STATUS_RESOLVED);
    dto.setResolution(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(dto.getIssueStatus()).isEqualTo(IssueStatus.FALSE_POSITIVE);

    dto.setStatus(Issue.STATUS_RESOLVED);
    dto.setResolution(Issue.RESOLUTION_WONT_FIX);
    assertThat(dto.getIssueStatus()).isEqualTo(IssueStatus.ACCEPTED);
  }

  @Test
  void getIssueStatus_shouldReturnOpen_whenStatusIsNull() {
    IssueWithoutRuleInfoDto dto = new IssueWithoutRuleInfoDto();
    assertThat(dto.getIssueStatus())
      .isEqualTo(IssueStatus.OPEN);
  }

  @Test
  void getLinkedTicketStatus_shouldReturnNotLinkedByDefault() {
    var dto = new IssueWithoutRuleInfoDto();

    assertThat(dto.getLinkedTicketStatus()).isEqualTo(LinkedTicketStatus.NOT_LINKED);
  }

  @Test
  void setLinkedTicketStatus_shouldSetLinkedTicketStatus() {
    var dto = new IssueWithoutRuleInfoDto();

    dto.setLinkedTicketStatus(LinkedTicketStatus.LINKED);

    assertThat(dto.getLinkedTicketStatus()).isEqualTo(LinkedTicketStatus.LINKED);
  }

  private static ImpactDto newImpactDto(SoftwareQuality softwareQuality, Severity severity) {
    return new ImpactDto()
      .setSoftwareQuality(softwareQuality)
      .setSeverity(severity);
  }

}
