/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.platform.monitoring;

import java.io.File;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.config.Settings;
import org.sonar.process.jmx.JmxConnector;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;

public class JmxConnectorProvider extends ProviderAdapter {

  private JmxConnector singleton = null;

  public synchronized JmxConnector provide(Settings settings) {
    if (singleton == null) {
      singleton = new JmxConnector(nonNullValueAsFile(settings, PROPERTY_SHARED_PATH));
    }
    return singleton;
  }

  private static File nonNullValueAsFile(Settings settings, String key) {
    String s = settings.getString(key);
    checkArgument(s != null, "Property %s is not set", key);
    return new File(s);
  }
}
