package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteSoftwareQualityRatingFromProjectMeasuresIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(DeleteSoftwareQualityRatingFromProjectMeasures.class);

  private final DeleteSoftwareQualityRatingFromProjectMeasures underTest = new DeleteSoftwareQualityRatingFromProjectMeasures(db.database());

  @Test
  void execute_whenMeasuresExists_shouldDeleteMeasures() throws SQLException {
    DeleteSoftwareQualityRatingFromProjectMeasures.SOFTWARE_QUALITY_METRICS_TO_DELETE.forEach(key -> {
      String metricUUid = insertMetric(key);
      insertProjectMeasure(metricUUid);
    });

    assertThat(db.countSql("select count(1) from project_measures;"))
      .isEqualTo(DeleteSoftwareQualityRatingFromProjectMeasures.SOFTWARE_QUALITY_METRICS_TO_DELETE.size());

    underTest.execute();

    assertThat(db.countSql("select count(1) from project_measures;")).isZero();
  }

  @Test
  void execute_whenOtherMeasuresExists_shouldNotDeleteMeasures() throws SQLException {
    String metricUUid = insertMetric("other_metric");
    insertProjectMeasure(metricUUid);

    assertThat(db.countSql("select count(1) from project_measures;")).isEqualTo(1);

    underTest.execute();

    assertThat(db.countSql("select count(1) from project_measures;")).isEqualTo(1);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    DeleteSoftwareQualityRatingFromProjectMeasures.SOFTWARE_QUALITY_METRICS_TO_DELETE.forEach(key -> {
      String metricUUid = insertMetric(key);
      insertProjectMeasure(metricUUid);
    });

    String metricUUid = insertMetric("other_metric");
    insertProjectMeasure(metricUUid);

    assertThat(db.countSql("select count(1) from project_measures;"))
      .isEqualTo(DeleteSoftwareQualityRatingFromProjectMeasures.SOFTWARE_QUALITY_METRICS_TO_DELETE.size() + 1);

    underTest.execute();
    underTest.execute();

    assertThat(db.countSql("select count(1) from project_measures;")).isOne();
  }

  private String insertMetric(String key) {
    String uuid = UuidFactoryImpl.INSTANCE.create();
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", uuid),
      Map.entry("NAME", key));
    db.executeInsert("metrics", map);
    return uuid;
  }

  private void insertProjectMeasure(String metricUuid) {
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", UuidFactoryImpl.INSTANCE.create()),
      Map.entry("METRIC_UUID", metricUuid),
      Map.entry("COMPONENT_UUID", UuidFactoryImpl.INSTANCE.create()),
      Map.entry("ANALYSIS_UUID", UuidFactoryImpl.INSTANCE.create()));
    db.executeInsert("project_measures", map);
  }
}
