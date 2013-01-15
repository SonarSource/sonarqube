/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

package org.sonar.api.tests;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class ProjectTestsImpl implements ProjectTests, BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectTestsImpl.class);
  private List<FileTest> fileTests;

  public ProjectTestsImpl() {
    fileTests = newArrayList();
  }

  public void addTest(String fileTestKey, Test test) {
    FileTest fileTest = getFileTest(fileTestKey);
    fileTest.addTest(test);

    LOG.info("Added a new test : " + toString());
  }

  public List<FileTest> getFileTests() {
    return fileTests;
  }

  public void cover(String fileTestKey, String test, String mainFile, Collection<Integer> lines){
    FileTest fileTest = find(fileTestKey);
    LOG.info("Covering - File test :" + toString() + ", test:" + test + ", file:" + mainFile+ ", lines:"+ Iterables.toString(lines));
  }

  private FileTest getFileTest(final String key) {
    FileTest fileTest = find(key);
    if (fileTest == null) {
      fileTest = new FileTest(key);
      fileTests.add(fileTest);
    }
    return fileTest;
  }

  private FileTest find(final String key){
    return Iterables.find(fileTests, new Predicate<FileTest>() {
      public boolean apply(FileTest fileTest) {
        return fileTest.getKey().equals(key);
      }
    }, null);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("fileTests", fileTests)
        .toString();
  }
}
