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
package org.sonar.ce.task.projectanalysis.purge;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Predicate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.MutableDisabledComponentsHolder;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.projectanalysis.step.BaseStepTest;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.server.project.Project;
import org.sonar.ce.task.projectanalysis.util.WrapInSingleElementArray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class PurgeDatastoresStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String PROJECT_UUID = "UUID-1234";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private ProjectCleaner projectCleaner = mock(ProjectCleaner.class);
  private ConfigurationRepository settingsRepository = mock(ConfigurationRepository.class);
  private MutableDisabledComponentsHolder disabledComponentsHolder = mock(MutableDisabledComponentsHolder.class, RETURNS_DEEP_STUBS);

  private PurgeDatastoresStep underTest = new PurgeDatastoresStep(mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS), projectCleaner, treeRootHolder,
    settingsRepository, disabledComponentsHolder, analysisMetadataHolder);

  @Before
  public void before() {
    analysisMetadataHolder.setProject(new Project("uuid", "key", "name", null, Collections.emptyList()));
  }

  @Test
  public void call_purge_method_of_the_purge_task_for_project() {
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).build();

    verify_call_purge_method_of_the_purge_task(project);
  }

  @Test
  public void call_purge_method_of_the_purge_task_for_view() {
    Component project = ViewsComponent.builder(Component.Type.VIEW, PROJECT_KEY).setUuid(PROJECT_UUID).build();

    verify_call_purge_method_of_the_purge_task(project);
  }

  @DataProvider
  public static Object[][] nonRootProjectComponentTypes() {
    return dataproviderFromComponentTypeValues(input -> input.isReportType() && input != Component.Type.PROJECT);
  }

  @Test
  @UseDataProvider("nonRootProjectComponentTypes")
  public void do_not_call_purge_method_of_the_purge_task_for_other_report_components(Component.Type type) {
    Component component = ReportComponent.builder(type, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).build();

    verify_do_not_call_purge_method_of_the_purge_task(component);
  }

  @DataProvider
  public static Object[][] nonRootViewsComponentTypes() {
    return dataproviderFromComponentTypeValues(input -> input.isViewsType() && input != Component.Type.VIEW);
  }

  @Test
  @UseDataProvider("nonRootViewsComponentTypes")
  public void do_not_call_purge_method_of_the_purge_task_for_other_views_components(Component.Type type) {
    Component component = ViewsComponent.builder(type, PROJECT_KEY).setUuid(PROJECT_UUID).build();

    verify_do_not_call_purge_method_of_the_purge_task(component);
  }

  private void verify_do_not_call_purge_method_of_the_purge_task(Component component) {
    treeRootHolder.setRoot(component);

    underTest.execute(new TestComputationStepContext());

    verifyNoMoreInteractions(projectCleaner);
  }

  private void verify_call_purge_method_of_the_purge_task(Component project) {
    treeRootHolder.setRoot(project);
    when(settingsRepository.getConfiguration()).thenReturn(new MapSettings().asConfig());

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    verify(projectCleaner).purge(any(), argumentCaptor.capture(), anyString(), any(), any());
    assertThat(argumentCaptor.getValue()).isEqualTo(PROJECT_UUID);
  }

  private static Object[][] dataproviderFromComponentTypeValues(Predicate<Component.Type> predicate) {
    return Arrays.stream(Component.Type.values())
      .filter(predicate)
      .map(WrapInSingleElementArray.INSTANCE)
      .toArray(Object[][]::new);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

}
