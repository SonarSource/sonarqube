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
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.DateUtils;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.TestBranch;
import org.sonar.ce.task.projectanalysis.component.Component.Type;
import org.sonar.ce.task.projectanalysis.component.MutableTreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.locations.flow.FlowGenerator;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.issue.TaintChecker;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
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
  private final PushEventFactory underTest = new PushEventFactory(treeRootHolder, analysisMetadataHolder, taintChecker, flowGenerator);

  @Before
  public void setUp() {
    when(taintChecker.getTaintRepositories()).thenReturn(List.of("roslyn.sonaranalyzer.security.cs",
      "javasecurity", "jssecurity", "tssecurity", "phpsecurity", "pythonsecurity"));
    when(taintChecker.isTaintVulnerability(any())).thenReturn(true);
    buildComponentTree();
  }

  @Test
  public void raise_event_to_repository_if_taint_vulnerability_is_new() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setNew(true)
      .setRuleDescriptionContextKey(randomAlphabetic(6));

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

    TaintVulnerabilityRaised taintVulnerabilityRaised = gson.fromJson(new String(payload, StandardCharsets.UTF_8), TaintVulnerabilityRaised.class);
    assertThat(taintVulnerabilityRaised.getProjectKey()).isEqualTo(defaultIssue.projectKey());
    assertThat(taintVulnerabilityRaised.getCreationDate()).isEqualTo(defaultIssue.creationDate().getTime());
    assertThat(taintVulnerabilityRaised.getKey()).isEqualTo(defaultIssue.key());
    assertThat(taintVulnerabilityRaised.getSeverity()).isEqualTo(defaultIssue.severity());
    assertThat(taintVulnerabilityRaised.getRuleKey()).isEqualTo(defaultIssue.ruleKey().toString());
    assertThat(taintVulnerabilityRaised.getType()).isEqualTo(defaultIssue.type().name());
    assertThat(taintVulnerabilityRaised.getBranch()).isEqualTo(BRANCH_NAME);
    String ruleDescriptionContextKey = taintVulnerabilityRaised.getRuleDescriptionContextKey().orElseGet(() -> fail("No rule description context key"));
    assertThat(ruleDescriptionContextKey).isEqualTo(defaultIssue.getRuleDescriptionContextKey().orElse(null));
  }

  @Test
  public void raise_event_to_repository_if_taint_vulnerability_is_reopened() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setChanged(true)
      .setNew(false)
      .setCopied(false)
      .setCurrentChange(new FieldDiffs().setDiff("status", "CLOSED", "OPEN"));

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo("TaintVulnerabilityRaised");
        assertThat(pushEventDto.getPayload()).isNotNull();
      });
  }

  @Test
  public void skip_event_if_taint_vulnerability_status_change() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setChanged(true)
      .setNew(false)
      .setCopied(false)
      .setCurrentChange(new FieldDiffs().setDiff("status", "OPEN", "FIXED"));

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  @Test
  public void raise_event_to_repository_if_taint_vulnerability_is_copied() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setCopied(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo("TaintVulnerabilityRaised");
        assertThat(pushEventDto.getPayload()).isNotNull();
      });
  }

  @Test
  public void raise_event_to_repository_if_taint_vulnerability_is_closed() {
    DefaultIssue defaultIssue = createDefaultIssue()
      .setComponentUuid("")
      .setNew(false)
      .setCopied(false)
      .setBeingClosed(true);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue))
      .isNotEmpty()
      .hasValueSatisfying(pushEventDto -> {
        assertThat(pushEventDto.getName()).isEqualTo("TaintVulnerabilityClosed");
        assertThat(pushEventDto.getPayload()).isNotNull();
      });
  }

  @Test
  public void skip_issue_if_issue_changed() {
    DefaultIssue defaultIssue = new DefaultIssue()
      .setComponentUuid("issue-component-uuid")
      .setNew(false)
      .setCopied(false)
      .setChanged(true)
      .setType(RuleType.VULNERABILITY)
      .setCreationDate(DateUtils.parseDate("2022-01-01"))
      .setRuleKey(RuleKey.of("javasecurity", "S123"));

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  @Test
  public void skip_if_issue_not_from_taint_vulnerability_repository() {
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
  public void skip_if_issue_is_a_hotspot() {
    DefaultIssue defaultIssue = new DefaultIssue()
      .setComponentUuid("issue-component-uuid")
      .setChanged(true)
      .setType(RuleType.SECURITY_HOTSPOT)
      .setRuleKey(RuleKey.of("javasecurity", "S123"));

    when(taintChecker.isTaintVulnerability(any())).thenReturn(false);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  @Test
  public void skip_if_issue_does_not_have_locations() {
    DefaultIssue defaultIssue = new DefaultIssue()
      .setComponentUuid("issue-component-uuid")
      .setChanged(true)
      .setType(RuleType.VULNERABILITY)
      .setRuleKey(RuleKey.of("javasecurity", "S123"));

    when(taintChecker.isTaintVulnerability(any())).thenReturn(false);

    assertThat(underTest.raiseEventOnIssue("some-project-uuid", defaultIssue)).isEmpty();
  }

  private void buildComponentTree() {
    treeRootHolder.setRoot(ReportComponent.builder(Type.PROJECT, 1)
      .setUuid("uuid_1")
      .addChildren(ReportComponent.builder(Type.FILE, 2)
        .setUuid("issue-component-uuid")
        .build())
      .addChildren(ReportComponent.builder(Type.FILE, 3)
        .setUuid("location-component-uuid")
        .build())
      .build());
  }

  private DefaultIssue createDefaultIssue() {
    return new DefaultIssue()
      .setComponentUuid("issue-component-uuid")
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

}
