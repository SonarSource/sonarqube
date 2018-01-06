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
package org.sonar.server.computation.task.projectanalysis.step;

import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.i18n.I18n;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.ComponentLink.ComponentLinkType;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.server.computation.task.projectanalysis.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.sonar.server.computation.task.projectanalysis.component.ComponentVisitor.Order.PRE_ORDER;

/**
 * Persist project and module links
 */
public class PersistProjectLinksStep implements ComputationStep {

  private final DbClient dbClient;
  private final I18n i18n;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;

  private static final Map<ComponentLinkType, String> typesConverter = ImmutableMap.of(
    ComponentLinkType.HOME, ComponentLinkDto.TYPE_HOME_PAGE,
    ComponentLinkType.SCM, ComponentLinkDto.TYPE_SOURCES,
    ComponentLinkType.SCM_DEV, ComponentLinkDto.TYPE_SOURCES_DEV,
    ComponentLinkType.CI, ComponentLinkDto.TYPE_CI,
    ComponentLinkType.ISSUE, ComponentLinkDto.TYPE_ISSUE_TRACKER);

  public PersistProjectLinksStep(DbClient dbClient, I18n i18n, TreeRootHolder treeRootHolder, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.i18n = i18n;
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
  }

  @Override
  public void execute() {
    try (DbSession session = dbClient.openSession(false)) {
      new DepthTraversalTypeAwareCrawler(new ProjectLinkVisitor(session))
        .visit(treeRootHolder.getRoot());
      session.commit();
    }
  }

  private class ProjectLinkVisitor extends TypeAwareVisitorAdapter {

    private final DbSession session;

    private ProjectLinkVisitor(DbSession session) {
      super(CrawlerDepthLimit.FILE, PRE_ORDER);
      this.session = session;
    }

    @Override
    public void visitProject(Component project) {
      processComponent(project);
    }

    @Override
    public void visitModule(Component module) {
      processComponent(module);
    }

    private void processComponent(Component component) {
      ScannerReport.Component batchComponent = reportReader.readComponent(component.getReportAttributes().getRef());
      processLinks(component.getUuid(), batchComponent.getLinkList());
    }

    private void processLinks(String componentUuid, List<ScannerReport.ComponentLink> links) {
      List<ComponentLinkDto> previousLinks = dbClient.componentLinkDao().selectByComponentUuid(session, componentUuid);
      mergeLinks(session, componentUuid, links, previousLinks);
    }

    private void mergeLinks(DbSession session, String componentUuid, List<ScannerReport.ComponentLink> links, List<ComponentLinkDto> previousLinks) {
      Set<String> linkType = new HashSet<>();
      for (final ScannerReport.ComponentLink link : links) {
        String type = convertType(link.getType());
        if (!linkType.contains(type)) {
          linkType.add(type);
        } else {
          throw new IllegalArgumentException(String.format("Link of type '%s' has already been declared on component '%s'", type, componentUuid));
        }

        Optional<ComponentLinkDto> previousLink = previousLinks.stream()
          .filter(input -> input != null && input.getType().equals(convertType(link.getType())))
          .findFirst();
        if (previousLink.isPresent()) {
          previousLink.get().setHref(link.getHref());
          dbClient.componentLinkDao().update(session, previousLink.get());
        } else {
          dbClient.componentLinkDao().insert(session,
            new ComponentLinkDto()
              .setComponentUuid(componentUuid)
              .setType(type)
              .setName(i18n.message(Locale.ENGLISH, "project_links." + type, null))
              .setHref(link.getHref()));
        }
      }

      for (ComponentLinkDto dto : previousLinks) {
        if (!linkType.contains(dto.getType()) && ComponentLinkDto.PROVIDED_TYPES.contains(dto.getType())) {
          dbClient.componentLinkDao().delete(session, dto.getId());
        }
      }
    }

    private String convertType(ComponentLinkType reportType) {
      String type = typesConverter.get(reportType);
      if (type != null) {
        return type;
      } else {
        throw new IllegalArgumentException(String.format("Unsupported type %s", reportType.name()));
      }
    }
  }

  @Override
  public String getDescription() {
    return "Persist project links";
  }
}
