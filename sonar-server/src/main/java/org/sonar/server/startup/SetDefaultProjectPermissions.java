/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.server.platform.PersistentSettings;

import java.util.Map;

/**
 * @since 3.2
 */
public class SetDefaultProjectPermissions {
  private static final String SONAR_ADMINISTRATORS = "sonar-administrators";
  private static final String ANYONE_AND_USERS = "Anyone,sonar-users";
  private static final String SUFFIX = ".defaultGroups";

  private final PersistentSettings persistentSettings;

  public SetDefaultProjectPermissions(PersistentSettings persistentSettings) {
    this.persistentSettings = persistentSettings;
  }

  public void start() {
    Map<String, String> props = Maps.newHashMap();
    props.putAll(missingProperties("TRK"));

    // Support old versions of Views plugin
    props.putAll(missingProperties("VW"));
    props.putAll(missingProperties("SVW"));

    if (!props.isEmpty()) {
      LoggerFactory.getLogger(SetDefaultProjectPermissions.class).info("Set default project roles");
      persistentSettings.saveProperties(props);
    }
  }

  private Map<String, String> missingProperties(String qualifier) {
    Map<String, String> props = Maps.newHashMap();
    if (StringUtils.isBlank(persistentSettings.getSettings().getString("sonar.role.user." + qualifier + SUFFIX))) {
      completeDefaultRoles(qualifier, props);
    }
    return props;
  }

  private void completeDefaultRoles(String qualifier, Map<String, String> props) {
    props.put("sonar.role.admin." + qualifier + SUFFIX, SONAR_ADMINISTRATORS);
    props.put("sonar.role.user." + qualifier + SUFFIX, ANYONE_AND_USERS);
    props.put("sonar.role.codeviewer." + qualifier + SUFFIX, ANYONE_AND_USERS);
  }
}
