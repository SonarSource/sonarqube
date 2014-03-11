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
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.DependencyTree;

import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DependencyTreeUnmarshallerTest extends UnmarshallerTestCase {
  @Test
  public void singleDepthOfDependencies() {
    List<DependencyTree> trees = new DependencyTreeUnmarshaller().toModels("[]");
    assertThat(trees.size(), is(0));

    trees = new DependencyTreeUnmarshaller().toModels(loadFile("/dependency_tree/single_depth.json"));
    assertThat(trees.size(), is(2));
    assertThat(trees.get(0).getDepId(), is("12345"));
    assertThat(trees.get(0).getResourceId(), is("2000"));
    assertThat(trees.get(0).getResourceName(), is("Commons Lang"));
    assertThat(trees.get(0).getResourceVersion(), is("1.4"));
    assertThat(trees.get(0).getResourceScope(), is("PRJ"));
    assertThat(trees.get(0).getResourceQualifier(), is("LIB"));
    assertThat(trees.get(0).getUsage(), is("compile"));
    assertThat(trees.get(0).getWeight(), is(1));
    assertThat(trees.get(0).getTo().size(), is(0));
  }

  @Test
  public void manyDepthsOfDependencies() {
    List<DependencyTree> trees = new DependencyTreeUnmarshaller().toModels(loadFile("/dependency_tree/many_depths.json"));
    assertThat(trees.size(), is(1));
    List<DependencyTree> secondLevelTrees = trees.get(0).getTo();
    assertThat(secondLevelTrees.size(), is(2));

    assertThat(secondLevelTrees.get(0).getDepId(), is("12346"));
    assertThat(secondLevelTrees.get(0).getResourceName(), is("SLF4J"));

    assertThat(secondLevelTrees.get(1).getDepId(), is("12347"));
    assertThat(secondLevelTrees.get(1).getResourceName(), is("Commons Lang"));
  }

}
