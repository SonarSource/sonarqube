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
package org.sonarqube.ws.client.tests;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/tests/list">Further information about this action online (including a response example)</a>
 * @since 5.2
 */
@Generated("sonar-ws-generator")
public class ListRequest {

  private String branch;
  private String p;
  private String ps;
  private String sourceFileId;
  private String sourceFileKey;
  private String sourceFileLineNumber;
  private String testFileId;
  private String testFileKey;
  private String testId;

  /**
   * This is part of the internal API.
   * Example value: "feature/my_branch"
   */
  public ListRequest setBranch(String branch) {
    this.branch = branch;
    return this;
  }

  public String getBranch() {
    return branch;
  }

  /**
   * Example value: "42"
   */
  public ListRequest setP(String p) {
    this.p = p;
    return this;
  }

  public String getP() {
    return p;
  }

  /**
   * Example value: "20"
   */
  public ListRequest setPs(String ps) {
    this.ps = ps;
    return this;
  }

  public String getPs() {
    return ps;
  }

  /**
   * Example value: "AU-TpxcA-iU5OvuD2FL0"
   */
  public ListRequest setSourceFileId(String sourceFileId) {
    this.sourceFileId = sourceFileId;
    return this;
  }

  public String getSourceFileId() {
    return sourceFileId;
  }

  /**
   * Example value: "my_project:/src/foo/Bar.php"
   */
  public ListRequest setSourceFileKey(String sourceFileKey) {
    this.sourceFileKey = sourceFileKey;
    return this;
  }

  public String getSourceFileKey() {
    return sourceFileKey;
  }

  /**
   * Example value: "10"
   */
  public ListRequest setSourceFileLineNumber(String sourceFileLineNumber) {
    this.sourceFileLineNumber = sourceFileLineNumber;
    return this;
  }

  public String getSourceFileLineNumber() {
    return sourceFileLineNumber;
  }

  /**
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public ListRequest setTestFileId(String testFileId) {
    this.testFileId = testFileId;
    return this;
  }

  public String getTestFileId() {
    return testFileId;
  }

  /**
   * Example value: "MY_PROJECT:src/test/java/foo/BarTest.java"
   */
  public ListRequest setTestFileKey(String testFileKey) {
    this.testFileKey = testFileKey;
    return this;
  }

  public String getTestFileKey() {
    return testFileKey;
  }

  /**
   * Example value: "AU-TpxcA-iU5OvuD2FLz"
   */
  public ListRequest setTestId(String testId) {
    this.testId = testId;
    return this;
  }

  public String getTestId() {
    return testId;
  }
}
