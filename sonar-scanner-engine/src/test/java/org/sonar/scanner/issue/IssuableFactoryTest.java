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
package org.sonar.scanner.issue;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.issue.Issuable;
import org.sonar.scanner.sensor.DefaultSensorContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IssuableFactoryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void file_should_be_issuable() {
    IssuableFactory factory = new IssuableFactory(mock(DefaultSensorContext.class));
    Issuable issuable = factory.loadPerspective(Issuable.class, new TestInputFileBuilder("foo", "src/Foo.java").build());

    assertThat(issuable).isNotNull();
    assertThat(issuable.issues()).isEmpty();
  }

  @Test
  public void project_should_be_issuable() throws IOException {
    IssuableFactory factory = new IssuableFactory(mock(DefaultSensorContext.class));
    Issuable issuable = factory.loadPerspective(Issuable.class,
      new DefaultInputModule(ProjectDefinition.create().setKey("foo").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder())));

    assertThat(issuable).isNotNull();
    assertThat(issuable.issues()).isEmpty();
  }
}
