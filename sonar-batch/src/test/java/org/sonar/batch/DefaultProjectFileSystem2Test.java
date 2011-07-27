/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.batch;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;

import java.io.File;

import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

public class DefaultProjectFileSystem2Test {
  @Test
  public void shouldIgnoreInexistingSourceDirs() {
    File exists = FileUtils.toFile(getClass().getResource("/org/sonar/batch/DefaultProjectFileSystem2Test/shouldIgnoreInexistingSourceDirs"));
    File notExists = new File("target/unknown");

    ProjectDefinition definition = ProjectDefinition.create().addSourceDirs(exists, notExists);

    DefaultProjectFileSystem2 fs = new DefaultProjectFileSystem2(new Project("foo"), new Languages(), definition);
    assertThat(fs.getSourceDirs().size(), Is.is(1));
    assertThat(fs.getSourceDirs(), hasItem(exists));

  }
}
