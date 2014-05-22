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

package org.sonar.wsclient.test;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class CoverageShowQuery {

  private String key;
  private String from;
  private String to;
  private String type;

  private CoverageShowQuery() {
    // Nothing here
  }

  public String key() {
    return key;
  }

  public CoverageShowQuery setKey(String key) {
    this.key = key;
    return this;
  }

  @CheckForNull
  public String from() {
    return from;
  }

  public CoverageShowQuery setFrom(@Nullable String from) {
    this.from = from;
    return this;
  }

  @CheckForNull
  public String to() {
    return to;
  }

  public CoverageShowQuery setTo(@Nullable String to) {
    this.to = to;
    return this;
  }

  @CheckForNull
  public String type() {
    return type;
  }

  public CoverageShowQuery setType(@Nullable String type) {
    this.type = type;
    return this;
  }

  public static CoverageShowQuery create(){
    return new CoverageShowQuery();
  }

}
