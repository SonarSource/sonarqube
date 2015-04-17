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

package org.sonar.server.computation.step;

import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;

public class PersistTestResultsStep implements ComputationStep {

  private final DbClient dbClient;

  public PersistTestResultsStep(DbClient dbClient, I18n i18n) {
    this.dbClient = dbClient;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(false);
    try {
      int rootComponentRef = context.getReportMetadata().getRootComponentRef();
      recursivelyProcessComponent(session, context, rootComponentRef);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void recursivelyProcessComponent(DbSession session, ComputationContext context, int componentRef) {
    BatchReportReader reportReader = context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    if (component.getIsTest() && reportReader.readTestResults(componentRef) != null) {
      persistTestResults(session, component);
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(session, context, childRef);
    }
  }

  private void persistTestResults(DbSession session, BatchReport.Component component) {
    dbClient.fileSourceDao().select(component.getUuid());
    dbClient.fileSourceDao().update(session, new FileSourceDto());
  }

  @Override
  public String getDescription() {
    return "Persist test results";
  }
}
