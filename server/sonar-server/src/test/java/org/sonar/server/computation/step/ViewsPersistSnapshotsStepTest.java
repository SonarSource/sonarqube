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

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.MapBasedDbIdsRepository;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;
import org.sonar.test.DbTests;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_DATE;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.db.component.SnapshotTesting.newSnapshotForProject;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.component.ComponentFunctions.toKey;

@Category(DbTests.class)
public class ViewsPersistSnapshotsStepTest extends BaseStepTest {

  private static final int PROJECT_KEY = 1;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  System2 system2 = mock(System2.class);

  MapBasedDbIdsRepository<String> dbIdsRepository = new MapBasedDbIdsRepository<>(toKey());

  DbClient dbClient = dbTester.getDbClient();

  long analysisDate;

  long now;

  PersistSnapshotsStep underTest;

  @Before
  public void setup() {
    dbTester.truncateTables();
    analysisDate = DateUtils.parseDateQuietly("2015-06-01").getTime();
    analysisMetadataHolder.setAnalysisDate(analysisDate);

    now = DateUtils.parseDateQuietly("2015-06-02").getTime();

    when(system2.now()).thenReturn(now);

    underTest = new PersistSnapshotsStep(system2, dbClient, treeRootHolder, analysisMetadataHolder, dbIdsRepository, periodsHolder);

    // initialize PeriodHolder to empty by default
    periodsHolder.setPeriods();
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void persist_snapshots() {
    ComponentDto projectDto = save(newProjectDto("proj"));
    ComponentDto viewDto = save(newView("ABCD").setKey(valueOf(PROJECT_KEY)).setName("Project"));
    ComponentDto subViewDto = save(newSubView(viewDto, "CDEF", "key").setKey("2"));
    ComponentDto projectViewDto = save(newProjectCopy("DEFG", projectDto, subViewDto).setKey("3"));
    dbTester.getSession().commit();

    Component projectView = ViewsComponent.builder(PROJECT_VIEW, 3).setUuid("DEFG").build();
    Component subView = ViewsComponent.builder(SUBVIEW, 2).setUuid("CDEF").addChildren(projectView).build();
    Component view = ViewsComponent.builder(VIEW, 1).setUuid("ABCD").addChildren(subView).build();
    treeRootHolder.setRoot(view);

    dbIdsRepository.setComponentId(view, viewDto.getId());
    dbIdsRepository.setComponentId(subView, subViewDto.getId());
    dbIdsRepository.setComponentId(projectView, projectViewDto.getId());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(3);

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(viewDto.getId());
    assertThat(projectSnapshot.getComponentId()).isEqualTo(viewDto.getId());
    assertThat(projectSnapshot.getRootProjectId()).isEqualTo(viewDto.getId());
    assertThat(projectSnapshot.getRootId()).isNull();
    assertThat(projectSnapshot.getParentId()).isNull();
    assertThat(projectSnapshot.getDepth()).isEqualTo(0);
    assertThat(projectSnapshot.getPath()).isNullOrEmpty();
    assertThat(projectSnapshot.getQualifier()).isEqualTo("VW");
    assertThat(projectSnapshot.getScope()).isEqualTo("PRJ");
    assertThat(projectSnapshot.getVersion()).isNull();
    assertThat(projectSnapshot.getLast()).isFalse();
    assertThat(projectSnapshot.getStatus()).isEqualTo("U");
    assertThat(projectSnapshot.getCreatedAt()).isEqualTo(analysisDate);
    assertThat(projectSnapshot.getBuildDate()).isEqualTo(now);

    SnapshotDto subViewSnapshot = getUnprocessedSnapshot(subViewDto.getId());
    assertThat(subViewSnapshot.getComponentId()).isEqualTo(subViewDto.getId());
    assertThat(subViewSnapshot.getRootProjectId()).isEqualTo(viewDto.getId());
    assertThat(subViewSnapshot.getRootId()).isEqualTo(projectSnapshot.getId());
    assertThat(subViewSnapshot.getParentId()).isEqualTo(projectSnapshot.getId());
    assertThat(subViewSnapshot.getDepth()).isEqualTo(1);
    assertThat(subViewSnapshot.getPath()).isEqualTo(projectSnapshot.getId() + ".");
    assertThat(subViewSnapshot.getQualifier()).isEqualTo("SVW");
    assertThat(subViewSnapshot.getScope()).isEqualTo("PRJ");
    assertThat(subViewSnapshot.getVersion()).isNull();
    assertThat(subViewSnapshot.getLast()).isFalse();
    assertThat(subViewSnapshot.getStatus()).isEqualTo("U");
    assertThat(subViewSnapshot.getCreatedAt()).isEqualTo(analysisDate);
    assertThat(subViewSnapshot.getBuildDate()).isEqualTo(now);

    SnapshotDto projectViewSnapshot = getUnprocessedSnapshot(projectViewDto.getId());
    assertThat(projectViewSnapshot.getComponentId()).isEqualTo(projectViewDto.getId());
    assertThat(projectViewSnapshot.getRootProjectId()).isEqualTo(viewDto.getId());
    assertThat(projectViewSnapshot.getRootId()).isEqualTo(projectSnapshot.getId());
    assertThat(projectViewSnapshot.getParentId()).isEqualTo(subViewSnapshot.getId());
    assertThat(projectViewSnapshot.getDepth()).isEqualTo(2);
    assertThat(projectViewSnapshot.getPath()).isEqualTo(projectSnapshot.getId() + "." + subViewSnapshot.getId() + ".");
    assertThat(projectViewSnapshot.getQualifier()).isEqualTo("TRK");
    assertThat(projectViewSnapshot.getScope()).isEqualTo("FIL");
    assertThat(projectViewSnapshot.getVersion()).isNull();
    assertThat(projectViewSnapshot.getLast()).isFalse();
    assertThat(projectViewSnapshot.getStatus()).isEqualTo("U");
    assertThat(projectViewSnapshot.getCreatedAt()).isEqualTo(analysisDate);
    assertThat(projectViewSnapshot.getBuildDate()).isEqualTo(now);

    assertThat(dbIdsRepository.getSnapshotId(view)).isEqualTo(projectSnapshot.getId());
    assertThat(dbIdsRepository.getComponentId(subView)).isEqualTo(subViewDto.getId());
    assertThat(dbIdsRepository.getComponentId(projectView)).isEqualTo(projectViewDto.getId());
  }

  @Test
  public void persist_snapshots_with_periods() {
    ComponentDto viewDto = save(newView("ABCD").setKey(valueOf(PROJECT_KEY)).setName("Project"));
    ComponentDto subViewDto = save(newSubView(viewDto, "CDEF", "key").setKey("2"));
    SnapshotDto viewSnapshotDto = save(newSnapshotForProject(viewDto).setCreatedAt(DateUtils.parseDateQuietly("2015-01-01").getTime()));
    SnapshotDto subViewSnapshotDto = save(newSnapshotForProject(subViewDto).setCreatedAt(DateUtils.parseDateQuietly("2015-01-01").getTime()));
    dbTester.getSession().commit();

    Component subView = ViewsComponent.builder(SUBVIEW, 2).setUuid("ABCD").build();
    Component view = ViewsComponent.builder(VIEW, PROJECT_KEY).setUuid("ABCD").addChildren(subView).build();
    treeRootHolder.setRoot(view);
    dbIdsRepository.setComponentId(view, viewDto.getId());
    dbIdsRepository.setComponentId(subView, subViewDto.getId());

    periodsHolder.setPeriods(new Period(1, TIMEMACHINE_MODE_DATE, "2015-01-01", analysisDate, 123L));

    underTest.execute();

    SnapshotDto viewSnapshot = getUnprocessedSnapshot(viewDto.getId());
    assertThat(viewSnapshot.getPeriodMode(1)).isEqualTo(TIMEMACHINE_MODE_DATE);
    assertThat(viewSnapshot.getPeriodDate(1)).isEqualTo(analysisDate);
    assertThat(viewSnapshot.getPeriodModeParameter(1)).isNotNull();

    SnapshotDto subViewSnapshot = getUnprocessedSnapshot(subViewDto.getId());
    assertThat(subViewSnapshot.getPeriodMode(1)).isEqualTo(TIMEMACHINE_MODE_DATE);
    assertThat(subViewSnapshot.getPeriodDate(1)).isEqualTo(analysisDate);
    assertThat(subViewSnapshot.getPeriodModeParameter(1)).isNotNull();
  }

  private ComponentDto save(ComponentDto componentDto) {
    dbClient.componentDao().insert(dbTester.getSession(), componentDto);
    return componentDto;
  }

  private SnapshotDto save(SnapshotDto snapshotDto) {
    dbClient.snapshotDao().insert(dbTester.getSession(), snapshotDto);
    return snapshotDto;
  }

  private SnapshotDto getUnprocessedSnapshot(long componentId) {
    List<SnapshotDto> projectSnapshots = dbClient.snapshotDao().selectSnapshotsByQuery(dbTester.getSession(),
      new SnapshotQuery().setComponentId(componentId).setIsLast(false).setStatus(SnapshotDto.STATUS_UNPROCESSED));
    assertThat(projectSnapshots).hasSize(1);
    return projectSnapshots.get(0);
  }

}
