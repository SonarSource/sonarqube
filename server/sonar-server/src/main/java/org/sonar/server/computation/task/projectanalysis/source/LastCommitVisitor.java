/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.source;

import com.google.common.base.Optional;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.PathAwareVisitorAdapter;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepository;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepository;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfo;
import org.sonar.server.computation.task.projectanalysis.scm.ScmInfoRepository;

import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class LastCommitVisitor extends PathAwareVisitorAdapter<LastCommitVisitor.LastCommit> {

  private final MeasureRepository measureRepository;
  private final ScmInfoRepository scmInfoRepository;
  private final Metric lastCommitDateMetric;

  public LastCommitVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, ScmInfoRepository scmInfoRepository) {
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
    this.measureRepository = measureRepository;
    this.scmInfoRepository = scmInfoRepository;
    this.lastCommitDateMetric = metricRepository.getByKey(CoreMetrics.LAST_COMMIT_DATE_KEY);
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

    Optional<ScmInfo> scmInfoOptional = scmInfoRepository.getScmInfo(file);
    if (scmInfoOptional.isPresent()) {
      ScmInfo scmInfo = scmInfoOptional.get();
      path.current().addDate(scmInfo.getLatestChangeset().getDate());
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

      if (!path.isRoot()) {
        path.parent().addDate(maxDate);
      }
    }
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
