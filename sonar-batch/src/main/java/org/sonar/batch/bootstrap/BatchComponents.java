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
import org.sonar.batch.design.DirectoryDsmDecorator;
import org.sonar.batch.design.DirectoryTangleIndexDecorator;
import org.sonar.batch.design.FileTangleIndexDecorator;
import org.sonar.batch.design.MavenDependenciesSensor;
import org.sonar.batch.design.ProjectDsmDecorator;
import org.sonar.batch.design.SubProjectDsmDecorator;
import org.sonar.batch.issue.tracking.IssueTracking;
import org.sonar.batch.maven.DefaultMavenPluginExecutor;
import org.sonar.batch.maven.MavenProjectBootstrapper;
import org.sonar.batch.maven.MavenProjectBuilder;
import org.sonar.batch.maven.MavenProjectConverter;
import org.sonar.batch.scan.report.ConsoleReport;
import org.sonar.batch.scan.report.JSONReport;
import org.sonar.batch.scm.ScmConfiguration;
import org.sonar.batch.scm.ScmSensor;
import org.sonar.batch.source.LinesSensor;
import org.sonar.core.computation.dbcleaner.DefaultPurgeTask;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.core.config.CorePropertyDefinitions;

import java.util.Collection;
import java.util.List;

public class BatchComponents {
  private BatchComponents() {
    // only static stuff
  }

  public static Collection all() {
    List components = Lists.newArrayList(
      // Maven
      MavenProjectBootstrapper.class, DefaultMavenPluginExecutor.class, MavenProjectConverter.class, MavenProjectBuilder.class,

      // Design
      MavenDependenciesSensor.class,
      ProjectDsmDecorator.class,
      SubProjectDsmDecorator.class,
      DirectoryDsmDecorator.class,
      DirectoryTangleIndexDecorator.class,
      FileTangleIndexDecorator.class,

      // SCM
      ScmConfiguration.class,
      ScmSensor.class,

      LinesSensor.class,

      // Issues tracking
      IssueTracking.class,

      // Reports
      ConsoleReport.class,
      JSONReport.class,

      // dbcleaner
      DefaultPeriodCleaner.class,
      DefaultPurgeTask.class
      );
    components.addAll(CorePropertyDefinitions.all());
    return components;
  }
}
