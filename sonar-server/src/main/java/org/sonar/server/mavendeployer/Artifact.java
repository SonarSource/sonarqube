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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Artifact {

  public static final String BASE_GROUP_ID = "org.codehaus.sonar.runtime";

  private String groupId;
  private String artifactId;
  protected String version;
  private Artifact[] dependencies;
  protected File jar;
  private String packaging;

  public Artifact(String groupId, String artifactId, String version, String packaging, File jar, Artifact... deps) {
    this.artifactId = artifactId;
    this.groupId = groupId;
    this.version = version;
    this.dependencies = deps;
    this.jar = jar;
    this.packaging = packaging;
  }

  public void deployTo(File rootDir, boolean deployDependencies) throws IOException {
    if (deployDependencies && dependencies != null) {
      for (Artifact dependency : dependencies) {
        dependency.deployTo(rootDir, true);
      }
    }

    File dir = createDir(rootDir);
    savePom(dir);
    saveMetadata(dir);
    saveJar(dir);
  }

  protected File createDir(File rootDir) throws IOException {
    String path = StringUtils.replace(groupId, ".", "/") + "/" + artifactId + "/" + version;
    File dir = new File(rootDir, path);
    FileUtils.forceMkdir(dir);
    return dir;
  }

  protected String getArtifactName() {
    return artifactId + "-" + version;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public Artifact[] getDependencies() {
    return dependencies;
  }

  public File getJar() {
    return jar;
  }

  public String getPackaging() {
    return packaging;
  }

  private void saveJar(File dir) throws IOException {
    if (jar != null) {
      copyTo(dir);
      File newJar = new File(dir, jar.getName());
      File target = new File(dir, getArtifactName() + ".jar");
      newJar.renameTo(target);
      saveDigests(target);
    }
  }

  protected void copyTo(File dir) throws IOException {
    FileUtils.copyFileToDirectory(jar, dir);
  }

  private void savePom(File dir) throws IOException {
    File pom = new File(dir, getArtifactName() + ".pom");
    FileUtils.writeStringToFile(pom, getPom(), CharEncoding.UTF_8);
    saveDigests(pom);
  }

  private void saveDigests(File file) throws IOException {
    String path = file.getAbsolutePath();
    byte[] content = FileUtils.readFileToByteArray(file);
    FileUtils.writeStringToFile(new File(path + ".md5"), DigestUtils.md5Hex(content), CharEncoding.UTF_8);
    FileUtils.writeStringToFile(new File(path + ".sha1"), DigestUtils.shaHex(content), CharEncoding.UTF_8);
  }

  public String getPom() throws IOException {
    return transformFromTemplatePath(getTemplatePath(), getDependenciesAsString());
  }

  protected String getTemplatePath() {
    return "/org/sonar/server/mavendeployer/pom.template";
  }

  private String getDependenciesAsString() throws IOException {
    StringBuilder sb = new StringBuilder();
    if (dependencies != null) {
      for (Artifact dependency : dependencies) {
        sb.append(dependency.getXmlDefinition());
      }
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Artifact that = (Artifact) o;
    if (!artifactId.equals(that.artifactId)) {
      return false;
    }
    return groupId.equals(that.groupId);
  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + artifactId.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return new StringBuilder().append(groupId).append(":").append(artifactId).toString();
  }

  public String getMetadata() throws IOException {
    return transformFromTemplatePath("/org/sonar/server/mavendeployer/maven-metadata.template");
  }

  protected void saveMetadata(File dir) throws IOException {
    File metadataFile = new File(dir.getParentFile(), "maven-metadata.xml");
    FileUtils.writeStringToFile(metadataFile, getMetadata(), CharEncoding.UTF_8);
  }

  protected String transformFromTemplatePath(String templatePath) throws IOException {
    return transformFromTemplatePath(templatePath, "");
  }

  protected final String transformFromTemplatePath(String templatePath, String depsXml) throws IOException {
    InputStream template = this.getClass().getResourceAsStream(templatePath);
    try {
      String content = IOUtils.toString(template);
      content = StringUtils.replace(content, "$groupId", groupId);
      content = StringUtils.replace(content, "$artifactId", artifactId);
      content = StringUtils.replace(content, "$version", version);
      content = StringUtils.replace(content, "$timestamp", version);
      content = StringUtils.replace(content, "$packaging", packaging);
      content = StringUtils.replace(content, "$dependencies", StringUtils.defaultString(depsXml, ""));
      return content;

    } finally {
      IOUtils.closeQuietly(template);
    }
  }

  public String getXmlDefinition() throws IOException {
    return transformFromTemplatePath("/org/sonar/server/mavendeployer/dependency.template");
  }
}
