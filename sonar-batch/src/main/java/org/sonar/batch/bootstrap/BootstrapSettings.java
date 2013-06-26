/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.bootstrap;

import com.google.common.collect.Maps;
import org.codehaus.plexus.util.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Encryption;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.Properties;

/**
 * @since 2.12
 */
public class BootstrapSettings {
  private Map<String, String> properties;
  private final Encryption encryption;

  public BootstrapSettings(BootstrapProperties bootstrapProperties) {
    this(bootstrapProperties, null);
  }

  public BootstrapSettings(BootstrapProperties bootstrapProperties, @Nullable ProjectReactor projectReactor) {
    properties = Maps.newHashMap();
    // order is important -> bottom-up. The last one overrides all the others.
    properties.putAll(bootstrapProperties.properties());
    if (projectReactor != null) {
      addProperties(projectReactor.getRoot().getProperties());
    }
    properties.putAll(System.getenv());
    addProperties(System.getProperties());
    encryption = new Encryption(properties.get(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
  }

  private void addProperties(Properties p) {
    for (Map.Entry<Object, Object> entry : p.entrySet()) {
      if (entry.getValue() != null) {
        properties.put(entry.getKey().toString(), entry.getValue().toString());
      }
    }
  }

  public Map<String, String> properties() {
    return properties;
  }

  public String property(String key) {
    String value = properties.get(key);
    if (value != null && encryption.isEncrypted(value)) {
      try {
        value = encryption.decrypt(value);
      } catch (Exception e) {
        throw new IllegalStateException("Fail to decrypt the property " + key + ". Please check your secret key.", e);
      }
    }
    return value;
  }

  public String property(String key, String defaultValue) {
    return StringUtils.defaultString(property(key), defaultValue);
  }
}
