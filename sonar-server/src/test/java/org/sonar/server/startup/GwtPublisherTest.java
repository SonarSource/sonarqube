/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.SonarException;
import org.sonar.api.web.GwtExtension;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GwtPublisherTest {
  private File outputDir;

  @Before
  public void cleanOutput() throws IOException {
    outputDir = new File("./target/test-tmp/org/sonar/server/startup/GwtPublisherTest/output");
    if (outputDir.exists()) {
      FileUtils.forceDelete(outputDir);

    }
  }

  @Test
  public void shouldCopyAllFilesToOutputDir() throws IOException, URISyntaxException {
    GwtExtension module1 = mock(GwtExtension.class);
    when(module1.getGwtId()).thenReturn("org.sonar.server.startup.GwtPublisherTest.module1");

    GwtExtension module2 = mock(GwtExtension.class);
    when(module2.getGwtId()).thenReturn("org.sonar.server.startup.GwtPublisherTest.module2");

    GwtPublisher publisher = new GwtPublisher(new GwtExtension[]{module1, module2}, outputDir);
    publisher.publish();

    assertThat(new File("./target/test-tmp/org/sonar/server/startup/GwtPublisherTest/output/org.sonar.server.startup.GwtPublisherTest.module1/one.js").exists(), is(true));
    assertThat(new File("./target/test-tmp/org/sonar/server/startup/GwtPublisherTest/output/org.sonar.server.startup.GwtPublisherTest.module1/two.css").exists(), is(true));
    assertThat(new File("./target/test-tmp/org/sonar/server/startup/GwtPublisherTest/output/org.sonar.server.startup.GwtPublisherTest.module2/file.js").exists(), is(true));
  }

  @Test(expected = SonarException.class)
  public void shouldFailIfGwtSourcesNotFound() throws IOException, URISyntaxException {
    GwtExtension component = mock(GwtExtension.class);
    when(component.getGwtId()).thenReturn("org.sonar.server.startup.GwtPublisherTest.unknownmodule");

    GwtPublisher publisher = new GwtPublisher(new GwtExtension[]{component}, outputDir);
    publisher.publish();
  }

  @Test
  public void shouldCleanTheOutputDirOnStop() throws IOException {
    File dir = new File("./target/test-tmp/org/sonar/server/startup/GwtPublisherTest/shouldCleanTheOutputDirOnStop");
    if (!dir.exists()) {
      FileUtils.forceMkdir(dir);
    }
    File file = new File(dir, "test.txt");
    FileUtils.writeStringToFile(file, "test");
    File testDir = new File(dir, "testDir");
    testDir.mkdir();
    File testDirFile = new File(testDir, "test.txt");
    FileUtils.writeStringToFile(testDirFile, "test");

    File scm = new File(dir, ".svn");
    scm.mkdir();

    assertThat(file.exists(), is(true));
    assertThat(testDir.exists(), is(true));
    assertThat(testDirFile.exists(), is(true));
    assertThat(scm.exists(), is(true));

    GwtPublisher publisher = new GwtPublisher(null, dir);
    publisher.cleanDirectory();
    assertThat(dir.exists(), is(true));
    assertThat(FileUtils.listFiles(dir, null, true).size(), is(0));
    scm = new File(dir, ".svn");
    // won't be hidden under windows and test will fail, no hidden setter on file unfortunatly
    if (scm.isHidden()) {
      assertThat(scm.exists(), is(true));
    }
  }

  @Test
  public void shouldGetPathWithValidCharacterEvenIfSpaceInPath() throws Exception {
    GwtPublisher publisher = new GwtPublisher();
    String path = "/org/sonar/server/startup/GwtPublisherTest/path with space/archive.jar";
    URL sourceDir = getClass().getResource(path);

    String result = publisher.getCleanPath(sourceDir.toString());
    assertThat(result, not(containsString("%20")));
    assertThat(result, containsString(" "));
  }
}
