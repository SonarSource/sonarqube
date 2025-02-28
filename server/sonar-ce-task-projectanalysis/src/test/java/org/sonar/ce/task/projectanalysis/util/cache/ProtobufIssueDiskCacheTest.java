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
package org.sonar.ce.task.projectanalysis.util.cache;

import java.util.Date;
import org.junit.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.core.issue.DefaultImpact;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtobufIssueDiskCacheTest {

  private static final String TEST_CONTEXT_KEY = "test_context_key";

  @Test
  public void toDefaultIssue_whenRuleDescriptionContextKeyPresent_shouldSetItInDefaultIssue() {
    IssueCache.Issue issue = prepareIssueWithCompulsoryFields()
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY)
      .build();

    DefaultIssue defaultIssue = ProtobufIssueDiskCache.toDefaultIssue(issue);

    assertThat(defaultIssue.getRuleDescriptionContextKey()).contains(TEST_CONTEXT_KEY);
  }

  @Test
  public void toDefaultIssue_whenRuleDescriptionContextKeyAbsent_shouldNotSetItInDefaultIssue() {
    IssueCache.Issue issue = prepareIssueWithCompulsoryFields()
      .build();

    DefaultIssue defaultIssue = ProtobufIssueDiskCache.toDefaultIssue(issue);

    assertThat(defaultIssue.getRuleDescriptionContextKey()).isEmpty();
  }

  @Test
  public void toDefaultIssue_whenImpactIsSet_shouldSetItInDefaultIssue() {
    IssueCache.Issue issue = prepareIssueWithCompulsoryFields()
      .addImpacts(toImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, true))
      .addImpacts(toImpact(SoftwareQuality.RELIABILITY, Severity.LOW, false))
      .build();

    DefaultIssue defaultIssue = ProtobufIssueDiskCache.toDefaultIssue(issue);

    assertThat(defaultIssue.getImpacts())
      .containsExactly(
        new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, true),
        new DefaultImpact(SoftwareQuality.RELIABILITY, Severity.LOW, false));
  }

  @Test
  public void toDefaultIssue_whenCleanCodeAttributeIsSet_shouldSetItInDefaultIssue() {
    IssueCache.Issue issue = prepareIssueWithCompulsoryFields()
      .setCleanCodeAttribute(CleanCodeAttribute.FOCUSED.name())
      .build();

    DefaultIssue defaultIssue = ProtobufIssueDiskCache.toDefaultIssue(issue);

    assertThat(defaultIssue.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.FOCUSED);
  }

  @Test
  public void toProto_whenRuleDescriptionContextKeySet_shouldCopyToIssueProto() {
    DefaultIssue defaultIssue = createDefaultIssueWithMandatoryFields();
    defaultIssue.setRuleDescriptionContextKey(TEST_CONTEXT_KEY);

    IssueCache.Issue issue = ProtobufIssueDiskCache.toProto(IssueCache.Issue.newBuilder(), defaultIssue);

    assertThat(issue.hasRuleDescriptionContextKey()).isTrue();
    assertThat(issue.getRuleDescriptionContextKey()).isEqualTo(TEST_CONTEXT_KEY);
  }

  @Test
  public void toProto_whenRuleDescriptionContextKeyNotSet_shouldCopyToIssueProto() {
    DefaultIssue defaultIssue = createDefaultIssueWithMandatoryFields();
    defaultIssue.setRuleDescriptionContextKey(null);

    IssueCache.Issue issue = ProtobufIssueDiskCache.toProto(IssueCache.Issue.newBuilder(), defaultIssue);

    assertThat(issue.hasRuleDescriptionContextKey()).isFalse();
  }

  @Test
  public void toProto_whenRuleDescriptionContextKeyIsSet_shouldCopyToIssueProto() {
    DefaultIssue defaultIssue = createDefaultIssueWithMandatoryFields();
    defaultIssue.addImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, true);
    defaultIssue.addImpact(SoftwareQuality.RELIABILITY, Severity.LOW, false);

    IssueCache.Issue issue = ProtobufIssueDiskCache.toProto(IssueCache.Issue.newBuilder(), defaultIssue);

    assertThat(issue.getImpactsList()).containsExactly(
      toImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, true),
      toImpact(SoftwareQuality.RELIABILITY, Severity.LOW, false));
  }

  @Test
  public void toProto_whenCleanCodeAttributeIsSet_shouldCopyToIssueProto() {
    DefaultIssue defaultIssue = createDefaultIssueWithMandatoryFields();
    defaultIssue.setCleanCodeAttribute(CleanCodeAttribute.FOCUSED);

    IssueCache.Issue issue = ProtobufIssueDiskCache.toProto(IssueCache.Issue.newBuilder(), defaultIssue);

    assertThat(issue.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.FOCUSED.name());
  }

  private IssueCache.Impact toImpact(SoftwareQuality softwareQuality, Severity severity, boolean manualSeverity) {
    return IssueCache.Impact.newBuilder().setSoftwareQuality(softwareQuality.name()).setSeverity(severity.name()).setManualSeverity(manualSeverity).build();
  }

  private static DefaultIssue createDefaultIssueWithMandatoryFields() {
    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setKey("test_issue:key");
    defaultIssue.setType(RuleType.CODE_SMELL);
    defaultIssue.setComponentKey("component_key");
    defaultIssue.setProjectUuid("project_uuid");
    defaultIssue.setProjectKey("project_key");
    defaultIssue.setRuleKey(RuleKey.of("ruleRepo", "rule1"));
    defaultIssue.setStatus("open");
    defaultIssue.setCreationDate(new Date());
    return defaultIssue;
  }

  private static IssueCache.Issue.Builder prepareIssueWithCompulsoryFields() {
    return IssueCache.Issue.newBuilder()
      .setRuleType(RuleType.CODE_SMELL.getDbConstant())
      .setRuleKey("test_issue:key")
      .setStatus("open");
  }

}
