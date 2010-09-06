/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.mavendeployer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.ZipUtils;

import java.io.File;
import java.io.IOException;

public final class Mojo extends Artifact {

  private Mojo(String artifactId, String version, File jar, Artifact... deps) {
    super(BASE_GROUP_ID, artifactId, version, "maven-plugin", jar, deps);
  }

  public static Mojo createMaven2Plugin(String version, File jar, Artifact... deps) {
    return new Mojo("sonar-core-maven-plugin", version, jar, deps);
  }

  public static Mojo createMaven3Plugin(String version, File jar, Artifact... deps) {
    return new Mojo("sonar-core-maven3-plugin", version, jar, deps);
  }

  @Override
  protected String getTemplatePath() {
    return "/org/sonar/server/mavendeployer/" + getArtifactId() + ".template";
  }

  @Override
  protected void copyTo(File toDir) throws IOException {
    File tmpDir = prepareTmpDir();
    try {
      copyTo(toDir, tmpDir);

    } finally {
      destroyTmpDir(tmpDir);
    }
  }

  protected void copyTo(File toDir, File temporaryDir) throws IOException {
    ZipUtils.unzip(jar, temporaryDir);

    File metadataFile = new File(temporaryDir, "META-INF/maven/plugin.xml");
    String metadata = FileUtils.readFileToString(metadataFile);
    metadata = updateVersion(metadata, version);

    FileUtils.writeStringToFile(metadataFile, metadata);
    ZipUtils.zipDir(temporaryDir, new File(toDir, jar.getName()));
  }

  protected String updateVersion(String metadata, String version) {
    String before = StringUtils.substringBefore(metadata, "<version>");
    String after = StringUtils.substringAfter(metadata, "</version>");
    return before + "<version>" + version + "</version>" + after;
  }

  private File prepareTmpDir() throws IOException {
    File tmpDir = new File(System.getProperty("java.io.tmpdir"), "sonar-" + version);
    if (tmpDir.exists()) {
      FileUtils.cleanDirectory(tmpDir);
    } else {
      FileUtils.forceMkdir(tmpDir);
    }
    return tmpDir;
  }

  private void destroyTmpDir(File tmpDir) {
    if (tmpDir != null) {
      FileUtils.deleteQuietly(tmpDir);
    }
  }
}
