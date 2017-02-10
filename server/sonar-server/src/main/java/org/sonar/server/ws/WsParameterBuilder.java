/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.server.ws.WebService;

import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Ordering.natural;
import static java.lang.String.format;

public class WsParameterBuilder {
  private static final String PARAM_QUALIFIER = "qualifier";
  private static final String PARAM_QUALIFIERS = "qualifiers";
  private static final Set<String> DEPRECATED_QUALIFIERS = ImmutableSet.of(Qualifiers.LIBRARY);

  private WsParameterBuilder() {
    // static methods only
  }

  public static WebService.NewParam createRootQualifierParameter(WebService.NewAction action, QualifierParameterContext context) {
    return action.createParam(PARAM_QUALIFIER)
      .setDescription("Project qualifier. Filter the results with the specified qualifier. Possible values are:" + buildRootQualifiersDescription(context))
      .setPossibleValues(getRootQualifiers(context.getResourceTypes()));
  }

  public static WebService.NewParam createQualifiersParameter(WebService.NewAction action, QualifierParameterContext context) {
    return action.createParam(PARAM_QUALIFIERS)
      .setDescription(
        "Comma-separated list of component qualifiers. Filter the results with the specified qualifiers. Possible values are:" + buildAllQualifiersDescription(context))
      .setPossibleValues(getAllQualifiers(context.getResourceTypes()));
  }

  private static Set<String> getRootQualifiers(ResourceTypes resourceTypes) {
    return from(resourceTypes.getRoots())
      .transform(ResourceType::getQualifier)
      .filter(not(IsDeprecatedQualifier.INSTANCE))
      .toSortedSet(natural());
  }

  private static Set<String> getAllQualifiers(ResourceTypes resourceTypes) {
    return from(resourceTypes.getAll())
      .transform(ResourceType::getQualifier)
      .filter(not(IsDeprecatedQualifier.INSTANCE))
      .toSortedSet(natural());
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

  private enum IsDeprecatedQualifier implements Predicate<String> {
    INSTANCE;

    @Override
    public boolean apply(@Nullable String input) {
      return DEPRECATED_QUALIFIERS.contains(input);
    }
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
