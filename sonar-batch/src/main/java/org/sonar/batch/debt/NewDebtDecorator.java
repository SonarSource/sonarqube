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

package org.sonar.batch.debt;

import com.google.common.collect.ImmutableList;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.RequiresDB;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.components.Period;
import org.sonar.batch.components.TimeMachineConfiguration;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Decorator that computes the technical debt metric
 */
@RequiresDB
@DependsUpon(DecoratorBarriers.ISSUES_TRACKED)
public final class NewDebtDecorator implements Decorator {

  private final ResourcePerspectives perspectives;
  private final TimeMachineConfiguration timeMachineConfiguration;
  private final IssueChangelogDebtCalculator issueChangelogDebtCalculator;

  public NewDebtDecorator(ResourcePerspectives perspectives, TimeMachineConfiguration timeMachineConfiguration,
    IssueChangelogDebtCalculator issueChangelogDebtCalculator) {
    this.perspectives = perspectives;
    this.timeMachineConfiguration = timeMachineConfiguration;
    this.issueChangelogDebtCalculator = issueChangelogDebtCalculator;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @DependedUpon
  public List<Metric> generatesMetrics() {
    return ImmutableList.<Metric>of(
      CoreMetrics.NEW_TECHNICAL_DEBT
      );
  }

  @Override
  public void decorate(Resource resource, DecoratorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, resource);
    if (issuable != null && shouldSaveNewMetrics(context)) {
      List<Issue> issues = newArrayList(issuable.issues());
      saveMeasures(context, issues);
    }
  }

  private void saveMeasures(DecoratorContext context, Collection<Issue> issues) {
    Measure measure = new Measure(CoreMetrics.NEW_TECHNICAL_DEBT);
    for (Period period : timeMachineConfiguration.periods()) {
      Date periodDate = period.getDate();
      double value = calculateNewTechnicalDebtValue(issues, periodDate);
      Collection<Measure> children = context.getChildrenMeasures(measure.getMetric());
      double sum = MeasureUtils.sumOnVariation(true, period.getIndex(), children) + value;
      measure.setVariation(period.getIndex(), sum);
    }
    context.saveMeasure(measure);
  }

  private long calculateNewTechnicalDebtValue(Collection<Issue> issues, @Nullable Date periodDate) {
    long result = 0;
    for (Issue issue : issues) {
      Long debt = issueChangelogDebtCalculator.calculateNewTechnicalDebt(issue, periodDate);
      if (debt != null) {
        result += debt;
      }
    }
    return result;
  }

  private boolean shouldSaveNewMetrics(DecoratorContext context) {
    return context.getMeasure(CoreMetrics.NEW_TECHNICAL_DEBT) == null;
  }

}
