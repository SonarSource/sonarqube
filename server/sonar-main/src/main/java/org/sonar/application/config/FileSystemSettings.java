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
package org.sonar.application.config;

import java.io.File;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Props;

import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;
import static org.sonar.process.ProcessProperties.Property.PATH_WEB;

public class FileSystemSettings implements Consumer<Props> {

  private static final Logger LOG = LoggerFactory.getLogger(FileSystemSettings.class);

  @Override
  public void accept(Props props) {
    ensurePropertyIsAbsolutePath(props, PATH_DATA.getKey());
    ensurePropertyIsAbsolutePath(props, PATH_WEB.getKey());
    ensurePropertyIsAbsolutePath(props, PATH_LOGS.getKey());
    ensurePropertyIsAbsolutePath(props, PATH_TEMP.getKey());
  }

  private static File ensurePropertyIsAbsolutePath(Props props, String propKey) {
    // default values are set by ProcessProperties
    String path = props.nonNullValue(propKey);
    File d = new File(path);
    if (!d.isAbsolute()) {
      File homeDir = props.nonNullValueAsFile(PATH_HOME.getKey());
      d = new File(homeDir, path);
      LOG.trace("Overriding property {} from relative path '{}' to absolute path '{}'", propKey, path, d.getAbsolutePath());
      props.set(propKey, d.getAbsolutePath());
    }
    return d;
  }

}
