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
package org.sonar.server.ui.ws;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.sonar.api.ServerComponent;
import org.sonar.api.i18n.I18n;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.server.user.UserSession;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

public class ComponentConfigurationPages implements ServerComponent {

  static final String PROPERTY_COMPARABLE = "comparable";
  static final String PROPERTY_CONFIGURABLE = "configurable";
  static final String PROPERTY_HAS_ROLE_POLICY = "hasRolePolicy";
  static final String PROPERTY_MODIFIABLE_HISTORY = "modifiable_history";
  static final String PROPERTY_UPDATABLE_KEY = "updatable_key";
  static final String PROPERTY_DELETABLE = "deletable";

  private final I18n i18n;
  private final ResourceTypes resourceTypes;

  public ComponentConfigurationPages(I18n i18n, ResourceTypes resourceTypes) {
    this.i18n = i18n;
    this.resourceTypes = resourceTypes;
  }

  List<ConfigPage> getConfigPages(ComponentDto component, UserSession userSession) {
    boolean isAdmin = userSession.hasProjectPermissionByUuid(UserRole.ADMIN, component.projectUuid());
    boolean isProject = Qualifiers.PROJECT.equals(component.qualifier());
    Locale locale = userSession.locale();
    String componentKey = encodeComponentKey(component);

    List<ConfigPage> configPages = Lists.newArrayList();

    configPages.add(new ConfigPage(
      isAdmin && componentTypeHasProperty(component, PROPERTY_CONFIGURABLE),
      String.format("/project/settings?id=%s", componentKey),
      i18n.message(locale, "project_settings.page", null)));

    configPages.add(new ConfigPage(
      isProject,
      String.format("/project/profile?id=%s", componentKey),
      i18n.message(locale, "project_quality_profiles.page", null)));

    configPages.add(new ConfigPage(
      isProject,
      String.format("/project/qualitygate?id=%s", componentKey),
      i18n.message(locale, "project_quality_gate.page", null)));

    configPages.add(new ConfigPage(
      isAdmin,
      String.format("/manual_measures/index?id=%s", componentKey),
      i18n.message(locale, "manual_measures.page", null)));

    configPages.add(new ConfigPage(
      isAdmin && isProject,
      String.format("/action_plans/index?id=%s", componentKey),
      i18n.message(locale, "action_plans.page", null)));

    configPages.add(new ConfigPage(
      isAdmin && isProject,
      String.format("/project/links?id=%s", componentKey),
      i18n.message(locale, "action_plans.page", null)));

    configPages.add(new ConfigPage(
      componentTypeHasProperty(component, PROPERTY_HAS_ROLE_POLICY),
      String.format("/project_roles/index?id=%s", componentKey),
      i18n.message(locale, "permissions.page", null)));

    configPages.add(new ConfigPage(
      componentTypeHasProperty(component, PROPERTY_MODIFIABLE_HISTORY),
      String.format("/project/history?id=%s", componentKey),
      i18n.message(locale, "project_history.page", null)));

    configPages.add(new ConfigPage(
      componentTypeHasProperty(component, PROPERTY_UPDATABLE_KEY),
      String.format("/project/key?id=%s", componentKey),
      i18n.message(locale, "update_key.page", null)));

    configPages.add(new ConfigPage(
      componentTypeHasProperty(component, PROPERTY_DELETABLE),
      String.format("/project/deletion?id=%s", componentKey),
      i18n.message(locale, "deletion.page", null)));

    return configPages;
  }

  static String encodeComponentKey(ComponentDto component) {
    String componentKey = component.getKey();
    try {
      componentKey = URLEncoder.encode(componentKey, Charsets.UTF_8.name());
    } catch (UnsupportedEncodingException unknownEncoding) {
      throw new IllegalStateException(unknownEncoding);
    }
    return componentKey;
  }

  boolean componentTypeHasProperty(ComponentDto component, String resourceTypeProperty) {
    ResourceType resourceType = resourceTypes.get(component.qualifier());
    if (resourceType != null) {
      return resourceType.getBooleanProperty(resourceTypeProperty);
    }
    return false;
  }

  static class ConfigPage {
    private final boolean visible;
    private final String url;
    private final String name;

    ConfigPage(boolean visible, String url, String name) {
      this.visible = visible;
      this.url = url;
      this.name = name;
    }

    void write(JsonWriter json) {
      if (visible) {
        json.beginObject()
          .prop("url", url)
          .prop("name", name)
          .endObject();
      }
    }
  }

}
