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
package org.sonar.duplications.detector.suffixtree;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SuffixTreeTest {

  @Parameters
  public static Collection<Object[]> generateData() {
    return Arrays.asList(new Object[][] { {"banana"}, {"mississippi"}, {"book"}, {"bookke"}, {"cacao"}, {"googol"}, {"abababc"}, {"aaaaa"}});
  }

  private final String data;

  public SuffixTreeTest(String data) {
    this.data = data;
  }

  @Test
  public void test() {
    String text = this.data + "$";
    StringSuffixTree tree = StringSuffixTree.create(text);

    assertThat(tree.getNumberOfLeafs()).as("number of leaves").isEqualTo(text.length());
    assertThat(tree.getNumberOfInnerNodes()).as("number of inner nodes").isLessThan(text.length() - 1);
    assertThat(tree.getNumberOfEdges()).as("number of edges").isEqualTo(tree.getNumberOfInnerNodes() + tree.getNumberOfLeafs());

    for (int beginIndex = 0; beginIndex < text.length(); beginIndex++) {
      for (int endIndex = beginIndex + 1; endIndex < text.length() + 1; endIndex++) {
        String substring = text.substring(beginIndex, endIndex);
        assertThat(tree.indexOf(substring)).as("index of " + substring + " in " + text).isEqualTo(text.indexOf(substring));
      }
    }
  }

}
