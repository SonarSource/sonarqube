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
package org.sonar.plugins.surefire;

import java.util.ArrayList;
import java.util.List;

public class TestSuiteReport {

  private String classKey;
  private int errors = 0;
  private int skipped = 0;
  private int tests = 0;
  private int timeMS = 0;
  private int failures = 0;

  private List<TestCaseDetails> details;
  

  public TestSuiteReport(String classKey) {
    super();
    this.classKey = classKey;
    this.details = new ArrayList<TestCaseDetails>();
  }

  public String getClassKey() {
    return classKey;
  }
  
  public int getErrors() {
    return errors;
  }

  public void setErrors(int errors) {
    this.errors = errors;
  }

  public int getSkipped() {
    return skipped;
  }

  public void setSkipped(int skipped) {
    this.skipped = skipped;
  }

  public int getTests() {
    return tests;
  }

  public void setTests(int tests) {
    this.tests = tests;
  }

  public int getTimeMS() {
    return timeMS;
  }

  public void setTimeMS(int timeMS) {
    this.timeMS = timeMS;
  }

  public int getFailures() {
    return failures;
  }

  public void setFailures(int failures) {
    this.failures = failures;
  }

  public List<TestCaseDetails> getDetails() {
    return details;
  }

  public void setDetails(List<TestCaseDetails> details) {
    this.details = details;
  }

  public boolean isValid() {
    return classKey!=null && !isInnerClass();
  }

  private boolean isInnerClass() {
    return classKey!=null && classKey.contains("$");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TestSuiteReport that = (TestSuiteReport) o;
    return classKey.equals(that.classKey);
  }

  @Override
  public int hashCode() {
    return classKey.hashCode();
  }
}
