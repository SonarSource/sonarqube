/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.channel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration parameters used by a CodeReader to handle some specificities.
 */
public class CodeReaderConfiguration {

  /**
   * @deprecated in 2.12, do not use anymore.
   */
  @Deprecated
  public static final int DEFAULT_BUFFER_CAPACITY = 8000;

  public static final int DEFAULT_TAB_WIDTH = 1;

  private int tabWidth = DEFAULT_TAB_WIDTH;

  private List<CodeReaderFilter<?>> codeReaderFilters = new ArrayList<CodeReaderFilter<?>>();

  /**
   * @deprecated in 2.12, do not use anymore.
   * @return the constant Integer.MAX_VALUE
   */
  @Deprecated
  public int getBufferCapacity() {
    return Integer.MAX_VALUE;
  }

  /**
   * @deprecated in 2.12, do not use anymore.
   * @param bufferCapacity
   *          the bufferCapacity to set
   */
  @Deprecated
  public void setBufferCapacity(int bufferCapacity) {
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
    return codeReaderFilters.toArray(new CodeReaderFilter[codeReaderFilters.size()]);
  }

  /**
   * @param codeReaderFilters
   *          the codeReaderFilters to set
   */
  public void setCodeReaderFilters(CodeReaderFilter<?>... codeReaderFilters) {
    this.codeReaderFilters = new ArrayList<CodeReaderFilter<?>>(Arrays.asList(codeReaderFilters));
  }

  /**
   * Adds a code reader filter
   *
   * @param codeReaderFilter
   *          the codeReaderFilter to add
   */
  public void addCodeReaderFilters(CodeReaderFilter<?> codeReaderFilter) {
    this.codeReaderFilters.add(codeReaderFilter);
  }

  public CodeReaderConfiguration cloneWithoutCodeReaderFilters() {
    CodeReaderConfiguration clone = new CodeReaderConfiguration();
    clone.setTabWidth(tabWidth);
    return clone;
  }

}
