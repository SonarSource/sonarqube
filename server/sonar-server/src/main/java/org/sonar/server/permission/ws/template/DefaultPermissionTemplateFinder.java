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

package org.sonar.server.permission.ws.template;

import com.google.common.base.Function;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Ordering.natural;
import static org.sonar.server.permission.DefaultPermissionTemplates.DEFAULT_TEMPLATE_PROPERTY;
import static org.sonar.server.permission.DefaultPermissionTemplates.defaultRootQualifierTemplateProperty;
import static org.sonar.server.permission.ws.ResourceTypeToQualifier.RESOURCE_TYPE_TO_QUALIFIER;

public class DefaultPermissionTemplateFinder {
  private final Settings settings;
  private final ResourceTypes resourceTypes;

  public DefaultPermissionTemplateFinder(Settings settings, ResourceTypes resourceTypes) {
    this.settings = settings;
    this.resourceTypes = resourceTypes;
  }

  public Set<String> getDefaultTemplateUuids() {
    return from(resourceTypes.getRoots())
      .transform(RESOURCE_TYPE_TO_QUALIFIER)
      .transform(new QualifierToDefaultTemplate(settings))
      .toSortedSet(natural());
  }

  public List<TemplateUuidQualifier> getDefaultTemplatesByQualifier() {
    return from(resourceTypes.getRoots())
      .transform(RESOURCE_TYPE_TO_QUALIFIER)
      .transform(new QualifierToTemplateUuidQualifier(settings))
      .toList();
  }

  public static class TemplateUuidQualifier {
    private final String templateUuid;
    private final String qualifier;

    TemplateUuidQualifier(String templateUuid, String qualifier) {
      this.templateUuid = templateUuid;
      this.qualifier = qualifier;
    }

    public String getTemplateUuid() {
      return templateUuid;
    }

    public String getQualifier() {
      return qualifier;
    }
  }

  private static class QualifierToDefaultTemplate implements Function<String, String> {
    private final Settings settings;

    QualifierToDefaultTemplate(Settings settings) {
      this.settings = settings;
    }

    @Override
    public String apply(@Nonnull String qualifier) {
      String effectiveTemplateUuid = effectiveTemplateUuid(settings, qualifier);
      return effectiveTemplateUuid;
    }
  }

  private static class QualifierToTemplateUuidQualifier implements Function<String, TemplateUuidQualifier> {
    private final Settings settings;

    QualifierToTemplateUuidQualifier(Settings settings) {
      this.settings = settings;
    }

    @Override
    public TemplateUuidQualifier apply(@Nonnull String qualifier) {
      String effectiveTemplateUuid = effectiveTemplateUuid(settings, qualifier);

      return new TemplateUuidQualifier(effectiveTemplateUuid, qualifier);
    }
  }

  private static String effectiveTemplateUuid(Settings settings, String qualifier) {
    String qualifierTemplateUuid = settings.getString(defaultRootQualifierTemplateProperty(qualifier));
    String projectTemplateUuid = settings.getString(defaultRootQualifierTemplateProperty(Qualifiers.PROJECT));
    String defaultTemplateUuid = settings.getString(DEFAULT_TEMPLATE_PROPERTY);

    if (qualifierTemplateUuid != null) {
      return qualifierTemplateUuid;
    } else if (projectTemplateUuid != null) {
      return projectTemplateUuid;
    } else {
      return defaultTemplateUuid;
    }
  }

}
