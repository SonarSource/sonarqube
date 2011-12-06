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

/**
 * Simplifies construction of <a href="http://en.wikipedia.org/wiki/Generalised_suffix_tree">generalised suffix-tree</a>.
 */
public class TextSet extends AbstractText {

  int[] lens;

  public TextSet(int size) {
    super(100); // FIXME

    lens = new int[size];
  }

  public TextSet(Text... text) {
    this(text.length);
    for (int i = 0; i < text.length; i++) {
      symbols.addAll(text[i].sequence(0, text[i].length()));
      symbols.add(new Terminator(i));
      lens[i] = symbols.size();
    }
  }

  public static class Terminator {

    private final int stringNumber;

    public Terminator(int i) {
      this.stringNumber = i;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof Terminator) && (((Terminator) obj).stringNumber == stringNumber);
    }

    @Override
    public int hashCode() {
      return stringNumber;
    }

    public int getStringNumber() {
      return stringNumber;
    }

    @Override
    public String toString() {
      return "$" + stringNumber;
    }

  }

}
