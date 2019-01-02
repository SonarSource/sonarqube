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
package org.sonar.server.webhook;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListener;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.SHORT;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

@RunWith(DataProviderRunner.class)
public class WebhookQGChangeEventListenerTest {

  private static final Set<QGChangeEventListener.ChangedIssue> CHANGED_ISSUES_ARE_IGNORED = emptySet();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();

  private EvaluatedQualityGate newQualityGate = mock(EvaluatedQualityGate.class);
  private WebHooks webHooks = mock(WebHooks.class);
  private WebhookPayloadFactory webhookPayloadFactory = mock(WebhookPayloadFactory.class);
  private DbClient spiedOnDbClient = Mockito.spy(dbClient);
  private WebhookQGChangeEventListener underTest = new WebhookQGChangeEventListener(webHooks, webhookPayloadFactory, spiedOnDbClient);
  private DbClient mockedDbClient = mock(DbClient.class);
  private WebhookQGChangeEventListener mockedUnderTest = new WebhookQGChangeEventListener(webHooks, webhookPayloadFactory, mockedDbClient);

  @Test
  @UseDataProvider("allCombinationsOfStatuses")
  public void onIssueChanges_has_no_effect_if_no_webhook_is_configured(Metric.Level previousStatus, Metric.Level newStatus) {
    Configuration configuration1 = mock(Configuration.class);
    when(newQualityGate.getStatus()).thenReturn(newStatus);
    QGChangeEvent qualityGateEvent = newQGChangeEvent(configuration1, previousStatus, newQualityGate);
    mockWebhookDisabled(qualityGateEvent.getProject());

    mockedUnderTest.onIssueChanges(qualityGateEvent, CHANGED_ISSUES_ARE_IGNORED);

    verify(webHooks).isEnabled(qualityGateEvent.getProject());
    verifyZeroInteractions(webhookPayloadFactory, mockedDbClient);
  }

  @DataProvider
  public static Object[][] allCombinationsOfStatuses() {
    Metric.Level[] levelsAndNull = concat(of((Metric.Level) null), stream(Metric.Level.values()))
      .toArray(Metric.Level[]::new);
    Object[][] res = new Object[levelsAndNull.length * levelsAndNull.length][2];
    int i = 0;
    for (Metric.Level previousStatus : Arrays.asList(levelsAndNull)) {
      for (Metric.Level newStatus : Arrays.asList(levelsAndNull)) {
        res[i][0] = previousStatus;
        res[i][1] = newStatus;
        i++;
      }
    }
    return res;
  }

  @Test
  public void onIssueChanges_has_no_effect_if_event_has_neither_previousQGStatus_nor_qualityGate() {
    Configuration configuration = mock(Configuration.class);
    QGChangeEvent qualityGateEvent = newQGChangeEvent(configuration, null, null);
    mockWebhookEnabled(qualityGateEvent.getProject());

    underTest.onIssueChanges(qualityGateEvent, CHANGED_ISSUES_ARE_IGNORED);

    verifyZeroInteractions(webhookPayloadFactory, mockedDbClient);
  }

  @Test
  public void onIssueChanges_has_no_effect_if_event_has_same_status_in_previous_and_new_QG() {
    Configuration configuration = mock(Configuration.class);
    Metric.Level previousStatus = randomLevel();
    when(newQualityGate.getStatus()).thenReturn(previousStatus);
    QGChangeEvent qualityGateEvent = newQGChangeEvent(configuration, previousStatus, newQualityGate);
    mockWebhookEnabled(qualityGateEvent.getProject());

    underTest.onIssueChanges(qualityGateEvent, CHANGED_ISSUES_ARE_IGNORED);

    verifyZeroInteractions(webhookPayloadFactory, mockedDbClient);
  }

  @Test
  @UseDataProvider("newQGorNot")
  public void onIssueChanges_calls_webhook_for_changeEvent_with_webhook_enabled(@Nullable EvaluatedQualityGate newQualityGate) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentAndBranch branch = insertProjectBranch(project, BranchType.SHORT, "foo");
    SnapshotDto analysis = insertAnalysisTask(branch);
    Configuration configuration = mock(Configuration.class);
    mockPayloadSupplierConsumedByWebhooks();
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.analysis.test1", randomAlphanumeric(50));
    properties.put("sonar.analysis.test2", randomAlphanumeric(5000));
    insertPropertiesFor(analysis.getUuid(), properties);
    QGChangeEvent qualityGateEvent = newQGChangeEvent(branch, analysis, configuration, newQualityGate);
    mockWebhookEnabled(qualityGateEvent.getProject());

    underTest.onIssueChanges(qualityGateEvent, CHANGED_ISSUES_ARE_IGNORED);

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, analysis, qualityGateEvent.getProject());
    assertThat(projectAnalysis).isEqualTo(
      new ProjectAnalysis(
        new Project(project.uuid(), project.getKey(), project.name()),
        null,
        new Analysis(analysis.getUuid(), analysis.getCreatedAt()),
        new Branch(false, "foo", Branch.Type.SHORT),
        newQualityGate,
        null,
        properties));
  }

  @Test
  @UseDataProvider("newQGorNot")
  public void onIssueChanges_calls_webhook_on_main_branch(@Nullable EvaluatedQualityGate newQualityGate) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch mainBranch = insertMainBranch(organization);
    SnapshotDto analysis = insertAnalysisTask(mainBranch);
    Configuration configuration = mock(Configuration.class);
    QGChangeEvent qualityGateEvent = newQGChangeEvent(mainBranch, analysis, configuration, newQualityGate);
    mockWebhookEnabled(qualityGateEvent.getProject());

    underTest.onIssueChanges(qualityGateEvent, CHANGED_ISSUES_ARE_IGNORED);

    verifyWebhookCalled(mainBranch, analysis, qualityGateEvent.getProject());
  }

  @Test
  public void onIssueChanges_calls_webhook_on_long_branch() {
    onIssueChangesCallsWebhookOnBranch(BranchType.LONG);
  }

  @Test
  public void onIssueChanges_calls_webhook_on_short_branch() {
    onIssueChangesCallsWebhookOnBranch(SHORT);
  }

  public void onIssueChangesCallsWebhookOnBranch(BranchType branchType) {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch mainBranch = insertMainBranch(organization);
    ComponentAndBranch longBranch = insertProjectBranch(mainBranch.component, branchType, "foo");
    SnapshotDto analysis = insertAnalysisTask(longBranch);
    Configuration configuration = mock(Configuration.class);
    QGChangeEvent qualityGateEvent = newQGChangeEvent(longBranch, analysis, configuration, null);
    mockWebhookEnabled(qualityGateEvent.getProject());

    underTest.onIssueChanges(qualityGateEvent, CHANGED_ISSUES_ARE_IGNORED);

    verifyWebhookCalled(longBranch, analysis, qualityGateEvent.getProject());
  }

  @DataProvider
  public static Object[][] newQGorNot() {
    EvaluatedQualityGate newQualityGate = mock(EvaluatedQualityGate.class);
    return new Object[][] {
      {null},
      {newQualityGate}
    };
  }

  private void mockWebhookEnabled(ComponentDto... projects) {
    for (ComponentDto dto : projects) {
      when(webHooks.isEnabled(dto)).thenReturn(true);
    }
  }

  private void mockWebhookDisabled(ComponentDto... projects) {
    for (ComponentDto dto : projects) {
      when(webHooks.isEnabled(dto)).thenReturn(false);
    }
  }

  private void mockPayloadSupplierConsumedByWebhooks() {
    Mockito.doAnswer(invocationOnMock -> {
      Supplier<WebhookPayload> supplier = (Supplier<WebhookPayload>) invocationOnMock.getArguments()[1];
      supplier.get();
      return null;
    }).when(webHooks)
      .sendProjectAnalysisUpdate(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  private void insertPropertiesFor(String snapshotUuid, Map<String, String> properties) {
    List<AnalysisPropertyDto> analysisProperties = properties.entrySet().stream()
      .map(entry -> new AnalysisPropertyDto()
        .setUuid(UuidFactoryFast.getInstance().create())
        .setSnapshotUuid(snapshotUuid)
        .setKey(entry.getKey())
        .setValue(entry.getValue()))
      .collect(toArrayList(properties.size()));
    dbTester.getDbClient().analysisPropertiesDao().insert(dbTester.getSession(), analysisProperties);
    dbTester.getSession().commit();
  }

  private SnapshotDto insertAnalysisTask(ComponentAndBranch componentAndBranch) {
    return dbTester.components().insertSnapshot(componentAndBranch.component);
  }

  private ProjectAnalysis verifyWebhookCalledAndExtractPayloadFactoryArgument(ComponentAndBranch componentAndBranch, SnapshotDto analysis, ComponentDto project) {
    verifyWebhookCalled(componentAndBranch, analysis, project);

    return extractPayloadFactoryArguments(1).iterator().next();
  }

  private void verifyWebhookCalled(ComponentAndBranch componentAndBranch, SnapshotDto analysis, ComponentDto project) {
    verify(webHooks).isEnabled(project);
    verify(webHooks).sendProjectAnalysisUpdate(
      eq(new WebHooks.Analysis(componentAndBranch.uuid(), analysis.getUuid(), null)),
      any());
  }

  private List<ProjectAnalysis> extractPayloadFactoryArguments(int time) {
    ArgumentCaptor<ProjectAnalysis> projectAnalysisCaptor = ArgumentCaptor.forClass(ProjectAnalysis.class);
    verify(webhookPayloadFactory, Mockito.times(time)).create(projectAnalysisCaptor.capture());
    return projectAnalysisCaptor.getAllValues();
  }

  public ComponentAndBranch insertMainBranch(OrganizationDto organization) {
    ComponentDto project = newPrivateProjectDto(organization);
    BranchDto branch = newBranchDto(project, LONG).setKey("master");
    dbTester.components().insertComponent(project);
    dbClient.branchDao().insert(dbTester.getSession(), branch);
    dbTester.commit();
    return new ComponentAndBranch(project, branch);
  }

  public ComponentAndBranch insertProjectBranch(ComponentDto project, BranchType type, String branchKey) {
    BranchDto branchDto = newBranchDto(project.projectUuid(), type).setKey(branchKey);
    ComponentDto newComponent = dbTester.components().insertProjectBranch(project, branchDto);
    return new ComponentAndBranch(newComponent, branchDto);
  }

  private static class ComponentAndBranch {
    private final ComponentDto component;

    private final BranchDto branch;

    private ComponentAndBranch(ComponentDto component, BranchDto branch) {
      this.component = component;
      this.branch = branch;
    }

    public ComponentDto getComponent() {
      return component;
    }

    public BranchDto getBranch() {
      return branch;
    }

    public String uuid() {
      return component.uuid();
    }

  }

  private static QGChangeEvent newQGChangeEvent(Configuration configuration, @Nullable Metric.Level previousQQStatus, @Nullable EvaluatedQualityGate evaluatedQualityGate) {
    return new QGChangeEvent(new ComponentDto(), new BranchDto(), new SnapshotDto(), configuration, previousQQStatus, () -> Optional.ofNullable(evaluatedQualityGate));
  }

  private static QGChangeEvent newQGChangeEvent(ComponentAndBranch branch, SnapshotDto analysis, Configuration configuration, @Nullable EvaluatedQualityGate evaluatedQualityGate) {
    Metric.Level previousStatus = randomLevel();
    if (evaluatedQualityGate != null) {
      Metric.Level otherLevel = stream(Metric.Level.values())
        .filter(s -> s != previousStatus)
        .toArray(Metric.Level[]::new)[new Random().nextInt(Metric.Level.values().length - 1)];
      when(evaluatedQualityGate.getStatus()).thenReturn(otherLevel);
    }
    return new QGChangeEvent(branch.component, branch.branch, analysis, configuration, previousStatus, () -> Optional.ofNullable(evaluatedQualityGate));
  }

  private static Metric.Level randomLevel() {
    return Metric.Level.values()[new Random().nextInt(Metric.Level.values().length)];
  }

}
