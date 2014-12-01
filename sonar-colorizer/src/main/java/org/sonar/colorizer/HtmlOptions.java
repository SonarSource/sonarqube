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
package org.sonar.colorizer;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @deprecated since 4.5.2 replace by highlighting mechanism
 */
@Deprecated
public class HtmlOptions {
  public static final HtmlOptions DEFAULT = new HtmlOptions(true, null, true);
  public static final OnlySyntaxHtmlOptions ONLY_SYNTAX = new OnlySyntaxHtmlOptions();

  private boolean generateTable = true;
  private boolean generateHtmlHeader = true;
  private String tableId = null;
  private int firstLineId = 1;

  public HtmlOptions() {
  }

  public HtmlOptions(boolean generateTable, @Nullable String tableId, boolean generateHtmlHeader) {
    this.generateTable = generateTable;
    this.generateHtmlHeader = generateHtmlHeader;
    this.tableId = tableId;
  }

  public boolean isGenerateTable() {
    return generateTable;
  }

  public HtmlOptions setGenerateTable(boolean b) {
    this.generateTable = b;
    return this;
  }

  /**
   * Used only if isGenerateTable() is true
   */
  public boolean isGenerateHtmlHeader() {
    return generateHtmlHeader;
  }

  /**
   * Defines if the HTML header, including CSS, must be generated.
   */
  public HtmlOptions setGenerateHtmlHeader(boolean b) {
    this.generateHtmlHeader = b;
    return this;
  }

  @CheckForNull
  public String getTableId() {
    return tableId;
  }

  /**
   * Used only if isGenerateTable() is true. This field is optional.
   */
  public HtmlOptions setTableId(String id) {
    this.tableId = id;
    return this;
  }

  /**
   * Used only if isGenerateTable() is true. Default value is 1.
   */
  public int getFirstLineId() {
    return firstLineId;
  }

  public HtmlOptions setFirstLineId(int i) {
    this.firstLineId = i;
    return this;
  }
}

class OnlySyntaxHtmlOptions extends HtmlOptions {

  @Override
  public boolean isGenerateTable() {
    return false;
  }

  @Override
  public boolean isGenerateHtmlHeader() {
    return false;
  }

  @Override
  public String getTableId() {
    return null;
  }

  @Override
  public HtmlOptions setGenerateHtmlHeader(boolean b) {
    throw new IllegalStateException();
  }

  @Override
  public HtmlOptions setGenerateTable(boolean b) {
    throw new IllegalStateException();
  }

  @Override
  public HtmlOptions setTableId(String id) {
    throw new IllegalStateException();
  }
}
