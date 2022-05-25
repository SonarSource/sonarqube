/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.measure.live;

import java.util.Date;
import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.metric.MetricDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REVIEW_RATING_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REVIEW_RATING_KEY;

public class HotspotMeasureUpdaterTest {
  @Rule
  public DbTester db = DbTester.create();

  private final HotspotMeasureUpdater hotspotMeasureUpdater = new HotspotMeasureUpdater(db.getDbClient());
  private ComponentIndexImpl componentIndex;
  private MeasureMatrix matrix;
  private ComponentDto project;
  private ComponentDto dir;
  private ComponentDto file1;
  private ComponentDto file2;

  @Before
  public void setUp() {
    // insert project and file structure
    project = db.components().insertPrivateProject();
    dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java"));
    file1 = db.components().insertComponent(ComponentTesting.newFileDto(project, dir));
    file2 = db.components().insertComponent(ComponentTesting.newFileDto(project, dir));

    // other needed data
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), insertHotspotMetrics(), List.of());
    componentIndex = new ComponentIndexImpl(db.getDbClient());
  }

  private List<MetricDto> insertHotspotMetrics() {
    return List.of(
      db.measures().insertMetric(m -> m.setKey(SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY)),
      db.measures().insertMetric(m -> m.setKey(SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY)),
      db.measures().insertMetric(m -> m.setKey(SECURITY_HOTSPOTS_REVIEWED_KEY)),
      db.measures().insertMetric(m -> m.setKey(SECURITY_REVIEW_RATING_KEY)),

      db.measures().insertMetric(m -> m.setKey(NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY)),
      db.measures().insertMetric(m -> m.setKey(NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY)),
      db.measures().insertMetric(m -> m.setKey(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY)),
      db.measures().insertMetric(m -> m.setKey(NEW_SECURITY_REVIEW_RATING_KEY))
    );
  }

  @Test
  public void should_count_hotspots_for_root() {
    db.issues().insertHotspot(project, file1, i -> i.setIssueCreationDate(new Date(999)).setStatus("TO_REVIEW"));
    db.issues().insertHotspot(project, file1, i -> i.setIssueCreationDate(new Date(999)).setStatus("REVIEWED"));

    db.issues().insertHotspot(project, dir, i -> i.setIssueCreationDate(new Date(1001)).setStatus("TO_REVIEW"));
    db.issues().insertHotspot(project, file1, i -> i.setIssueCreationDate(new Date(1002)).setStatus("REVIEWED"));
    db.issues().insertHotspot(project, file1, i -> i.setIssueCreationDate(new Date(1003)).setStatus("REVIEWED"));

    componentIndex.load(db.getSession(), List.of(file1));
    hotspotMeasureUpdater.apply(db.getSession(), matrix, componentIndex, true, 1000L);

    assertThat(matrix.getMeasure(project, SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY).get().getValue()).isEqualTo(3d);
    assertThat(matrix.getMeasure(project, SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY).get().getValue()).isEqualTo(2d);
    assertThat(matrix.getMeasure(project, SECURITY_REVIEW_RATING_KEY).get().getDataAsString()).isEqualTo("C");
    assertThat(matrix.getMeasure(project, SECURITY_HOTSPOTS_REVIEWED_KEY).get().getValue()).isEqualTo(60d);

    assertThat(matrix.getMeasure(project, NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY).get().getVariation()).isEqualTo(2d);
    assertThat(matrix.getMeasure(project, NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY).get().getVariation()).isEqualTo(1d);
    assertThat(matrix.getMeasure(project, NEW_SECURITY_REVIEW_RATING_KEY).get().getVariation()).isEqualTo(3);
    assertThat(matrix.getMeasure(project, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY).get().getVariation()).isCloseTo(66d, Offset.offset(1d));
  }

  @Test
  public void dont_create_leak_measures_if_no_leak_period() {
    db.issues().insertHotspot(project, dir, i -> i.setIssueCreationDate(new Date(1001)).setStatus("TO_REVIEW"));
    db.issues().insertHotspot(project, file1, i -> i.setIssueCreationDate(new Date(1002)).setStatus("REVIEWED"));
    db.issues().insertHotspot(project, file1, i -> i.setIssueCreationDate(new Date(1003)).setStatus("REVIEWED"));

    componentIndex.load(db.getSession(), List.of(file1));
    hotspotMeasureUpdater.apply(db.getSession(), matrix, componentIndex, false, 1000L);

    assertThat(matrix.getMeasure(project, NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY)).isEmpty();
    assertThat(matrix.getMeasure(project, NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY)).isEmpty();
    assertThat(matrix.getMeasure(project, NEW_SECURITY_REVIEW_RATING_KEY)).isEmpty();
    assertThat(matrix.getMeasure(project, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY)).isEmpty();
  }
}
