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
package org.sonar.api.test;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.sonar.api.batch.maven.MavenUtils;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class MavenTestUtils {

  public static MavenProject loadPom(Class clazz, String path) {
    String fullpath = "/" + clazz.getName().replaceAll("\\.", "/") + "/" + path;
    return loadPom(fullpath);
  }

  public static MavenProject loadPom(String pomUrlInClasspath) {
    FileReader fileReader = null;
    try {
      File pomFile = new File(MavenTestUtils.class.getResource(pomUrlInClasspath).toURI());
      MavenXpp3Reader pomReader = new MavenXpp3Reader();
      fileReader = new FileReader(pomFile);
      Model model = pomReader.read(fileReader);
      MavenProject project = new MavenProject(model);
      project.setFile(pomFile);
      project.getBuild().setDirectory(pomFile.getParentFile().getPath());
      project.addCompileSourceRoot(pomFile.getParentFile().getPath() + "/src/main/java");
      project.addTestCompileSourceRoot(pomFile.getParentFile().getPath() + "/src/test/java");
      return project;
    } catch (Exception e) {
      throw new SonarException("Failed to read Maven project file : " + pomUrlInClasspath, e);

    } finally {
      IOUtils.closeQuietly(fileReader);
    }
  }

  public static Project loadProjectFromPom(Class clazz, String path) {
    MavenProject pom = loadPom(clazz, path);
    Project project = new Project(pom.getGroupId() + ":" + pom.getArtifactId())
      .setPom(pom);
    // configuration.setProperty("sonar.java.source", MavenUtils.getJavaSourceVersion(pom));
    // configuration.setProperty("sonar.java.target", MavenUtils.getJavaVersion(pom));
    // configuration.setProperty(CoreProperties.ENCODING_PROPERTY, MavenUtils.getSourceEncoding(pom));

    project.setFileSystem(new MavenModuleFileSystem(pom));
    return project;
  }

  public static MavenProject mockPom(String packaging) {
    MavenProject mavenProject = mock(MavenProject.class);
    when(mavenProject.getPackaging()).thenReturn(packaging);
    return mavenProject;
  }

  static class MavenModuleFileSystem implements ProjectFileSystem {
    private MavenProject pom;

    MavenModuleFileSystem(MavenProject pom) {
      this.pom = pom;
    }

    public Charset getSourceCharset() {
      return Charset.forName(MavenUtils.getSourceEncoding(pom));
    }

    public File getBasedir() {
      return pom.getBasedir();
    }

    public File getBuildDir() {
      return new File(pom.getBuild().getDirectory());
    }

    public File getBuildOutputDir() {
      return new File(pom.getBuild().getOutputDirectory());
    }

    public List<File> getSourceDirs() {
      return Arrays.asList(new File(pom.getBuild().getSourceDirectory()));
    }

    public ProjectFileSystem addSourceDir(File dir) {
      throw new UnsupportedOperationException();
    }

    public List<File> getTestDirs() {
      return null;
    }

    public ProjectFileSystem addTestDir(File dir) {
      throw new UnsupportedOperationException();
    }

    public File getReportOutputDir() {
      return null;
    }

    public File getSonarWorkingDirectory() {
      File dir = new File(getBuildDir(), "sonar");
      try {
        FileUtils.forceMkdir(dir);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      return dir;
    }

    public File resolvePath(String path) {
      return new PathResolver().relativeFile(getBasedir(), path);
    }

    public List<File> getSourceFiles(Language... langs) {
      return new ArrayList(FileUtils.listFiles(getSourceDirs().get(0), new String[] {"java"}, true));
    }

    public List<File> getJavaSourceFiles() {
      return getSourceFiles();
    }

    public boolean hasJavaSourceFiles() {
      return !getJavaSourceFiles().isEmpty();
    }

    public List<File> getTestFiles(Language... langs) {
      return new ArrayList(FileUtils.listFiles(getTestDirs().get(0), new String[] {"java"}, true));
    }

    public boolean hasTestFiles(Language lang) {
      return !getTestFiles(lang).isEmpty();
    }

    public File writeToWorkingDirectory(String content, String fileName) throws IOException {
      throw new UnsupportedOperationException();
    }

    public File getFileFromBuildDirectory(String filename) {
      throw new UnsupportedOperationException();
    }

    public Resource toResource(File file) {
      throw new UnsupportedOperationException();
    }

    public List<InputFile> mainFiles(String... langs) {
      throw new UnsupportedOperationException();
    }

    public List<InputFile> testFiles(String... langs) {
      throw new UnsupportedOperationException();
    }
  }
}
