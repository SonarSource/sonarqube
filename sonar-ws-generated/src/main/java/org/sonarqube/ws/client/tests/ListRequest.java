/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
 * Get the list of tests either in a test file or that test a given line of source code.<br /> Requires 'Browse' permission on the file's project.<br /> One (and only one) of the following combination of parameters must be provided: <ul><li>testId - get a specific test</li><li>testFileId - get the tests in a test file</li><li>testFileKey - get the tests in a test file</li><li>sourceFileId and sourceFileLineNumber - get the tests that cover a specific line of code</li><li>sourceFileKey and sourceFileLineNumber - get the tests that cover a specific line of code</li></ul>
 *
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
   * Branch key
   *
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
   * 1-based page number
   *
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
   * Page size. Must be greater than 0 and less than 500
   *
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
   * ID of source file. Must be provided with the source file line number.
   *
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
   * Key of source file. Must be provided with the source file line number.
   *
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
   * Source file line number. Must be provided with the source file ID or key.
   *
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
   * ID of test file
   *
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
   * Key of test file
   *
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
   * ID of test
   *
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
