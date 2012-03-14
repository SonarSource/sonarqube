/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.diff;

import org.junit.Test;
import org.sonar.diff.Edit.Type;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DiffTest {

  @Test
  public void emptyInputs() {
    List<Edit> r = diff(t(""), t(""));
    assertThat(r.isEmpty(), is(true));
  }

  @Test
  public void createFile() {
    List<Edit> r = diff(t(""), t("AB"));
    assertThat(r.size(), is(1));
    assertThat(r.get(0), is(new Edit(Type.INSERT, -1, -1, 0, 1)));
  }

  @Test
  public void deleteFile() {
    List<Edit> r = diff(t("AB"), t(""));
    assertThat(r.size(), is(0));
  }

  @Test
  public void insertMiddle() {
    List<Edit> r = diff(t("ac"), t("aBc"));
    assertThat(r.size(), is(3));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 0, 0, 0, 0)));
    assertThat(r.get(1), is(new Edit(Type.INSERT, -1, -1, 1, 1)));
    assertThat(r.get(2), is(new Edit(Type.MOVE, 1, 1, 2, 2)));
  }

  @Test
  public void deleteMiddle() {
    List<Edit> r = diff(t("aBc"), t("ac"));
    assertThat(r.size(), is(2));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 0, 0, 0, 0)));
    assertThat(r.get(1), is(new Edit(Type.MOVE, 2, 2, 1, 1)));
  }

  @Test
  public void replaceMiddle() {
    List<Edit> r = diff(t("aBc"), t("aDc"));
    assertThat(r.size(), is(3));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 0, 0, 0, 0)));
    assertThat(r.get(1), is(new Edit(Type.INSERT, -1, -1, 1, 1)));
    assertThat(r.get(2), is(new Edit(Type.MOVE, 2, 2, 2, 2)));
  }

  @Test
  public void insertStart() {
    List<Edit> r = diff(t("bc"), t("Abc"));
    assertThat(r.size(), is(2));
    assertThat(r.get(0), is(new Edit(Type.INSERT, -1, -1, 0, 0)));
    assertThat(r.get(1), is(new Edit(Type.MOVE, 0, 1, 1, 2)));
  }

  @Test
  public void deleteStart() {
    List<Edit> r = diff(t("Abc"), t("bc"));
    assertThat(r.size(), is(1));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 1, 2, 0, 1)));
  }

  @Test
  public void insertEnd() {
    List<Edit> r = diff(t("ab"), t("abC"));
    assertThat(r.size(), is(2));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 0, 1, 0, 1)));
    assertThat(r.get(1), is(new Edit(Type.INSERT, -1, -1, 2, 2)));
  }

  @Test
  public void deleteEnd() {
    List<Edit> r = diff(t("abC"), t("ab"));
    assertThat(r.size(), is(1));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 0, 1, 0, 1)));
  }

  /**
   * This is important special case, for which other algorithms can not detect movement.
   */
  @Test
  public void move() {
    List<Edit> r = diff(t("Abc"), t("bcA"));
    assertThat(r.size(), is(2));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 1, 2, 0, 1)));
    assertThat(r.get(1), is(new Edit(Type.MOVE, 0, 0, 2, 2)));
  }

  @Test
  public void move2() {
    List<Edit> r = diff(t("abcd"), t("abcda"));
    assertThat(r.size(), is(2));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 0, 3, 0, 3)));
    assertThat(r.get(1), is(new Edit(Type.MOVE, 0, 0, 4, 4)));
  }

  @Test
  public void move3() {
    List<Edit> r = diff(t("abcd"), t("bcdaa"));
    assertThat(r.size(), is(3));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 1, 3, 0, 2)));
    assertThat(r.get(1), is(new Edit(Type.MOVE, 0, 0, 3, 3)));
    assertThat(r.get(2), is(new Edit(Type.MOVE, 0, 0, 4, 4)));
  }

  @Test
  public void severalInserts() {
    List<Edit> r = diff(t("ac"), t("aBcD"));
    assertThat(r.size(), is(4));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 0, 0, 0, 0)));
    assertThat(r.get(1), is(new Edit(Type.INSERT, -1, -1, 1, 1)));
    assertThat(r.get(2), is(new Edit(Type.MOVE, 1, 1, 2, 2)));
    assertThat(r.get(3), is(new Edit(Type.INSERT, -1, -1, 3, 3)));
  }

  @Test
  public void insertSeveralLines() {
    List<Edit> r = diff(t("ade"), t("aBCde"));
    assertThat(r.size(), is(3));
    assertThat(r.get(0), is(new Edit(Type.MOVE, 0, 0, 0, 0)));
    assertThat(r.get(1), is(new Edit(Type.INSERT, -1, -1, 1, 2)));
    assertThat(r.get(2), is(new Edit(Type.MOVE, 1, 2, 3, 4)));
  }

  private List<Edit> diff(Text a, Text b) {
    return new DiffAlgorithm().diff(a, b, TextComparator.DEFAULT);
  }

  public static Text t(String text) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      sb.append(text.charAt(i)).append('\n');
    }
    try {
      return new Text(sb.toString().getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

}
