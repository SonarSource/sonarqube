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
package org.sonar.server.ws;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;

import static java.lang.String.format;

public class WsParameterBuilder {
  private static final String PARAM_QUALIFIER = "qualifier";
  private static final String PARAM_QUALIFIERS = "qualifiers";

  private WsParameterBuilder() {
    // static methods only
  }

  public static WebService.NewParam createRootQualifierParameter(WebService.NewAction action, QualifierParameterContext context) {
    return action.createParam(PARAM_QUALIFIER)
      .setDescription("Project qualifier. Filter the results with the specified qualifier. Possible values are:" + buildRootQualifiersDescription(context))
      .setPossibleValues(getRootQualifiers(context.getResourceTypes()));
  }

  public static WebService.NewParam createRootQualifiersParameter(WebService.NewAction action, QualifierParameterContext context) {
    return action.createParam(PARAM_QUALIFIERS)
      .setDescription("Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. " +
        "Possible values are:" + buildRootQualifiersDescription(context))
      .setPossibleValues(getRootQualifiers(context.getResourceTypes()));
  }

  public static WebService.NewParam createDefaultTemplateQualifierParameter(WebService.NewAction action, QualifierParameterContext context) {
    return action.createParam(PARAM_QUALIFIER)
      .setDescription("Project qualifier. Filter the results with the specified qualifier. Possible values are:" + buildDefaultTemplateQualifiersDescription(context))
      .setPossibleValues(getDefaultTemplateQualifiers(context.getResourceTypes()));
  }

  public static WebService.NewParam createQualifiersParameter(WebService.NewAction action, QualifierParameterContext context) {
    return action.createParam(PARAM_QUALIFIERS)
      .setDescription(
        "Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. Possible values are:" + buildAllQualifiersDescription(context))
      .setPossibleValues(getAllQualifiers(context.getResourceTypes()));
  }

  private static Set<String> getRootQualifiers(ResourceTypes resourceTypes) {
    return resourceTypes.getRoots().stream()
      .map(ResourceType::getQualifier)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  private static Set<String> getDefaultTemplateQualifiers(ResourceTypes resourceTypes) {
    return resourceTypes.getRoots().stream()
      .map(ResourceType::getQualifier)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  private static Set<String> getAllQualifiers(ResourceTypes resourceTypes) {
    return resourceTypes.getAll().stream()
      .map(ResourceType::getQualifier)
      .collect(Collectors.toCollection(TreeSet::new));
  }

  private static String buildDefaultTemplateQualifiersDescription(QualifierParameterContext context) {
    return buildQualifiersDescription(context, getDefaultTemplateQualifiers(context.getResourceTypes()));
  }

  private static String buildRootQualifiersDescription(QualifierParameterContext context) {
    return buildQualifiersDescription(context, getRootQualifiers(context.getResourceTypes()));
  }

  private static String buildAllQualifiersDescription(QualifierParameterContext context) {
    return buildQualifiersDescription(context, getAllQualifiers(context.getResourceTypes()));
  }

  private static String buildQualifiersDescription(QualifierParameterContext context, Set<String> qualifiers) {
    StringBuilder description = new StringBuilder();
    description.append("<ul>");
    String qualifierPattern = "<li>%s - %s</li>";
    for (String qualifier : qualifiers) {
      description.append(format(qualifierPattern, qualifier, qualifierLabel(context, qualifier)));
    }
    description.append("</ul>");

    return description.toString();
  }

  private static String qualifierLabel(QualifierParameterContext context, String qualifier) {
    String qualifiersPropertyPrefix = "qualifiers.";
    return context.getI18n().message(Locale.ENGLISH, qualifiersPropertyPrefix + qualifier, "no description available");
  }

  public static class QualifierParameterContext {
    private final I18n i18n;
    private final ResourceTypes resourceTypes;

    private QualifierParameterContext(I18n i18n, ResourceTypes resourceTypes) {
      this.i18n = i18n;
      this.resourceTypes = resourceTypes;
    }

    public static QualifierParameterContext newQualifierParameterContext(I18n i18n, ResourceTypes resourceTypes) {
      return new QualifierParameterContext(i18n, resourceTypes);
    }

    public I18n getI18n() {
      return i18n;
    }

    public ResourceTypes getResourceTypes() {
      return resourceTypes;
    }
  }
}
