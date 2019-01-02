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
package org.sonar.api.web;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.SonarException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @since 1.11
 * @deprecated since 6.3. This class is ignored.
 */
@Deprecated
public abstract class AbstractRubyTemplate {

  private String cache = null;

  public String getTemplate() {
    String result = loadTemplateFromCache();
    try {
      if (result == null) {
        result = loadTemplateFromClasspath();
      }
      if (result == null) {
        result = loadTemplateFromAbsolutePath();
      }
      return result;

    } catch (IOException e) {
      throw new SonarException("Can not read the file " + getTemplatePath(), e);
    }
  }

  private String loadTemplateFromAbsolutePath() throws IOException {
    File file = new File(getTemplatePath());
    if (file.exists()) {
      // the result is not cached
      return FileUtils.readFileToString(file, UTF_8);
    }
    throw new FileNotFoundException(getTemplatePath());
  }

  private String loadTemplateFromClasspath() throws IOException {
    InputStream input = getClass().getResourceAsStream(getTemplatePath());
    try {
      if (input != null) {
        cache = IOUtils.toString(input);
        return cache;
      }
    } finally {
      IOUtils.closeQuietly(input);
    }
    return null;
  }

  protected String loadTemplateFromCache() {
    return cache;
  }

  /**
   * the path of the template. In production environment, it's the classloader path (for example "/org/sonar/my_template.erb").
   * In dev mode, it's useful to return an absolute path (for example C:/temp/my_template.erb). In such a case the result is not cached.
   */
  protected abstract String getTemplatePath();

}
