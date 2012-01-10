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
package org.sonar.plugins.squid;

import org.apache.commons.io.FileUtils;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.utils.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;

public final class SquidTestUtils {

  private SquidTestUtils() {
  }

  /**
   * See http://svn.apache.org/repos/asf/struts/struts1/tags/STRUTS_1_3_9/core
   */
  public static Collection<InputFile> getStrutsCoreSources() throws IOException, URISyntaxException {
    File sourceDir = new File("target/struts-core-1.3.9-sources");
    if (!sourceDir.exists() || sourceDir.list().length==0) {
      FileUtils.forceMkdir(sourceDir);
      ZipUtils.unzip(new File(SquidTestUtils.class.getResource("/struts-core-1.3.9-sources.jar").toURI()), sourceDir);
    }
    Collection<File> javaFiles = FileUtils.listFiles(sourceDir, new String[]{"java"}, true);

    return InputFileUtils.create(sourceDir, javaFiles);
  }

  /**
   * See http://svn.apache.org/repos/asf/struts/struts1/tags/STRUTS_1_3_9/core
   */
  public static File getStrutsCoreJar() throws IOException, URISyntaxException {
    return new File(SquidTestUtils.class.getResource("/struts-core-1.3.9.jar").toURI());
  }
}
