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
package org.sonar.batch;

import org.junit.Test;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AbstractMavenPluginExecutorTest {

  @Test
  public void pluginVersionIsOptional() {
    assertThat(AbstractMavenPluginExecutor.getGoal("group", "artifact", null, "goal"), is("group:artifact::goal"));
  }

  static class FakeCheckstyleMavenPluginHandler implements MavenPluginHandler {
    public String getGroupId() {
      return "org.apache.maven.plugins";
    }

    public String getArtifactId() {
      return "maven-checkstyle-plugin";
    }

    public String getVersion() {
      return "2.2";
    }

    public boolean isFixedVersion() {
      return false;
    }

    public String[] getGoals() {
      return new String[] { "checkstyle" };
    }

    public void configure(Project project, MavenPlugin plugin) {
    }
  }

}
