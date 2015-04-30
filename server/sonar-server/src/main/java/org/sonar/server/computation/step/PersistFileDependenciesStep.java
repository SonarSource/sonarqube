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

import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.design.FileDependencyDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.source.ReportIterator;
import org.sonar.server.db.DbClient;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PersistFileDependenciesStep implements ComputationStep {

  private final DbClient dbClient;
  private final System2 system2;

  public PersistFileDependenciesStep(DbClient dbClient, System2 system2) {
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  @Override
  public String[] supportedProjectQualifiers() {
    return new String[] {Qualifiers.PROJECT};
  }

  @Override
  public void execute(ComputationContext context) {
    DbSession session = dbClient.openSession(true);
    try {
      FileDependenciesContext fileDependenciesContext = new FileDependenciesContext(context, session);
      int rootComponentRef = context.getReportMetadata().getRootComponentRef();
      recursivelyProcessComponent(fileDependenciesContext, rootComponentRef, rootComponentRef);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void recursivelyProcessComponent(FileDependenciesContext fileDependenciesContext, int componentRef, int parentModuleRef) {
    BatchReportReader reportReader = fileDependenciesContext.context.getReportReader();
    BatchReport.Component component = reportReader.readComponent(componentRef);
    if (component.getType().equals(Constants.ComponentType.FILE)) {
      processFileDependenciesReport(fileDependenciesContext, component);
    }

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(fileDependenciesContext, childRef, componentRef);
    }
  }

  private void processFileDependenciesReport(FileDependenciesContext fileDependenciesContext, BatchReport.Component component){
    File fileDependencyReport = fileDependenciesContext.context.getReportReader().readFileDependencies(component.getRef());
    if (fileDependencyReport != null) {
      ReportIterator<BatchReport.FileDependency> fileDependenciesIterator = new ReportIterator<>(fileDependencyReport, BatchReport.FileDependency.PARSER);
      try {
        while (fileDependenciesIterator.hasNext()) {
          BatchReport.FileDependency fileDependency = fileDependenciesIterator.next();
          persistFileDependency(fileDependenciesContext, fileDependency, component.getRef());
        }
      } finally {
        fileDependenciesIterator.close();
      }
    }
  }

  private void persistFileDependency(FileDependenciesContext fileDependenciesContext, BatchReport.FileDependency fileDependency, int fromRef){
    int toFileRef = fileDependency.getToFileRef();
    String fromComponentUuid = fileDependenciesContext.uuidsByRef.getUuidFromRef(fromRef);
    String toComponentUuid = fileDependenciesContext.uuidsByRef.getUuidFromRef(toFileRef);
    dbClient.fileDependencyDao().insert(fileDependenciesContext.session, new FileDependencyDto()
        .setFromComponentUuid(fileDependenciesContext.uuidsByRef.getUuidFromRef(fromRef))
        .setToComponentUuid(fileDependenciesContext.uuidsByRef.getUuidFromRef(toFileRef))
        .setFromParentUuid(fileDependenciesContext.parentUuidsByComponentUuid.get(fromComponentUuid))
        .setToParentUuid(fileDependenciesContext.parentUuidsByComponentUuid.get(toComponentUuid))
        .setRootProjectSnapshotId(fileDependenciesContext.rootSnapshotId)
        .setWeight(fileDependency.getWeight())
        .setCreatedAt(system2.now())
    );
  }

  private static class FileDependenciesContext {
    private final Long rootSnapshotId;
    private final ComponentUuidsCache uuidsByRef;
    private final ComputationContext context;
    private final Map<String, String> parentUuidsByComponentUuid = new HashMap<>();
    private final DbSession session;

    public FileDependenciesContext(ComputationContext context, DbSession session) {
      this.context = context;
      this.rootSnapshotId = context.getReportMetadata().getSnapshotId();
      this.session = session;
      this.uuidsByRef = new ComponentUuidsCache(context.getReportReader());
      int rootComponentRef = context.getReportMetadata().getRootComponentRef();
      recursivelyProcessParentByComponentCache(rootComponentRef, rootComponentRef);
    }

    private void recursivelyProcessParentByComponentCache(int componentRef, int parentModuleRef){
      BatchReportReader reportReader = context.getReportReader();
      BatchReport.Component component = reportReader.readComponent(componentRef);
      BatchReport.Component parent = reportReader.readComponent(parentModuleRef);
      if (component.getType().equals(Constants.ComponentType.FILE)) {
        parentUuidsByComponentUuid.put(component.getUuid(), parent.getUuid());
      }

      for (Integer childRef : component.getChildRefList()) {
        recursivelyProcessParentByComponentCache(childRef, componentRef);
      }
    }
  }

  @Override
  public String getDescription() {
    return "Persist file dependencies";
  }
}
