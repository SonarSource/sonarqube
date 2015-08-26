/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.config.Settings;
import org.sonar.core.computation.dbcleaner.ProjectCleaner;
import org.sonar.db.DbSession;
import org.sonar.db.purge.IdUuidPair;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.MutableDbIdsRepositoryRule;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.SettingsRepository;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.db.DbClient;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class PurgeDatastoresStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final long PROJECT_ID = 123L;
  private static final String PROJECT_UUID = "UUID-1234";

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MutableDbIdsRepositoryRule dbIdsRepository = MutableDbIdsRepositoryRule.standalone();

  ProjectCleaner projectCleaner = mock(ProjectCleaner.class);
  SettingsRepository settingsRepository = mock(SettingsRepository.class);

  PurgeDatastoresStep underTest = new PurgeDatastoresStep(mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS), projectCleaner, dbIdsRepository, treeRootHolder, settingsRepository);

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
    return dataproviderFromComponentTypeValues(new Predicate<Component.Type>() {
      @Override
      public boolean apply(Component.Type input) {
        return input.isReportType() && input != Component.Type.PROJECT;
      }
    });
  }

  @Test
  @UseDataProvider("nonRootProjectComponentTypes")
  public void do_not_call_purge_method_of_the_purge_task_for_other_report_components(Component.Type type) {
    Component component = ReportComponent.builder(type, 1).setUuid(PROJECT_UUID).setKey(PROJECT_KEY).build();

    verify_do_not_call_purge_method_of_the_purge_task(component);
  }

  @DataProvider
  public static Object[][] nonRootViewsComponentTypes() {
    return dataproviderFromComponentTypeValues(new Predicate<Component.Type>() {
      @Override
      public boolean apply(Component.Type input) {
        return input.isViewsType() && input != Component.Type.VIEW;
      }
    });
  }

  @Test
  @UseDataProvider("nonRootViewsComponentTypes")
  public void do_not_call_purge_method_of_the_purge_task_for_other_views_components(Component.Type type) {
    Component component = ViewsComponent.builder(type, PROJECT_KEY).setUuid(PROJECT_UUID).build();

    verify_do_not_call_purge_method_of_the_purge_task(component);
  }

  private void verify_do_not_call_purge_method_of_the_purge_task(Component component) {
    treeRootHolder.setRoot(component);

    underTest.execute();

    verifyNoMoreInteractions(projectCleaner);
  }

  private void verify_call_purge_method_of_the_purge_task(Component project) {
    treeRootHolder.setRoot(project);
    when(settingsRepository.getSettings(project)).thenReturn(new Settings());
    dbIdsRepository.setComponentId(project, PROJECT_ID);

    underTest.execute();

    ArgumentCaptor<IdUuidPair> argumentCaptor = ArgumentCaptor.forClass(IdUuidPair.class);
    verify(projectCleaner).purge(any(DbSession.class), argumentCaptor.capture(), any(Settings.class));
    assertThat(argumentCaptor.getValue().getId()).isEqualTo(PROJECT_ID);
    assertThat(argumentCaptor.getValue().getUuid()).isEqualTo(PROJECT_UUID);
  }

  private static Object[][] dataproviderFromComponentTypeValues(Predicate<Component.Type> predicate) {
    return FluentIterable.from(asList(Component.Type.values()))
        .filter(predicate)
        .transform(new Function<Object, Object[]>() {
          @Nullable
          @Override
          public Object[] apply(Object input) {
            return new Object[]{input};
          }
        }).toArray(Object[].class);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
