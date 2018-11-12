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

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbTester;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistAnalysisPropertiesStepTest {
  private static final String SNAPSHOT_UUID = randomAlphanumeric(40);
  private static final String SMALL_VALUE1 = randomAlphanumeric(50);
  private static final String SMALL_VALUE2 = randomAlphanumeric(50);
  private static final String SMALL_VALUE3 = randomAlphanumeric(50);
  private static final String BIG_VALUE = randomAlphanumeric(5000);
  private static final String VALUE_PREFIX_FOR_PR_PROPERTIES = "pr_";
  private static final List<ScannerReport.ContextProperty> PROPERTIES = Arrays.asList(
    newContextProperty("key1", "value1"),
    newContextProperty("key2", "value1"),
    newContextProperty("sonar.analysis", SMALL_VALUE1),
    newContextProperty("sonar.analysis.branch", SMALL_VALUE2),
    newContextProperty("sonar.analysis.empty_string", ""),
    newContextProperty("sonar.analysis.big_value", BIG_VALUE),
    newContextProperty("sonar.analysis.", SMALL_VALUE3),
    newContextProperty("sonar.pullrequest", VALUE_PREFIX_FOR_PR_PROPERTIES + SMALL_VALUE1),
    newContextProperty("sonar.pullrequest.branch", VALUE_PREFIX_FOR_PR_PROPERTIES + SMALL_VALUE2),
    newContextProperty("sonar.pullrequest.empty_string", ""),
    newContextProperty("sonar.pullrequest.big_value", VALUE_PREFIX_FOR_PR_PROPERTIES + BIG_VALUE),
    newContextProperty("sonar.pullrequest.", VALUE_PREFIX_FOR_PR_PROPERTIES + SMALL_VALUE3));
  private static final String SCM_REV_ID = "sha1";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private BatchReportReader batchReportReader = mock(BatchReportReader.class);
  private AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);
  private PersistAnalysisPropertiesStep underTest = new PersistAnalysisPropertiesStep(dbTester.getDbClient(), analysisMetadataHolder, batchReportReader,
    UuidFactoryFast.getInstance());

  @Test
  public void persist_should_stores_sonarDotAnalysisDot_and_sonarDotPullRequestDot_properties() {
    when(batchReportReader.readContextProperties()).thenReturn(CloseableIterator.from(PROPERTIES.iterator()));
    when(analysisMetadataHolder.getUuid()).thenReturn(SNAPSHOT_UUID);
    when(analysisMetadataHolder.getScmRevisionId()).thenReturn(Optional.of(SCM_REV_ID));

    underTest.execute(new TestComputationStepContext());

    assertThat(dbTester.countRowsOfTable("analysis_properties")).isEqualTo(9);
    List<AnalysisPropertyDto> propertyDtos = dbTester.getDbClient()
      .analysisPropertiesDao().selectBySnapshotUuid(dbTester.getSession(), SNAPSHOT_UUID);

    assertThat(propertyDtos)
      .extracting(AnalysisPropertyDto::getSnapshotUuid, AnalysisPropertyDto::getKey, AnalysisPropertyDto::getValue)
      .containsExactlyInAnyOrder(
        tuple(SNAPSHOT_UUID, "sonar.analysis.branch", SMALL_VALUE2),
        tuple(SNAPSHOT_UUID, "sonar.analysis.empty_string", ""),
        tuple(SNAPSHOT_UUID, "sonar.analysis.big_value", BIG_VALUE),
        tuple(SNAPSHOT_UUID, "sonar.analysis.", SMALL_VALUE3),
        tuple(SNAPSHOT_UUID, "sonar.analysis.scm_revision_id", SCM_REV_ID),
        tuple(SNAPSHOT_UUID, "sonar.pullrequest.branch", VALUE_PREFIX_FOR_PR_PROPERTIES + SMALL_VALUE2),
        tuple(SNAPSHOT_UUID, "sonar.pullrequest.empty_string", ""),
        tuple(SNAPSHOT_UUID, "sonar.pullrequest.big_value", VALUE_PREFIX_FOR_PR_PROPERTIES + BIG_VALUE),
        tuple(SNAPSHOT_UUID, "sonar.pullrequest.", VALUE_PREFIX_FOR_PR_PROPERTIES + SMALL_VALUE3));
  }

  @Test
  public void persist_should_not_stores_sonarDotAnalysisDotscm_revision_id_properties_when_its_not_available_in_report_metada() {
    when(batchReportReader.readContextProperties()).thenReturn(CloseableIterator.from(PROPERTIES.iterator()));
    when(analysisMetadataHolder.getUuid()).thenReturn(SNAPSHOT_UUID);
    when(analysisMetadataHolder.getScmRevisionId()).thenReturn(Optional.empty());

    underTest.execute(new TestComputationStepContext());

    assertThat(dbTester.countRowsOfTable("analysis_properties")).isEqualTo(8);
    List<AnalysisPropertyDto> propertyDtos = dbTester.getDbClient()
      .analysisPropertiesDao().selectBySnapshotUuid(dbTester.getSession(), SNAPSHOT_UUID);

    assertThat(propertyDtos)
      .extracting(AnalysisPropertyDto::getSnapshotUuid, AnalysisPropertyDto::getKey, AnalysisPropertyDto::getValue)
      .containsExactlyInAnyOrder(
        tuple(SNAPSHOT_UUID, "sonar.analysis.branch", SMALL_VALUE2),
        tuple(SNAPSHOT_UUID, "sonar.analysis.empty_string", ""),
        tuple(SNAPSHOT_UUID, "sonar.analysis.big_value", BIG_VALUE),
        tuple(SNAPSHOT_UUID, "sonar.analysis.", SMALL_VALUE3),
        tuple(SNAPSHOT_UUID, "sonar.pullrequest.branch", VALUE_PREFIX_FOR_PR_PROPERTIES + SMALL_VALUE2),
        tuple(SNAPSHOT_UUID, "sonar.pullrequest.empty_string", ""),
        tuple(SNAPSHOT_UUID, "sonar.pullrequest.big_value", VALUE_PREFIX_FOR_PR_PROPERTIES + BIG_VALUE),
        tuple(SNAPSHOT_UUID, "sonar.pullrequest.", VALUE_PREFIX_FOR_PR_PROPERTIES + SMALL_VALUE3));
  }

  @Test
  public void persist_filtering_of_properties_is_case_sensitive() {
    when(analysisMetadataHolder.getScmRevisionId()).thenReturn(Optional.of(SCM_REV_ID));
    when(batchReportReader.readContextProperties()).thenReturn(CloseableIterator.from(ImmutableList.of(
      newContextProperty("sonar.ANALYSIS.foo", "foo"),
      newContextProperty("sonar.anaLysis.bar", "bar"),
      newContextProperty("sonar.anaLYSIS.doo", "doh"),
      newContextProperty("sonar.PULLREQUEST.foo", "foo"),
      newContextProperty("sonar.pullRequest.bar", "bar"),
      newContextProperty("sonar.pullREQUEST.doo", "doh")).iterator()));
    when(analysisMetadataHolder.getUuid()).thenReturn(SNAPSHOT_UUID);

    underTest.execute(new TestComputationStepContext());

    assertThat(dbTester.countRowsOfTable("analysis_properties")).isEqualTo(1);
  }

  @Test
  public void persist_should_only_store_scmRevisionId_if_there_is_no_context_properties() {
    when(analysisMetadataHolder.getScmRevisionId()).thenReturn(Optional.of(SCM_REV_ID));
    when(batchReportReader.readContextProperties()).thenReturn(CloseableIterator.emptyCloseableIterator());
    when(analysisMetadataHolder.getUuid()).thenReturn(SNAPSHOT_UUID);

    underTest.execute(new TestComputationStepContext());

    assertThat(dbTester.countRowsOfTable("analysis_properties")).isEqualTo(1);
    List<AnalysisPropertyDto> propertyDtos = dbTester.getDbClient()
      .analysisPropertiesDao().selectBySnapshotUuid(dbTester.getSession(), SNAPSHOT_UUID);

    assertThat(propertyDtos)
      .extracting(AnalysisPropertyDto::getSnapshotUuid, AnalysisPropertyDto::getKey, AnalysisPropertyDto::getValue)
      .containsExactlyInAnyOrder(tuple(SNAPSHOT_UUID, "sonar.analysis.scm_revision_id", SCM_REV_ID));
  }

  @Test
  public void verify_description_value() {
    assertThat(underTest.getDescription()).isEqualTo("Persist analysis properties");
  }

  private static ScannerReport.ContextProperty newContextProperty(String key, String value) {
    return ScannerReport.ContextProperty.newBuilder()
      .setKey(key)
      .setValue(value)
      .build();
  }
}
