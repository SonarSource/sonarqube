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
package org.sonar.api.batch.fs.internal.charhandler;

public class LineOffsetCounter extends CharHandler {
  private long currentOriginalOffset = 0;
  private IntArrayList originalLineOffsets = new IntArrayList();
  private long lastValidOffset = 0;

  public LineOffsetCounter() {
    originalLineOffsets.add(0);
  }

  @Override
  public void handleAll(char c) {
    currentOriginalOffset++;
  }

  @Override
  public void newLine() {
    if (currentOriginalOffset > Integer.MAX_VALUE) {
      throw new IllegalStateException("File is too big: " + currentOriginalOffset);
    }
    originalLineOffsets.add((int) currentOriginalOffset);
  }

  @Override
  public void eof() {
    lastValidOffset = currentOriginalOffset;
  }

  public int[] getOriginalLineOffsets() {
    return originalLineOffsets.trimAndGet();
  }

  public int getLastValidOffset() {
    if (lastValidOffset > Integer.MAX_VALUE) {
      throw new IllegalStateException("File is too big: " + lastValidOffset);
    }
    return (int) lastValidOffset;
  }

}
