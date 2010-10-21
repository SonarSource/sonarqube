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
package org.sonar.channel;

/**
 * Configuration parameters used by a CodeReader to handle some specificities.
 */
public class CodeReaderConfiguration {

  private final static int DEFAULT_BUFFER_CAPACITY = 8000;

  private final static int DEFAULT_TAB_WIDTH = 1;

  private int bufferCapacity = DEFAULT_BUFFER_CAPACITY;

  private int tabWidth = DEFAULT_TAB_WIDTH;

  private CodeReaderFilter<?>[] codeReaderFilters = new CodeReaderFilter<?>[0];

  /**
   * @return the bufferCapacity
   */
  public int getBufferCapacity() {
    return bufferCapacity;
  }

  /**
   * @param bufferCapacity
   *          the bufferCapacity to set
   */
  public void setBufferCapacity(int bufferCapacity) {
    this.bufferCapacity = bufferCapacity;
  }

  /**
   * @return the tabWidth
   */
  public int getTabWidth() {
    return tabWidth;
  }

  /**
   * @param tabWidth
   *          the tabWidth to set
   */
  public void setTabWidth(int tabWidth) {
    this.tabWidth = tabWidth;
  }

  /**
   * @return the codeReaderFilters
   */
  @SuppressWarnings("rawtypes")
  public CodeReaderFilter[] getCodeReaderFilters() {
    return codeReaderFilters;
  }

  /**
   * @param codeReaderFilters
   *          the codeReaderFilters to set
   */
  public void setCodeReaderFilters(CodeReaderFilter<?>... codeReaderFilters) {
    this.codeReaderFilters = codeReaderFilters;
  }

  public CodeReaderConfiguration cloneWithoutCodeReaderFilters() {
    CodeReaderConfiguration clone = new CodeReaderConfiguration();
    clone.setBufferCapacity(bufferCapacity);
    clone.setTabWidth(tabWidth);
    return clone;
  }

}
