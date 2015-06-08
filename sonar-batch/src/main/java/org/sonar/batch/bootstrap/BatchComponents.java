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
package org.sonar.batch.bootstrap;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.batch.compute.ApplyProjectRolesDecorator;
import org.sonar.batch.compute.BranchCoverageDecorator;
import org.sonar.batch.compute.CommentDensityDecorator;
import org.sonar.batch.compute.CountFalsePositivesDecorator;
import org.sonar.batch.compute.CountUnresolvedIssuesDecorator;
import org.sonar.batch.compute.CoverageDecorator;
import org.sonar.batch.compute.DirectoriesDecorator;
import org.sonar.batch.compute.FilesDecorator;
import org.sonar.batch.compute.ItBranchCoverageDecorator;
import org.sonar.batch.compute.ItCoverageDecorator;
import org.sonar.batch.compute.ItLineCoverageDecorator;
import org.sonar.batch.compute.LineCoverageDecorator;
import org.sonar.batch.compute.ManualMeasureDecorator;
import org.sonar.batch.compute.NewCoverageAggregator;
import org.sonar.batch.compute.NewCoverageFileAnalyzer;
import org.sonar.batch.compute.NewItCoverageFileAnalyzer;
import org.sonar.batch.compute.NewOverallCoverageFileAnalyzer;
import org.sonar.batch.compute.OverallBranchCoverageDecorator;
import org.sonar.batch.compute.OverallCoverageDecorator;
import org.sonar.batch.compute.OverallLineCoverageDecorator;
import org.sonar.batch.compute.TimeMachineConfigurationPersister;
import org.sonar.batch.compute.UnitTestDecorator;
import org.sonar.batch.compute.VariationDecorator;
import org.sonar.batch.cpd.CpdComponents;
import org.sonar.batch.debt.DebtDecorator;
import org.sonar.batch.debt.IssueChangelogDebtCalculator;
import org.sonar.batch.debt.NewDebtDecorator;
import org.sonar.batch.debt.SqaleRatingDecorator;
import org.sonar.batch.debt.SqaleRatingSettings;
import org.sonar.batch.issue.tracking.InitialOpenIssuesSensor;
import org.sonar.batch.issue.tracking.IssueHandlers;
import org.sonar.batch.issue.tracking.IssueTracking;
import org.sonar.batch.issue.tracking.IssueTrackingDecorator;
import org.sonar.batch.language.LanguageDistributionDecorator;
import org.sonar.batch.qualitygate.QualityGateVerifier;
import org.sonar.batch.scan.report.ConsoleReport;
import org.sonar.batch.scan.report.HtmlReport;
import org.sonar.batch.scan.report.IssuesReportBuilder;
import org.sonar.batch.scan.report.JSONReport;
import org.sonar.batch.scan.report.RuleNameProvider;
import org.sonar.batch.scan.report.SourceProvider;
import org.sonar.batch.scm.ScmConfiguration;
import org.sonar.batch.scm.ScmSensor;
import org.sonar.batch.source.CodeColorizerSensor;
import org.sonar.batch.source.LinesSensor;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.resource.DefaultResourceTypes;

public class BatchComponents {
  private BatchComponents() {
    // only static stuff
  }

  public static Collection all(DefaultAnalysisMode analysisMode) {
    List components = Lists.newArrayList(
      DefaultResourceTypes.get(),
      // SCM
      ScmConfiguration.class,
      ScmSensor.class,

      LinesSensor.class,
      CodeColorizerSensor.class,

      // Issues tracking
      IssueTracking.class,

      // Reports
      ConsoleReport.class,
      JSONReport.class,
      HtmlReport.class,
      IssuesReportBuilder.class,
      SourceProvider.class,
      RuleNameProvider.class,

      // language
      LanguageDistributionDecorator.class,

      // Debt
      IssueChangelogDebtCalculator.class,
      DebtDecorator.class,
      NewDebtDecorator.class,
      SqaleRatingDecorator.class,
      SqaleRatingSettings.class,

      DefaultNotificationManager.class,

      // Quality Gate
      QualityGateVerifier.class,

      // Issue tracking
      IssueTrackingDecorator.class,
      IssueHandlers.class,
      InitialOpenIssuesSensor.class,

      // to be moved to compute engine
      CountUnresolvedIssuesDecorator.class,
      CountFalsePositivesDecorator.class,
      UnitTestDecorator.class,
      LineCoverageDecorator.class,
      CoverageDecorator.class,
      BranchCoverageDecorator.class,
      ItLineCoverageDecorator.class,
      ItCoverageDecorator.class,
      ItBranchCoverageDecorator.class,
      OverallLineCoverageDecorator.class,
      OverallCoverageDecorator.class,
      OverallBranchCoverageDecorator.class,
      ApplyProjectRolesDecorator.class,
      CommentDensityDecorator.class,
      DirectoriesDecorator.class,
      FilesDecorator.class,
      ManualMeasureDecorator.class,
      VariationDecorator.class,
      TimeMachineConfigurationPersister.class,
      NewCoverageFileAnalyzer.class,
      NewItCoverageFileAnalyzer.class,
      NewOverallCoverageFileAnalyzer.class,
      NewCoverageAggregator.class,
      TimeMachineConfiguration.class
      );
    components.addAll(CorePropertyDefinitions.all());
    // CPD
    components.addAll(CpdComponents.all());
    return components;
  }
}
