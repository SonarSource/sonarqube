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
package org.sonar.plugins.scm.git;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class GitScmProviderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void sanityCheck() {
    assertThat(new GitScmProvider(null, null, null).key()).isEqualTo("git");
  }

  @Test
  public void selectImplem() {
    GitBlameCommand blameCommand = new GitBlameCommand();
    JGitBlameCommand jblameCommand = new JGitBlameCommand();
    Settings settings = new Settings(new PropertyDefinitions(new GitPlugin().getExtensions()));
    GitScmProvider gitScmProvider = new GitScmProvider(settings, blameCommand, jblameCommand);

    assertThat(gitScmProvider.blameCommand()).isEqualTo(jblameCommand);

    settings.setProperty(GitPlugin.GIT_IMPLEMENTATION_PROP_KEY, GitPlugin.EXE);
    assertThat(gitScmProvider.blameCommand()).isEqualTo(blameCommand);

    settings.setProperty(GitPlugin.GIT_IMPLEMENTATION_PROP_KEY, GitPlugin.JGIT);
    assertThat(gitScmProvider.blameCommand()).isEqualTo(jblameCommand);

    settings.setProperty(GitPlugin.GIT_IMPLEMENTATION_PROP_KEY, "foo");
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Illegal value for " + GitPlugin.GIT_IMPLEMENTATION_PROP_KEY + ": foo");
    gitScmProvider.blameCommand();
  }

  @Test
  public void testAutodetection() throws IOException {
    File baseDirEmpty = temp.newFolder();
    assertThat(new GitScmProvider(null, null, null).supports(baseDirEmpty)).isFalse();

    File gitBaseDir = temp.newFolder();
    new File(gitBaseDir, ".git").mkdir();
    assertThat(new GitScmProvider(null, null, null).supports(gitBaseDir)).isTrue();
  }

}
