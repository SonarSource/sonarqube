/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;

import static com.google.common.base.Preconditions.checkArgument;

public class PersistProjectLinksStep implements ComputationStep {

  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final DbClient dbClient;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final UuidFactory uuidFactory;

  private static final Map<ComponentLinkType, String> typesConverter = ImmutableMap.of(
    ComponentLinkType.HOME, ProjectLinkDto.TYPE_HOME_PAGE,
    ComponentLinkType.SCM, ProjectLinkDto.TYPE_SOURCES,
    ComponentLinkType.CI, ProjectLinkDto.TYPE_CI,
    ComponentLinkType.ISSUE, ProjectLinkDto.TYPE_ISSUE_TRACKER);

  public PersistProjectLinksStep(AnalysisMetadataHolder analysisMetadataHolder, DbClient dbClient, TreeRootHolder treeRootHolder,
    BatchReportReader reportReader, UuidFactory uuidFactory) {
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.dbClient = dbClient;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(ComputationStep.Context context) {
    if (!analysisMetadataHolder.getBranch().isMain()) {
      return;
    }

    try (DbSession session = dbClient.openSession(false)) {
      Component project = treeRootHolder.getRoot();
      ScannerReport.Component batchComponent = reportReader.readComponent(project.getReportAttributes().getRef());
      List<ProjectLinkDto> previousLinks = dbClient.projectLinkDao().selectByProjectUuid(session, project.getUuid());
      mergeLinks(session, project.getUuid(), batchComponent.getLinkList(), previousLinks);
      session.commit();
    }
  }

  private void mergeLinks(DbSession session, String componentUuid, List<ScannerReport.ComponentLink> links, List<ProjectLinkDto> previousLinks) {
    Set<String> linkType = new HashSet<>();
    links.forEach(
      link -> {
        String type = convertType(link.getType());
        checkArgument(!linkType.contains(type), "Link of type '%s' has already been declared on component '%s'", type, componentUuid);
        linkType.add(type);

        Optional<ProjectLinkDto> previousLink = previousLinks.stream()
          .filter(input -> input != null && input.getType().equals(convertType(link.getType())))
          .findFirst();
        if (previousLink.isPresent()) {
          previousLink.get().setHref(link.getHref());
          dbClient.projectLinkDao().update(session, previousLink.get());
        } else {
          dbClient.projectLinkDao().insert(session,
            new ProjectLinkDto()
              .setUuid(uuidFactory.create())
              .setProjectUuid(componentUuid)
              .setType(type)
              .setHref(link.getHref()));
        }
      });

    previousLinks.stream()
      .filter(dto -> !linkType.contains(dto.getType()))
      .filter(dto -> ProjectLinkDto.PROVIDED_TYPES.contains(dto.getType()))
      .forEach(dto -> dbClient.projectLinkDao().delete(session, dto.getUuid()));
  }

  private static String convertType(ComponentLinkType reportType) {
    String type = typesConverter.get(reportType);
    checkArgument(type != null, "Unsupported type %s", reportType.name());
    return type;
  }

  @Override
  public String getDescription() {
    return "Persist project links";
  }
}
