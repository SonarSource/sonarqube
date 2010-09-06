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
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ArtifactTest {

  @Test
  public void shouldCreateAllSubdirectories() throws IOException {
    Artifact artifact = createArtifact();
    File root = createTestRootDir();
    artifact.createDir(root);
    assertThat(new File(root, "org/sonar/test/parent/1.0/").exists(), is(true));
  }

  @Test
  public void shouldReplaceTokensInMetadata() throws IOException {
    Artifact artifact = createArtifact();
    String metadata = artifact.getMetadata();
    assertThat(metadata.contains("$"), is(false));
    assertThat(metadata.contains("<groupId>org.sonar.test</groupId>"), is(true));
    assertThat(metadata.contains("<artifactId>parent</artifactId>"), is(true));
    assertThat(metadata.contains("<version>1.0</version>"), is(true));
    assertThat(metadata.contains("<lastUpdated>1.0</lastUpdated>"), is(true));
  }

  @Test
  public void testEquals() {
    assertThat(createArtifact().equals(createArtifact()), is(true));
  }

  private File createTestRootDir() throws IOException {
    File dir = new File("./target/org/sonar/server/mavendeployer/ArtifactTest/");
    FileUtils.deleteQuietly(dir);
    FileUtils.forceMkdir(dir);
    return dir;
  }

  private Artifact createArtifact() {
    return new Artifact("org.sonar.test", "parent", "1.0", "jar", null);
  }
}
