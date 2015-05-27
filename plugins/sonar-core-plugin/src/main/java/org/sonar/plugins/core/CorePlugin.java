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
package org.sonar.plugins.core;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.core.notifications.alerts.NewAlerts;

@Properties({
  @Property(
    key = CoreProperties.SERVER_BASE_URL,
    defaultValue = CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE,
    name = "Server base URL",
    description = "HTTP URL of this SonarQube server, such as <i>http://yourhost.yourdomain/sonar</i>. This value is used i.e. to create links in emails.",
    project = false,
    global = true,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_HOME_PAGE,
    defaultValue = "",
    name = "Project Home Page",
    description = "HTTP URL of the home page of the project.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_CI,
    defaultValue = "",
    name = "CI server",
    description = "HTTP URL of the continuous integration server.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_ISSUE_TRACKER,
    defaultValue = "",
    name = "Issue Tracker",
    description = "HTTP URL of the issue tracker.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_SOURCES,
    defaultValue = "",
    name = "SCM server",
    description = "HTTP URL of the server which hosts the sources of the project.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_SOURCES_DEV,
    defaultValue = "",
    name = "SCM connection for developers",
    description = "HTTP URL used by developers to connect to the SCM server for the project.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.ANALYSIS_MODE,
    defaultValue = CoreProperties.ANALYSIS_MODE_ANALYSIS,
    name = "Analysis mode",
    type = PropertyType.SINGLE_SELECT_LIST,
    options = {CoreProperties.ANALYSIS_MODE_ANALYSIS, CoreProperties.ANALYSIS_MODE_PREVIEW, CoreProperties.ANALYSIS_MODE_INCREMENTAL},
    global = false, project = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.PREVIEW_INCLUDE_PLUGINS,
    deprecatedKey = CoreProperties.DRY_RUN_INCLUDE_PLUGINS,
    name = "Plugins accepted for Preview and Incremental modes",
    description = "Comma-separated list of plugin keys. Those plugins will be used during preview or incremental analyses.",
    defaultValue = CoreProperties.PREVIEW_INCLUDE_PLUGINS_DEFAULT_VALUE,
    global = true, project = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.PREVIEW_EXCLUDE_PLUGINS,
    deprecatedKey = CoreProperties.DRY_RUN_EXCLUDE_PLUGINS,
    name = "Plugins excluded for Preview and Incremental modes",
    description = "Comma-separated list of plugin keys. Those plugins will not be used during preview or incremental analyses.",
    global = true, project = false,
    defaultValue = CoreProperties.PREVIEW_EXCLUDE_PLUGINS_DEFAULT_VALUE,
    category = CoreProperties.CATEGORY_GENERAL),

  // SERVER-SIDE TECHNICAL PROPERTIES

  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_REALM,
    name = "Security Realm",
    project = false,
    global = false
  ),
  @Property(
    key = "sonar.security.savePassword",
    name = "Save external password",
    project = false,
    global = false
  ),
  @Property(
    key = "sonar.authenticator.downcase",
    name = "Downcase login",
    description = "Downcase login during user authentication, typically for Active Directory",
    project = false,
    global = false,
    defaultValue = "false",
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS,
    name = "Create user accounts",
    description = "Create accounts when authenticating users via an external system",
    project = false,
    global = false,
    defaultValue = "true",
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_UPDATE_USER_ATTRIBUTES,
    name = "Update user attributes",
    description = "When using the LDAP or OpenID plugin, at each login, the user attributes (name, email, ...) are re-synchronized",
    project = false,
    global = false,
    defaultValue = "true",
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE,
    name = "Ignore failures during authenticator startup",
    defaultValue = "false",
    project = false,
    global = false,
    type = PropertyType.BOOLEAN),
  @Property(
    key = "sonar.enableFileVariation",
    name = "Enable file variation",
    global = false,
    defaultValue = "false",
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_LOCAL_USERS,
    name = "Local/technical users",
    description = "Comma separated list of user logins that will always be authenticated using SonarQube database. "
      + "When using the LDAP plugin, for these accounts, the user attributes (name, email, ...) are not re-synchronized",
    project = false,
    global = false,
    defaultValue = "admin",
    type = PropertyType.STRING,
    multiValues = true),
  @Property(
    key = CoreProperties.SCM_DISABLED_KEY,
    defaultValue = "false",
    name = "Disable the SCM Sensor",
    description = "Disable the retrieval of blame information from Source Control Manager",
    module = false,
    project = true,
    global = true,
    category = CoreProperties.CATEGORY_SCM,
    type = PropertyType.BOOLEAN
  ),
  @Property(
    key = CoreProperties.SCM_PROVIDER_KEY,
    defaultValue = "",
    name = "Key of the SCM provider for this project",
    description = "Force the provider to be used to get SCM information for this project. By default auto-detection is done. Example: svn, git.",
    module = false,
    project = true,
    global = false,
    category = CoreProperties.CATEGORY_SCM
  )
})
public final class CorePlugin extends SonarPlugin {

  @SuppressWarnings("rawtypes")
  @Override
  public List getExtensions() {
    ImmutableList.Builder<Object> extensions = ImmutableList.builder();

    extensions.add(
      DefaultResourceTypes.class,
      UserManagedMetrics.class,

      // Notify alerts on my favourite projects
      NewAlerts.class,
      NewAlerts.newMetadata());

    return extensions.build();
  }

}
