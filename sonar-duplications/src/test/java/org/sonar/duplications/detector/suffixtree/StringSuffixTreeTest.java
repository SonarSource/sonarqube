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
package org.sonar.duplications.detector.suffixtree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringSuffixTreeTest {

  @Test
  public void testMississippi() {
    StringSuffixTree st = StringSuffixTree.create("mississippi");

    assertTrue(st.contains("miss"));
    assertTrue(st.contains("missis"));
    assertTrue(st.contains("pi"));
  }

  @Test
  public void testBanana() {
    StringSuffixTree st = StringSuffixTree.create("banana");

    assertTrue(st.contains("ana"));
    assertTrue(st.contains("an"));
    assertTrue(st.contains("na"));
  }

  @Test
  public void testBook() {
    StringSuffixTree st = StringSuffixTree.create("book");

    assertTrue(st.contains("book"));
    assertTrue(st.contains("oo"));
    assertTrue(st.contains("ok"));
    assertFalse(st.contains("okk"));
    assertFalse(st.contains("bookk"));
    assertFalse(st.contains("bok"));

    assertEquals(0, st.indexOf("book"));
    assertEquals(1, st.indexOf("oo"));
    assertEquals(2, st.indexOf("ok"));
  }

  @Test
  public void testBookke() {
    StringSuffixTree st = StringSuffixTree.create("bookke");

    assertTrue(st.contains("bookk"));

    assertEquals(0, st.indexOf("book"));
    assertEquals(1, st.indexOf("oo"));
    assertEquals(2, st.indexOf("ok"));
  }

  @Test
  public void testCacao() {
    StringSuffixTree st = StringSuffixTree.create("cacao");

    assertTrue(st.contains("aca"));

    assertEquals(3, st.indexOf("ao"));
    assertEquals(0, st.indexOf("ca"));
    assertEquals(2, st.indexOf("cao"));
  }

  @Test
  public void testGoogol() {
    StringSuffixTree st = StringSuffixTree.create("googol");

    assertTrue(st.contains("oo"));

    assertEquals(0, st.indexOf("go"));
    assertEquals(3, st.indexOf("gol"));
    assertEquals(1, st.indexOf("oo"));
  }

  @Test
  public void testAbababc() {
    StringSuffixTree st = StringSuffixTree.create("abababc");

    assertTrue(st.contains("aba"));

    assertEquals(0, st.indexOf("aba"));
    assertEquals(4, st.indexOf("abc"));
  }
}
