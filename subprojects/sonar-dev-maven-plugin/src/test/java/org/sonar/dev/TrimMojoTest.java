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
package org.sonar.dev;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.File;
import java.io.IOException;

public class TrimMojoTest {

  @Test
  public void trimFilesFromDirectory() throws IOException, MojoFailureException, MojoExecutionException {
    File dir = newDir("trimFilesFromDirectory");
    File file1 = copyResourceToDir(1, dir);
    File file2 = copyResourceToDir(2, dir);

    TrimMojo mojo = new TrimMojo();
    mojo.setDirectory(dir);
    mojo.execute();

    assertTrimmed(file1);
    assertTrimmed(file2);
  }

  @Test
  public void excludeSomeFiles() throws IOException, MojoFailureException, MojoExecutionException {
    File dir = newDir("excludeSomeFiles");
    File file1 = copyResourceToDir(1, dir);
    File file2 = copyResourceToDir(2, dir);

    TrimMojo mojo = new TrimMojo();
    mojo.setDirectory(dir);
    mojo.setExcludes(new String[]{"**/*-1.txt"});
    mojo.execute();

    assertNotTrimmed(file1);
    assertTrimmed(file2);
  }

  @Test
  public void trimOnlySomeFiles() throws IOException, MojoFailureException, MojoExecutionException {
    File dir = newDir("trimOnlySomeFiles");
    File file1 = copyResourceToDir(1, dir);
    File file2 = copyResourceToDir(2, dir);

    TrimMojo mojo = new TrimMojo();
    mojo.setDirectory(dir);
    mojo.setIncludes(new String[]{"**/*-1.txt"});
    mojo.execute();

    assertTrimmed(file1);
    assertNotTrimmed(file2);
  }

  private void assertNotTrimmed(File file) throws IOException {
    String content = FileUtils.readFileToString(file);
    assertThat(content, startsWith("         "));
    assertThat(content, containsString("            "));
  }

  private void assertTrimmed(File file) throws IOException {
    String content = FileUtils.readFileToString(file);
    assertThat(content, startsWith("many spaces"));
    assertThat(content, not(containsString("            ")));
    assertThat(content, containsString("white spaces should be  kept  in   the   line"));
  }


  private File copyResourceToDir(int index, File dir) throws IOException {
    File file = new File(dir, "whitespace-indented-" + index + ".txt");
    FileUtils.copyURLToFile(getClass().getResource("/org/sonar/dev/TrimMojoTest/whitespace-indented.txt"), file);
    return file;
  }

  private File newDir(String name) throws IOException {
    File dir = new File("target/tmp/org/sonar/dev/TrimMojoTest/" + name);
    FileUtils.forceMkdir(dir);
    FileUtils.cleanDirectory(dir);
    return dir;
  }
}
