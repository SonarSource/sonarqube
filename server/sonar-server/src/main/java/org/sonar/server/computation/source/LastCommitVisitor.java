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
package org.sonar.server.computation.source;

import com.google.common.base.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

public class LastCommitVisitor extends PathAwareVisitorAdapter<LastCommitVisitor.LastCommit> {

  private static final long MILLISECONDS_PER_DAY = 1000L * 60 * 60 * 24;

  private final BatchReportReader reportReader;
  private final MeasureRepository measureRepository;
  private final Metric lastCommitDateMetric;
  private final Metric daysSinceLastCommitDateMetric;
  private final System2 system2;

  public LastCommitVisitor(BatchReportReader reportReader, MetricRepository metricRepository,
    MeasureRepository measureRepository, System2 system2) {
    super(CrawlerDepthLimit.LEAVES, POST_ORDER, new SimpleStackElementFactory<LastCommit>() {
      @Override
      public LastCommit createForAny(Component component) {
        return new LastCommit();
      }

      /** Stack item is not used at ProjectView level, saves on instantiating useless objects */
      @Override
      public LastCommit createForProjectView(Component projectView) {
        return null;
      }
    });
    this.reportReader = reportReader;
    this.measureRepository = measureRepository;
    this.system2 = system2;
    this.lastCommitDateMetric = metricRepository.getByKey(CoreMetrics.LAST_COMMIT_DATE_KEY);
    this.daysSinceLastCommitDateMetric = metricRepository.getByKey(CoreMetrics.DAYS_SINCE_LAST_COMMIT_KEY);
  }

  @Override
  public void visitProject(Component project, Path<LastCommit> path) {
    saveAndAggregate(project, path);
  }

  @Override
  public void visitDirectory(Component directory, Path<LastCommit> path) {
    saveAndAggregate(directory, path);
  }

  @Override
  public void visitModule(Component module, Path<LastCommit> path) {
    saveAndAggregate(module, path);
  }

  @Override
  public void visitFile(Component file, Path<LastCommit> path) {
    // load SCM blame information from report. It can be absent when the file was not touched
    // since previous analysis (optimization to decrease execution of blame commands). In this case
    // the date is loaded from database, as it did not change from previous analysis.
    BatchReport.Changesets changesets = reportReader.readChangesets(file.getReportAttributes().getRef());
    if (changesets == null) {
      Optional<Measure> baseMeasure = measureRepository.getBaseMeasure(file, lastCommitDateMetric);
      if (baseMeasure.isPresent()) {
        path.current().addDate(baseMeasure.get().getLongValue());
      }
    } else {
      for (BatchReport.Changesets.Changeset changeset : changesets.getChangesetList()) {
        if (changeset.hasDate()) {
          path.current().addDate(changeset.getDate());
        }
      }
    }
    saveAndAggregate(file, path);
  }

  @Override
  public void visitView(Component view, Path<LastCommit> path) {
    saveAndAggregate(view, path);
  }

  @Override
  public void visitSubView(Component subView, Path<LastCommit> path) {
    saveAndAggregate(subView, path);
  }

  @Override
  public void visitProjectView(Component projectView, Path<LastCommit> path) {
    Optional<Measure> rawMeasure = measureRepository.getRawMeasure(projectView, lastCommitDateMetric);
    if (rawMeasure.isPresent()) {
      // path.parent() should never fail as a project view must never be a root component
      path.parent().addDate(rawMeasure.get().getLongValue());
    }
  }

  private void saveAndAggregate(Component component, Path<LastCommit> path) {
    long maxDate = path.current().getDate();
    if (maxDate > 0L) {
      measureRepository.add(component, lastCommitDateMetric, Measure.newMeasureBuilder().create(maxDate));
      measureRepository.add(component, daysSinceLastCommitDateMetric, Measure.newMeasureBuilder().create(daysBetween(system2.now(), maxDate)));

      if (!path.isRoot()) {
        path.parent().addDate(maxDate);
      }
    }
  }

  private static int daysBetween(long d1, long d2) {
    // limitation of metric type: long is not supported yet, so casting to int
    return (int) (Math.abs(d1 - d2) / MILLISECONDS_PER_DAY);
  }

  public static final class LastCommit {
    private long date = 0;

    public void addDate(long l) {
      this.date = Math.max(this.date, l);
    }

    public long getDate() {
      return date;
    }
  }
}
