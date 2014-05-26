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

package org.sonar.wsclient.test.internal;

import org.sonar.wsclient.test.TestableTestCases;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultTestableTestCases implements TestableTestCases {

  private final List<TestCase> tests = new ArrayList<TestCase>();
  private final Map<String, File> filesByRef = new HashMap<String, File>();

  @Override
  public List<TestCase> tests() {
    return tests;
  }

  @Override
  public List<File> files() {
    return new ArrayList<File>(filesByRef.values());
  }

  public File fileByRef(String ref) {
    return filesByRef.get(ref);
  }

  public DefaultTestableTestCases addTest(TestCase testCase) {
    tests.add(testCase);
    return this;
  }

  public DefaultTestableTestCases addFile(String ref, File file) {
    filesByRef.put(ref, file);
    return this;
  }

}
