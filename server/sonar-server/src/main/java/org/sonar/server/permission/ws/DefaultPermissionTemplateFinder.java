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

package org.sonar.server.permission.ws;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.ResourceTypes;

import static com.google.common.base.Objects.firstNonNull;
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
    return ImmutableSet.<String>builder()
      .addAll(
        FluentIterable.from(resourceTypes.getRoots())
          .transform(RESOURCE_TYPE_TO_QUALIFIER)
          .transform(new QualifierToDefaultTemplate(settings))
          .toSet())
      .add(settings.getString(DEFAULT_TEMPLATE_PROPERTY))
      .build();
  }

  private static class QualifierToDefaultTemplate implements Function<String, String> {
    private final Settings settings;

    QualifierToDefaultTemplate(Settings settings) {
      this.settings = settings;
    }

    @Override
    public String apply(@Nonnull String qualifier) {
      String qualifierProperty = settings.getString(defaultRootQualifierTemplateProperty(qualifier));
      return firstNonNull(qualifierProperty, settings.getString(DEFAULT_TEMPLATE_PROPERTY));
    }
  }

}
