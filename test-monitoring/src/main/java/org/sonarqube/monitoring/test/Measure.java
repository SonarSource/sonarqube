/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarqube.monitoring.test;

public class Measure {
  private String timestamp;
  private String branchName;
  private String commit;
  private String build;
  private String category;
  private String testClass;
  private String testMethod;
  private String exceptionClass;
  private String exceptionMessage;
  private String exceptionLogs;

  private Measure() {
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public String getCommit() {
    return commit;
  }

  public void setCommit(String commit) {
    this.commit = commit;
  }

  public String getBuild() {
    return build;
  }

  public void setBuild(String build) {
    this.build = build;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getTestClass() {
    return testClass;
  }

  public void setTestClass(String testClass) {
    this.testClass = testClass;
  }

  public String getTestMethod() {
    return testMethod;
  }

  public void setTestMethod(String testMethod) {
    this.testMethod = testMethod;
  }

  public String getExceptionClass() {
    return exceptionClass;
  }

  public void setExceptionClass(String exceptionClass) {
    this.exceptionClass = exceptionClass;
  }

  public String getExceptionMessage() {
    return exceptionMessage;
  }

  public void setExceptionMessage(String exceptionMessage) {
    this.exceptionMessage = exceptionMessage;
  }

  public String getExceptionLogs() {
    return exceptionLogs;
  }

  public void setExceptionLogs(String exceptionLogs) {
    this.exceptionLogs = exceptionLogs;
  }

  @Override
  public String toString() {
    return "Measure{" +
      "timestamp='" + timestamp + '\'' +
      ", branchName='" + branchName + '\'' +
      ", commit='" + commit + '\'' +
      ", build='" + build + '\'' +
      ", category='" + category + '\'' +
      ", testClass='" + testClass + '\'' +
      ", testMethod='" + testMethod + '\'' +
      ", exceptionClass='" + exceptionClass + '\'' +
      ", exceptionMessage='" + exceptionMessage + '\'' +
      ", exceptionLogs='" + exceptionLogs + '\'' +
      '}';
  }

  public static final class MeasureBuilder {
    private Measure measure;

    private MeasureBuilder() {
      measure = new Measure();
    }

    public static MeasureBuilder newMeasureBuilder() {
      return new MeasureBuilder();
    }

    public MeasureBuilder setTimestamp(String timestamp) {
      measure.setTimestamp(timestamp);
      return this;
    }

    public MeasureBuilder setBranchName(String branchName) {
      measure.setBranchName(branchName);
      return this;
    }

    public MeasureBuilder setCommit(String commit) {
      measure.setCommit(commit);
      return this;
    }

    public MeasureBuilder setBuild(String build) {
      measure.setBuild(build);
      return this;
    }

    public MeasureBuilder setCategory(String category) {
      measure.setCategory(category);
      return this;
    }

    public MeasureBuilder setTestClass(String testClass) {
      measure.setTestClass(testClass);
      return this;
    }

    public MeasureBuilder setTestMethod(String testMethod) {
      measure.setTestMethod(testMethod);
      return this;
    }

    public MeasureBuilder setExceptionClass(String exceptionClass) {
      measure.setExceptionClass(exceptionClass);
      return this;
    }

    public MeasureBuilder setExceptionMessage(String exceptionMessage) {
      measure.setExceptionMessage(exceptionMessage);
      return this;
    }

    public MeasureBuilder setExceptionLogs(String exceptionLogs) {
      measure.setExceptionLogs(exceptionLogs);
      return this;
    }

    public Measure build() {
      return measure;
    }
  }
}
