package org.sonar.server.computation.measure;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import javax.annotation.Nonnull;
import org.sonar.api.measures.Metric;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.db.DbClient;

public class MeasureRepositoryImpl implements MeasureRepository {
  private final DbClient dbClient;
  private final BatchReportReader reportReader;

  public MeasureRepositoryImpl(DbClient dbClient, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
  }

  @Override
  public Optional<MeasureDto> findPrevious(Component component, Metric<?> metric) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return Optional.fromNullable(
        // TODO replace component.getKey() by ${link #getKey} as component.getKey() is only for project/module and does not take into
        // account usage of the branch
        dbClient.measureDao().findByComponentKeyAndMetricKey(dbSession, component.getKey(), metric.getKey())
        );
    }
  }

  @Override
  public Optional<BatchReport.Measure> findCurrent(Component component, final Metric<?> metric) {
    return Optional.fromNullable(Iterables.find(
        reportReader.readComponentMeasures(component.getRef()),
        new Predicate<BatchReport.Measure>() {
          @Override
          public boolean apply(@Nonnull BatchReport.Measure input) {
            return input.getMetricKey().equals(metric.getKey());
          }
        }
    ));
  }
}
