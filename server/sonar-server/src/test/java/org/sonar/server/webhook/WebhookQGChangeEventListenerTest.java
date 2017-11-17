/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
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
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.ShortLivingBranchQualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.Trigger;

import static java.lang.String.valueOf;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.stream.MoreCollectors.toArrayList;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

public class WebhookQGChangeEventListenerTest {

  private static final EvaluatedQualityGate EVALUATED_QUALITY_GATE_1 = EvaluatedQualityGate.newBuilder()
    .setQualityGate(new QualityGate(valueOf(ShortLivingBranchQualityGate.ID), ShortLivingBranchQualityGate.NAME, emptySet()))
    .setStatus(EvaluatedQualityGate.Status.OK)
    .build();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = dbTester.getDbClient();

  private WebHooks webHooks = mock(WebHooks.class);
  private WebhookPayloadFactory webhookPayloadFactory = mock(WebhookPayloadFactory.class);
  private DbClient spiedOnDbClient = Mockito.spy(dbClient);
  private WebhookQGChangeEventListener underTest = new WebhookQGChangeEventListener(webHooks, webhookPayloadFactory, spiedOnDbClient);
  private DbClient mockedDbClient = mock(DbClient.class);
  private WebhookQGChangeEventListener mockedUnderTest = new WebhookQGChangeEventListener(webHooks, webhookPayloadFactory, mockedDbClient);

  @Test
  public void onChanges_has_no_effect_if_changeEvents_is_empty() {
    mockedUnderTest.onChanges(Trigger.ISSUE_CHANGE, Collections.emptyList());

    verifyZeroInteractions(webHooks, webhookPayloadFactory, mockedDbClient);
  }

  @Test
  public void onChanges_has_no_effect_if_no_webhook_is_configured() {
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    mockWebhookDisabled(configuration1, configuration2);

    mockedUnderTest.onChanges(Trigger.ISSUE_CHANGE, ImmutableList.of(
      new QGChangeEvent(new ComponentDto(), new BranchDto(), new SnapshotDto(), configuration1, Optional::empty),
      new QGChangeEvent(new ComponentDto(), new BranchDto(), new SnapshotDto(), configuration2, Optional::empty)));

    verify(webHooks).isEnabled(configuration1);
    verify(webHooks).isEnabled(configuration2);
    verifyZeroInteractions(webhookPayloadFactory, mockedDbClient);
  }

  @Test
  public void onChanges_calls_webhook_for_changeEvent_with_webhook_enabled() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentAndBranch branch = insertProjectBranch(project, BranchType.SHORT, "foo");
    SnapshotDto analysis = insertAnalysisTask(branch);
    Configuration configuration = mock(Configuration.class);
    mockWebhookEnabled(configuration);
    mockPayloadSupplierConsumedByWebhooks();
    Map<String, String> properties = new HashMap<>();
    properties.put("sonar.analysis.test1", randomAlphanumeric(50));
    properties.put("sonar.analysis.test2", randomAlphanumeric(5000));
    insertPropertiesFor(analysis.getUuid(), properties);

    underTest.onChanges(Trigger.ISSUE_CHANGE, singletonList(newQGChangeEvent(branch, analysis, configuration, EVALUATED_QUALITY_GATE_1)));

    ProjectAnalysis projectAnalysis = verifyWebhookCalledAndExtractPayloadFactoryArgument(branch, configuration, analysis);
    assertThat(projectAnalysis).isEqualTo(
      new ProjectAnalysis(
        new Project(project.uuid(), project.getKey(), project.name()),
        null,
        new Analysis(analysis.getUuid(), analysis.getCreatedAt()),
        new Branch(false, "foo", Branch.Type.SHORT),
        EVALUATED_QUALITY_GATE_1,
        null,
        properties));
  }

  @Test
  public void onChanges_does_not_call_webhook_if_disabled_for_QGChangeEvent() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPublicProject(organization);
    ComponentAndBranch branch1 = insertProjectBranch(project, BranchType.SHORT, "foo");
    ComponentAndBranch branch2 = insertProjectBranch(project, BranchType.SHORT, "bar");
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    SnapshotDto analysis2 = insertAnalysisTask(branch2);
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    mockWebhookDisabled(configuration1);
    mockWebhookEnabled(configuration2);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChanges(
      Trigger.ISSUE_CHANGE,
      ImmutableList.of(
        newQGChangeEvent(branch1, analysis1, configuration1, null),
        newQGChangeEvent(branch2, analysis2, configuration2, EVALUATED_QUALITY_GATE_1)));

    verifyWebhookNotCalled(branch1, analysis1, configuration1);
    verifyWebhookCalled(branch2, analysis2, configuration2);
  }

  @Test
  public void onChanges_calls_webhook_for_any_type_of_branch() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch mainBranch = insertMainBranch(organization);
    ComponentAndBranch longBranch = insertProjectBranch(mainBranch.component, BranchType.LONG, "foo");
    SnapshotDto analysis1 = insertAnalysisTask(mainBranch);
    SnapshotDto analysis2 = insertAnalysisTask(longBranch);
    Configuration configuration1 = mock(Configuration.class);
    Configuration configuration2 = mock(Configuration.class);
    mockWebhookEnabled(configuration1, configuration2);

    underTest.onChanges(Trigger.ISSUE_CHANGE, ImmutableList.of(
      newQGChangeEvent(mainBranch, analysis1, configuration1, EVALUATED_QUALITY_GATE_1),
      newQGChangeEvent(longBranch, analysis2, configuration2, null)));

    verifyWebhookCalled(mainBranch, analysis1, configuration1);
    verifyWebhookCalled(longBranch, analysis2, configuration2);
  }

  @Test
  public void onChanges_calls_webhook_once_per_QGChangeEvent_even_for_same_branch_and_configuration() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentAndBranch branch1 = insertPrivateBranch(organization, BranchType.SHORT);
    SnapshotDto analysis1 = insertAnalysisTask(branch1);
    Configuration configuration1 = mock(Configuration.class);
    mockWebhookEnabled(configuration1);
    mockPayloadSupplierConsumedByWebhooks();

    underTest.onChanges(Trigger.ISSUE_CHANGE, ImmutableList.of(
      newQGChangeEvent(branch1, analysis1, configuration1, null),
      newQGChangeEvent(branch1, analysis1, configuration1, EVALUATED_QUALITY_GATE_1),
      newQGChangeEvent(branch1, analysis1, configuration1, null)));

    verify(webHooks, times(3)).isEnabled(configuration1);
    verify(webHooks, times(3)).sendProjectAnalysisUpdate(
      Matchers.same(configuration1),
      Matchers.eq(new WebHooks.Analysis(branch1.uuid(), analysis1.getUuid(), null)),
      any(Supplier.class));
    extractPayloadFactoryArguments(3);
  }

  private void mockWebhookEnabled(Configuration... configurations) {
    for (Configuration configuration : configurations) {
      when(webHooks.isEnabled(configuration)).thenReturn(true);
    }
  }

  private void mockWebhookDisabled(Configuration... configurations) {
    for (Configuration configuration : configurations) {
      when(webHooks.isEnabled(configuration)).thenReturn(false);
    }
  }

  private void mockPayloadSupplierConsumedByWebhooks() {
    Mockito.doAnswer(invocationOnMock -> {
      Supplier<WebhookPayload> supplier = (Supplier<WebhookPayload>) invocationOnMock.getArguments()[2];
      supplier.get();
      return null;
    }).when(webHooks)
      .sendProjectAnalysisUpdate(Matchers.any(Configuration.class), Matchers.any(), Matchers.any());
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

  private ProjectAnalysis verifyWebhookCalledAndExtractPayloadFactoryArgument(ComponentAndBranch componentAndBranch, Configuration configuration, SnapshotDto analysis) {
    verifyWebhookCalled(componentAndBranch, analysis, configuration);

    return extractPayloadFactoryArguments(1).iterator().next();
  }

  private void verifyWebhookCalled(ComponentAndBranch componentAndBranch, SnapshotDto analysis, Configuration branchConfiguration) {
    verify(webHooks).isEnabled(branchConfiguration);
    verify(webHooks).sendProjectAnalysisUpdate(
      Matchers.same(branchConfiguration),
      Matchers.eq(new WebHooks.Analysis(componentAndBranch.uuid(), analysis.getUuid(), null)),
      any(Supplier.class));
  }

  private void verifyWebhookNotCalled(ComponentAndBranch componentAndBranch, SnapshotDto analysis, Configuration branchConfiguration) {
    verify(webHooks).isEnabled(branchConfiguration);
    verify(webHooks, times(0)).sendProjectAnalysisUpdate(
      Matchers.same(branchConfiguration),
      Matchers.eq(new WebHooks.Analysis(componentAndBranch.uuid(), analysis.getUuid(), null)),
      any(Supplier.class));
  }

  private List<ProjectAnalysis> extractPayloadFactoryArguments(int time) {
    ArgumentCaptor<ProjectAnalysis> projectAnalysisCaptor = ArgumentCaptor.forClass(ProjectAnalysis.class);
    verify(webhookPayloadFactory, Mockito.times(time)).create(projectAnalysisCaptor.capture());
    return projectAnalysisCaptor.getAllValues();
  }

  private ComponentAndBranch insertPrivateBranch(OrganizationDto organization, BranchType branchType) {
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    BranchDto branchDto = newBranchDto(project.projectUuid(), branchType)
      .setKey("foo");
    ComponentDto newComponent = dbTester.components().insertProjectBranch(project, branchDto);
    return new ComponentAndBranch(newComponent, branchDto);
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

  private static QGChangeEvent newQGChangeEvent(ComponentAndBranch branch, SnapshotDto analysis, Configuration configuration, @Nullable EvaluatedQualityGate evaluatedQualityGate) {
    return new QGChangeEvent(branch.component, branch.branch, analysis, configuration, () -> Optional.ofNullable(evaluatedQualityGate));
  }

}
