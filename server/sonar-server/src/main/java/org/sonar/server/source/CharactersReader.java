/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

class CharactersReader {

  static final int END_OF_STREAM = -1;

  private final BufferedReader stringBuffer;
  private final Deque<String> openTags;

  private int currentValue;
  private int previousValue;
  private int currentIndex = -1;

  public CharactersReader(BufferedReader stringBuffer) {
    this.stringBuffer = stringBuffer;
    this.openTags = new ArrayDeque<>();
  }

  boolean readNextChar() throws IOException {
    previousValue = currentValue;
    currentValue = stringBuffer.read();
    currentIndex++;
    return currentValue != END_OF_STREAM;
  }

  int getCurrentValue() {
    return currentValue;
  }

  int getPreviousValue() {
    return previousValue;
  }

  int getCurrentIndex() {
    return currentIndex;
  }

  void registerOpenTag(String textType) {
    openTags.push(textType);
  }

  void removeLastOpenTag() {
    openTags.remove();
  }

  Deque<String> getOpenTags() {
    return openTags;
  }
}
