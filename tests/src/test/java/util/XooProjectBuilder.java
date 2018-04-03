/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import static java.util.Arrays.asList;

public class XooProjectBuilder {
  private final String key;
  private final List<String> moduleKeys = new ArrayList<>();
  private int filesPerModule = 1;

  public XooProjectBuilder(String projectKey) {
    this.key = projectKey;
  }

  public XooProjectBuilder addModules(String key, String... otherKeys) {
    this.moduleKeys.add(key);
    this.moduleKeys.addAll(asList(otherKeys));
    return this;
  }

  public XooProjectBuilder setFilesPerModule(int i) {
    this.filesPerModule = i;
    return this;
  }

  public File build(File dir) {
    for (String moduleKey : moduleKeys) {
      generateModule(moduleKey, new File(dir, moduleKey), new Properties());
    }
    Properties additionalProps = new Properties();
    additionalProps.setProperty("sonar.modules", StringUtils.join(moduleKeys, ","));
    generateModule(key, dir, additionalProps);
    return dir;
  }

  private void generateModule(String key, File dir, Properties additionalProps) {
    try {
      File sourceDir = new File(dir, "src");
      FileUtils.forceMkdir(sourceDir);
      for (int i = 0; i < filesPerModule; i++) {
        File sourceFile = new File(sourceDir, "File" + i + ".xoo");
        FileUtils.write(sourceFile, "content of " + sourceFile.getName());
      }
      Properties props = new Properties();
      props.setProperty("sonar.projectKey", key);
      props.setProperty("sonar.projectName", key);
      props.setProperty("sonar.projectVersion", "1.0");
      props.setProperty("sonar.sources", sourceDir.getName());
      props.putAll(additionalProps);
      File propsFile = new File(dir, "sonar-project.properties");
      try (OutputStream output = FileUtils.openOutputStream(propsFile)) {
        props.store(output, "generated");
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
