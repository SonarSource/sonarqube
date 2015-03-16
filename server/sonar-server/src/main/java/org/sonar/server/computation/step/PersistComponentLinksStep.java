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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.component.ComponentLinkDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class PersistComponentLinksStep implements ComputationStep {

  private final DbClient dbClient;
  private final I18n i18n;

  private static final Map<Constants.ComponentLinkType, String> typesConverter = ImmutableMap.of(
    Constants.ComponentLinkType.HOME, ComponentLinkDto.TYPE_HOME_PAGE,
    Constants.ComponentLinkType.SCM, ComponentLinkDto.TYPE_SOURCES,
    Constants.ComponentLinkType.SCM_DEV, ComponentLinkDto.TYPE_SOURCES_DEV,
    Constants.ComponentLinkType.CI, ComponentLinkDto.TYPE_CI,
    Constants.ComponentLinkType.ISSUE, ComponentLinkDto.TYPE_ISSUE_TRACKER
    );

  public PersistComponentLinksStep(DbClient dbClient, I18n i18n) {
    this.dbClient = dbClient;
    this.i18n = i18n;
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
    processLinks(session, component);

    for (Integer childRef : component.getChildRefList()) {
      recursivelyProcessComponent(session, context, childRef);
    }
  }

  private void processLinks(DbSession session, BatchReport.Component component) {
    if (component.getType().equals(Constants.ComponentType.PROJECT) || component.getType().equals(Constants.ComponentType.MODULE)) {
      List<BatchReport.ComponentLink> links = component.getLinkList();
      List<ComponentLinkDto> previousLinks = dbClient.componentLinkDao().selectByComponentUuid(session, component.getUuid());
      mergeLinks(session, component.getUuid(), links, previousLinks);
    }
  }

  private void mergeLinks(DbSession session, String componentUuid, List<BatchReport.ComponentLink> links, List<ComponentLinkDto> previousLinks) {
    Set<String> linkType = newHashSet();
    for (final BatchReport.ComponentLink link : links) {
      String type = convertType(link.getType());
      if (!linkType.contains(type)) {
        linkType.add(type);
      } else {
        throw new IllegalArgumentException(String.format("Link of type '%s' has already been declared on component '%s'", type, componentUuid));
      }

      ComponentLinkDto previousLink = Iterables.find(previousLinks, new Predicate<ComponentLinkDto>() {
        @Override
        public boolean apply(@Nullable ComponentLinkDto input) {
          return input != null && input.getType().equals(convertType(link.getType()));
        }
      }, null);
      if (previousLink == null) {
        dbClient.componentLinkDao().insert(session,
          new ComponentLinkDto()
            .setComponentUuid(componentUuid)
            .setType(type)
            .setName(i18n.message(Locale.ENGLISH, "project_links." + type, null))
            .setHref(link.getHref())
          );
      } else {
        previousLink.setHref(link.getHref());
        dbClient.componentLinkDao().update(session, previousLink);
      }
    }

    for (ComponentLinkDto dto : previousLinks) {
      if (!linkType.contains(dto.getType()) && ComponentLinkDto.PROVIDED_TYPES.contains(dto.getType())) {
        dbClient.componentLinkDao().delete(session, dto.getId());
      }
    }
  }

  private static String convertType(Constants.ComponentLinkType reportType) {
    String type = typesConverter.get(reportType);
    if (type != null) {
      return type;
    } else {
      throw new IllegalArgumentException(String.format("Unsupported type %s", reportType.name()));
    }
  }

  @Override
  public String getDescription() {
    return "Persist component links";
  }
}
