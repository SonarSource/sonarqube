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
package org.sonar.server.computation;

import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentTreeBuilder;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.db.DbClient;

public class ComputationContext implements org.sonar.server.computation.context.ComputationContext {
  private final BatchReportReader reportReader;
  private final DbClient dbClient;
  // cache of metadata as it's frequently accessed
  private final BatchReport.Metadata reportMetadata;
  private final Component component;
  private final LanguageRepository languageRepository;

  public ComputationContext(BatchReportReader reportReader, String projectKey, Settings projectSettings, DbClient dbClient,
    ComponentTreeBuilder componentTreeBuilder, LanguageRepository languageRepository) {
    this.reportReader = reportReader;
    this.dbClient = dbClient;
    this.reportMetadata = reportReader.readMetadata();
    this.component = componentTreeBuilder.build(this);
    this.languageRepository = languageRepository;
  }

  public BatchReport.Metadata getReportMetadata() {
    return reportMetadata;
  }

  public BatchReportReader getReportReader() {
    return reportReader;
  }

  @Override
  public Component getRoot() {
    return component;
  }

  /**
   * @deprecated because dbclient is too low level to be exposed in the CE API
   */
  @Deprecated
  public DbClient getDbClient() {
    return dbClient;
  }

  @Override
  public LanguageRepository getLanguageRepository() {
    return languageRepository;
  }
}
