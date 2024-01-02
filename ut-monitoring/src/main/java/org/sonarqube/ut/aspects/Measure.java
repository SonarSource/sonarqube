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
package org.sonarqube.ut.aspects;

public class Measure {
  private final String category = "Validate-UT-Backend";
  private final String measureClass = "";
  private final String measureMethod ="";
  private final String operation = "total";
  private final String suite = "Standalone";

  private String testClass;
  private String testMethod;
  private Long duration;
  private String build;
  private String commit;
  private String timestamp;
  private String kind;


  public Measure() {
    // http://stackoverflow.com/a/18645370/229031
  }

  public String getCategory() {
    return category;
  }

  public String getSuite() {
    return suite;
  }

  public String getTestClass() {
    return testClass;
  }

  public Measure setTestClass(String testClass) {
    this.testClass = testClass;
    return this;
  }

  public String getTestMethod() {
    return testMethod;
  }

  public Measure setTestMethod(String testMethod) {
    this.testMethod = testMethod;
    return this;
  }

  public String getMeasureClass() {
    return measureClass;
  }

  public String getMeasureMethod() {
    return measureMethod;
  }

  public Long getDuration() {
    return duration;
  }

  public Measure setDuration(Long duration) {
    this.duration = duration;
    return this;
  }

  public String getOperation() {
    return operation;
  }

  public String getKind() {
    return kind;
  }

  public Measure setKind(MeasureKind measureKind) {
    this.kind = measureKind.getName();
    return this;
  }

  public String getBuild() {
    return build;
  }

  public Measure setBuild(String build) {
    this.build = build;
    return this;
  }

  public String getCommit() {
    return commit;
  }

  public Measure setCommit(String commit) {
    this.commit = commit;
    return this;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public Measure setTimestamp(String timestamp) {
    this.timestamp = timestamp;
    return this;
  }
}
