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
package org.sonar.duplications.detector.suffixtree;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SuffixTreeTest {

  @Parameters
  public static Collection<Object[]> generateData() {
    return Arrays.asList(new Object[][] { { "banana" }, { "mississippi" }, { "book" }, { "bookke" }, { "cacao" }, { "googol" }, { "abababc" }, { "aaaaa" } });
  }

  private final String data;

  public SuffixTreeTest(String data) {
    this.data = data;
  }

  @Test
  public void test() {
    String text = this.data + "$";
    StringSuffixTree tree = StringSuffixTree.create(text);

    assertThat("number of leaves", tree.getNumberOfLeafs(), is(text.length()));
    assertThat("number of inner nodes", tree.getNumberOfInnerNodes(), lessThan(text.length() - 1));
    assertThat("number of edges", tree.getNumberOfEdges(), is(tree.getNumberOfInnerNodes() + tree.getNumberOfLeafs()));

    for (int beginIndex = 0; beginIndex < text.length(); beginIndex++) {
      for (int endIndex = beginIndex + 1; endIndex < text.length() + 1; endIndex++) {
        String substring = text.substring(beginIndex, endIndex);
        assertThat("index of " + substring + " in " + text, tree.indexOf(substring), is(text.indexOf(substring)));
      }
    }
  }

}
