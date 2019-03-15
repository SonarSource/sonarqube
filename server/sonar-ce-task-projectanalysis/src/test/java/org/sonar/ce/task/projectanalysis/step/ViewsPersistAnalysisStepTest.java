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
package org.sonar.ce.task.projectanalysis.step;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.projectanalysis.period.Period;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DATE;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.ComponentTesting.newSubView;
import static org.sonar.db.component.ComponentTesting.newView;

public class ViewsPersistAnalysisStepTest extends BaseStepTest {

  private static final String ANALYSIS_UUID = "U1";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();

  private System2 system2 = mock(System2.class);
  private DbClient dbClient = dbTester.getDbClient();
  private long analysisDate;
  private long now;
  private PersistAnalysisStep underTest;

  @Before
  public void setup() {
    analysisDate = DateUtils.parseDateQuietly("2015-06-01").getTime();
    analysisMetadataHolder.setUuid(ANALYSIS_UUID);
    analysisMetadataHolder.setAnalysisDate(analysisDate);

    now = DateUtils.parseDateQuietly("2015-06-02").getTime();

    when(system2.now()).thenReturn(now);

    underTest = new PersistAnalysisStep(system2, dbClient, treeRootHolder, analysisMetadataHolder, periodsHolder);

    // initialize PeriodHolder to empty by default
    periodsHolder.setPeriod(null);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void persist_analysis() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto viewDto = save(newView(organizationDto, "UUID_VIEW").setDbKey("KEY_VIEW"));
    save(newSubView(viewDto, "UUID_SUBVIEW", "KEY_SUBVIEW"));
    save(newPrivateProjectDto(organizationDto, "proj"));
    dbTester.getSession().commit();

    Component projectView = ViewsComponent.builder(PROJECT_VIEW, "KEY_PROJECT_COPY").setUuid("UUID_PROJECT_COPY").build();
    Component subView = ViewsComponent.builder(SUBVIEW, "KEY_SUBVIEW").setUuid("UUID_SUBVIEW").addChildren(projectView).build();
    Component view = ViewsComponent.builder(VIEW, "KEY_VIEW").setUuid("UUID_VIEW").addChildren(subView).build();
    treeRootHolder.setRoot(view);

    underTest.execute(new TestComputationStepContext());

    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(1);

    SnapshotDto viewSnapshot = getUnprocessedSnapshot(viewDto.uuid());
    assertThat(viewSnapshot.getUuid()).isEqualTo(ANALYSIS_UUID);
    assertThat(viewSnapshot.getComponentUuid()).isEqualTo(view.getUuid());
    assertThat(viewSnapshot.getProjectVersion()).isNull();
    assertThat(viewSnapshot.getLast()).isFalse();
    assertThat(viewSnapshot.getStatus()).isEqualTo("U");
    assertThat(viewSnapshot.getCreatedAt()).isEqualTo(analysisDate);
    assertThat(viewSnapshot.getBuildDate()).isEqualTo(now);
  }

  @Test
  public void persist_snapshots_with_leak_period() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto viewDto = save(newView(organizationDto, "UUID_VIEW").setDbKey("KEY_VIEW"));
    ComponentDto subViewDto = save(newSubView(viewDto, "UUID_SUBVIEW", "KEY_SUBVIEW"));
    dbTester.getSession().commit();

    Component subView = ViewsComponent.builder(SUBVIEW, "KEY_SUBVIEW").setUuid("UUID_SUBVIEW").build();
    Component view = ViewsComponent.builder(VIEW, "KEY_VIEW").setUuid("UUID_VIEW").addChildren(subView).build();
    treeRootHolder.setRoot(view);

    periodsHolder.setPeriod(new Period(LEAK_PERIOD_MODE_DATE, "2015-01-01", analysisDate, "u1"));

    underTest.execute(new TestComputationStepContext());

    SnapshotDto viewSnapshot = getUnprocessedSnapshot(viewDto.uuid());
    assertThat(viewSnapshot.getPeriodMode()).isEqualTo(LEAK_PERIOD_MODE_DATE);
    assertThat(viewSnapshot.getPeriodDate()).isEqualTo(analysisDate);
    assertThat(viewSnapshot.getPeriodModeParameter()).isNotNull();
  }

  private ComponentDto save(ComponentDto componentDto) {
    dbClient.componentDao().insert(dbTester.getSession(), componentDto);
    return componentDto;
  }

  private SnapshotDto getUnprocessedSnapshot(String componentUuid) {
    List<SnapshotDto> projectSnapshots = dbClient.snapshotDao().selectAnalysesByQuery(dbTester.getSession(),
      new SnapshotQuery().setComponentUuid(componentUuid).setIsLast(false).setStatus(SnapshotDto.STATUS_UNPROCESSED));
    assertThat(projectSnapshots).hasSize(1);
    return projectSnapshots.get(0);
  }

}
