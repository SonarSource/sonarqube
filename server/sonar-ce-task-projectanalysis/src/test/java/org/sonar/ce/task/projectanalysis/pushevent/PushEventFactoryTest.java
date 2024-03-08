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
package org.sonar.ce.task.projectanalysis.pushevent;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.TestBranch;
import org.sonar.ce.task.projectanalysis.component.Component.Type;
import org.sonar.ce.task.projectanalysis.component.MutableTreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.issue.RuleRepository;
import org.sonar.ce.task.projectanalysis.locations.flow.FlowGenerator;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.TaintChecker;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PushEventFactoryTest {

  private static final Gson gson = new Gson();
  private static final String BRANCH_NAME = "develop";

  private final TaintChecker taintChecker = mock(TaintChecker.class);
  @Rule
  public MutableTreeRootHolderRule treeRootHolder = new MutableTreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setBranch(new TestBranch(BRANCH_NAME));
  private final FlowGenerator flowGenerator = new FlowGenerator(treeRootHolder);
  private final RuleRepository ruleRepository = mock(RuleRepository.class);
  private final PushEventFactory underTest = new PushEventFactory(treeRootHolder, analysisMetadataHolder, taintChecker, flowGenerator,
    ruleRepository);

  @Before
  public void setUp() {
    when(ruleRepository.getByKey(RuleKey.of("javasecurity", "S123"))).thenReturn(buildRule());
    buildComponentTree();
  }

  @Test
  public void raiseEventOnIssue_whenNewTaintVulnerability_shouldCreateRaisedEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setNew(true)
      .setRuleDescriptionContextKey(randomAlphabetic(6));

    when(taintChecker.isTaintVulnerability(any())).thenReturn(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo("TaintVulnerabilityRaised");
        verifyPayload(pushEventDto.getPayload(), defaultIssue);
        assertThat(pushEventDto.getLanguage()).isEqualTo("java");
        assertThat(pushEventDto.getProjectUuid()).isEqualTo("some-project-uuid");
      });

  }

  private static void verifyPayload(byte[] payload, DefaultIssue defaultIssue) {
    assertThat(payload).isNotNull();

    TaintVulnerabilityRaised taintVulnerabilityRaised = gson.fromJson(new String(payload, StandardCharsets.UTF_8),
      TaintVulnerabilityRaised.class);
    assertThat(taintVulnerabilityRaised.getProjectKey()).isEqualTo(defaultIssue.projectKey());
    assertThat(taintVulnerabilityRaised.getCreationDate()).isEqualTo(defaultIssue.creationDate().getTime());
    assertThat(taintVulnerabilityRaised.getKey()).isEqualTo(defaultIssue.key());
    assertThat(taintVulnerabilityRaised.getSeverity()).isEqualTo(defaultIssue.severity());
    assertThat(taintVulnerabilityRaised.getRuleKey()).isEqualTo(defaultIssue.ruleKey().toString());
    assertThat(taintVulnerabilityRaised.getType()).isEqualTo(defaultIssue.type().name());
    assertThat(taintVulnerabilityRaised.getBranch()).isEqualTo(BRANCH_NAME);
    assertThat(taintVulnerabilityRaised.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CONVENTIONAL.name());
    assertThat(taintVulnerabilityRaised.getCleanCodeAttributeCategory()).isEqualTo(CleanCodeAttribute.CONVENTIONAL.getAttributeCategory().name());
    assertThat(taintVulnerabilityRaised.getImpacts()).extracting(TaintVulnerabilityRaised.Impact::getSoftwareQuality, TaintVulnerabilityRaised.Impact::getSeverity)
      .containsExactlyInAnyOrder(Tuple.tuple(SoftwareQuality.MAINTAINABILITY.name(), Severity.MEDIUM.name()),
        Tuple.tuple(SoftwareQuality.RELIABILITY.name(), Severity.HIGH.name()));

    String ruleDescriptionContextKey = taintVulnerabilityRaised.getRuleDescriptionContextKey().orElseGet(() -> fail("No rule description " +
                                                                                                                    "context key"));
    assertThat(ruleDescriptionContextKey).isEqualTo(defaultIssue.getRuleDescriptionContextKey().orElse(null));
  }

  @Test
  public void raiseEventOnIssue_whenNewTaintVulnerabilityWithImpactAtRuleAndIssueLevel_shouldMergeImpacts() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setNew(true)
      .addImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH)
      .setRuleDescriptionContextKey(randomAlphabetic(6));

    when(taintChecker.isTaintVulnerability(any())).thenReturn(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        TaintVulnerabilityRaised taintVulnerabilityRaised = gson.fromJson(new String(pushEventDto.getPayload(), StandardCharsets.UTF_8),
          TaintVulnerabilityRaised.class);
        assertThat(taintVulnerabilityRaised.getImpacts()).extracting(TaintVulnerabilityRaised.Impact::getSoftwareQuality, TaintVulnerabilityRaised.Impact::getSeverity)
          .containsExactlyInAnyOrder(tuple(SoftwareQuality.MAINTAINABILITY.name(), Severity.HIGH.name()), tuple(SoftwareQuality.RELIABILITY.name(), Severity.HIGH.name()));
      });
  }

  @Test
  public void raiseEventOnIssue_whenReopenedTaintVulnerability_shouldCreateRaisedEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setChanged(true)
      .setNew(false)
      .setCopied(false)
      .setCurrentChange(new FieldDiffs().setDiff("status", "CLOSED", "OPEN"));

    when(taintChecker.isTaintVulnerability(any())).thenReturn(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo("TaintVulnerabilityRaised");
        assertThat(pushEventDto.getPayload()).isNotNull();
      });
  }

  @Test
  public void raiseEventOnIssue_whenTaintVulnerabilityStatusChange_shouldSkipEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setChanged(true)
      .setNew(false)
      .setCopied(false)
      .setCurrentChange(new FieldDiffs().setDiff("status", "OPEN", "FIXED"));

    when(taintChecker.isTaintVulnerability(any())).thenReturn(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  @Test
  public void raiseEventOnIssue_whenCopiedTaintVulnerability_shouldCreateRaisedEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setCopied(true);

    when(taintChecker.isTaintVulnerability(any())).thenReturn(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo("TaintVulnerabilityRaised");
        assertThat(pushEventDto.getPayload()).isNotNull();
      });
  }

  @Test
  public void raiseEventOnIssue_whenClosedTaintVulnerability_shouldCreateClosedEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setNew(false)
      .setCopied(false)
      .setBeingClosed(true);

    when(taintChecker.isTaintVulnerability(any())).thenReturn(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo("TaintVulnerabilityClosed");
        assertThat(pushEventDto.getPayload()).isNotNull();
      });
  }

  @Test
  public void raiseEventOnIssue_whenChangedTaintVulnerability_shouldSkipEvent() {
    DefaultIssue defaultIssue = new DefaultIssue()
      .setComponentUuid("issue-component-uuid")
      .setNew(false)
      .setCopied(false)
      .setChanged(true)
      .setType(RuleType.VULNERABILITY)
      .setCreationDate(DateUtils.parseDate("2022-01-01"))
      .setRuleKey(RuleKey.of("javasecurity", "S123"));

    when(taintChecker.isTaintVulnerability(any())).thenReturn(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  @Test
  public void raiseEventOnIssue_whenIssueNotFromTaintVulnerabilityRepository_shouldSkipEvent() {
    DefaultIssue defaultIssue = new DefaultIssue()
      .setComponentUuid("issue-component-uuid")
      .setChanged(true)
      .setType(RuleType.VULNERABILITY)
      .setRuleKey(RuleKey.of("weirdrepo", "S123"));

    when(taintChecker.isTaintVulnerability(any())).thenReturn(false);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();

    defaultIssue = new DefaultIssue()
      .setComponentUuid("issue-component-uuid")
      .setChanged(false)
      .setNew(false)
      .setBeingClosed(true)
      .setType(RuleType.VULNERABILITY)
      .setRuleKey(RuleKey.of("weirdrepo", "S123"));

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  @Test
  public void raiseEventOnIssue_whenIssueDoesNotHaveLocations_shouldSkipEvent() {
    DefaultIssue defaultIssue = new DefaultIssue()
      .setComponentUuid("issue-component-uuid")
      .setChanged(true)
      .setType(RuleType.VULNERABILITY)
      .setRuleKey(RuleKey.of("javasecurity", "S123"));

    when(taintChecker.isTaintVulnerability(any())).thenReturn(false);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  @Test
  public void raiseEventOnIssue_whenNewHotspot_shouldCreateRaisedEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setNew(true)
      .setRuleDescriptionContextKey(randomAlphabetic(6));

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo(SecurityHotspotRaised.EVENT_NAME);
        verifyHotspotRaisedEventPayload(pushEventDto.getPayload(), defaultIssue);
        assertThat(pushEventDto.getLanguage()).isEqualTo("java");
        assertThat(pushEventDto.getProjectUuid()).isEqualTo("some-project-uuid");
      });
  }

  private static void verifyHotspotRaisedEventPayload(byte[] payload, DefaultIssue defaultIssue) {
    assertThat(payload).isNotNull();

    SecurityHotspotRaised event = gson.fromJson(new String(payload, StandardCharsets.UTF_8), SecurityHotspotRaised.class);
    assertThat(event.getProjectKey()).isEqualTo(defaultIssue.projectKey());
    assertThat(event.getCreationDate()).isEqualTo(defaultIssue.creationDate().getTime());
    assertThat(event.getKey()).isEqualTo(defaultIssue.key());
    assertThat(event.getRuleKey()).isEqualTo(defaultIssue.ruleKey().toString());
    assertThat(event.getStatus()).isEqualTo(Issue.STATUS_TO_REVIEW);
    assertThat(event.getVulnerabilityProbability()).isEqualTo("LOW");
    assertThat(event.getMainLocation()).isNotNull();
    assertThat(event.getBranch()).isEqualTo(BRANCH_NAME);
    assertThat(event.getAssignee()).isEqualTo("some-user-login");
  }

  @Test
  public void raiseEventOnIssue_whenReopenedHotspot_shouldCreateRaisedEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setChanged(true)
      .setNew(false)
      .setCopied(false)
      .setCurrentChange(new FieldDiffs().setDiff("status", "CLOSED", "TO_REVIEW"));

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo(SecurityHotspotRaised.EVENT_NAME);
        assertThat(pushEventDto.getPayload()).isNotNull();
      });
  }

  @Test
  public void raiseEventOnIssue_whenCopiedHotspot_shouldCreateRaisedEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setCopied(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo(SecurityHotspotRaised.EVENT_NAME);
        assertThat(pushEventDto.getPayload()).isNotNull();
      });
  }

  @Test
  public void raiseEventOnIssue_whenClosedHotspot_shouldCreateClosedEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setNew(false)
      .setCopied(false)
      .setBeingClosed(true)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo(SecurityHotspotClosed.EVENT_NAME);
        verifyHotspotClosedEventPayload(pushEventDto.getPayload(), defaultIssue);
        assertThat(pushEventDto.getLanguage()).isEqualTo("java");
        assertThat(pushEventDto.getProjectUuid()).isEqualTo("some-project-uuid");
      });
  }

  private static void verifyHotspotClosedEventPayload(byte[] payload, DefaultIssue defaultIssue) {
    assertThat(payload).isNotNull();

    SecurityHotspotClosed event = gson.fromJson(new String(payload, StandardCharsets.UTF_8), SecurityHotspotClosed.class);
    assertThat(event.getProjectKey()).isEqualTo(defaultIssue.projectKey());
    assertThat(event.getKey()).isEqualTo(defaultIssue.key());
    assertThat(event.getStatus()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(event.getResolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(event.getFilePath()).isEqualTo("component-name");
  }

  @Test
  public void raiseEventOnIssue_whenChangedHotspot_shouldSkipEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setType(RuleType.SECURITY_HOTSPOT)
      .setChanged(true)
      .setNew(false)
      .setCopied(false);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  @Test
  public void raiseEventOnIssue_whenComponentUuidNull_shouldSkipEvent() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setComponentUuid(null);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  private void buildComponentTree() {
    treeRootHolder.setRoot(ReportComponent.builder(Type.PROJECT, 1)
      .setUuid("uuid_1")
      .addChildren(ReportComponent.builder(Type.FILE, 2)
        .setName("component-name")
        .setUuid("issue-component-uuid")
        .build())
      .addChildren(ReportComponent.builder(Type.FILE, 3)
        .setUuid("location-component-uuid")
        .build())
      .build());
  }

  private DefaultIssue createDefaultIssue() {
    return new DefaultIssue()
      .setKey("issue-key")
      .setProjectKey("project-key")
      .setComponentUuid("issue-component-uuid")
      .setAssigneeUuid("some-user-uuid")
      .setAssigneeLogin("some-user-login")
      .setType(RuleType.VULNERABILITY)
      .setLanguage("java")
      .setCreationDate(new Date())
      .setLocations(DbIssues.Locations.newBuilder()
        .addFlow(DbIssues.Flow.newBuilder()
          .addLocation(DbIssues.Location.newBuilder()
            .setChecksum("checksum")
            .setComponentId("location-component-uuid")
            .build())
          .build())
        .setTextRange(DbCommons.TextRange.newBuilder()
          .setStartLine(1)
          .build())
        .build())
      .setRuleKey(RuleKey.of("javasecurity", "S123"));
  }

  private org.sonar.ce.task.projectanalysis.issue.Rule buildRule() {
    RuleDto ruleDto = new RuleDto();
    ruleDto.setRuleKey(RuleKey.of("javasecurity", "S123"));
    ruleDto.setSecurityStandards(Set.of("owasp-a1"));
    ruleDto.setCleanCodeAttribute(CleanCodeAttribute.CONVENTIONAL);
    ruleDto.addDefaultImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.MAINTAINABILITY).setSeverity(Severity.MEDIUM));
    ruleDto.addDefaultImpact(new ImpactDto().setSoftwareQuality(SoftwareQuality.RELIABILITY).setSeverity(Severity.HIGH));
    return new org.sonar.ce.task.projectanalysis.issue.RuleImpl(ruleDto);
  }
}
