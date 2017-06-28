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