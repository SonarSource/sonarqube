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
package org.sonar.scanner.protocol.output;

import java.io.File;

import javax.annotation.concurrent.Immutable;

/**
 * Structure of files in the zipped report
 */
@Immutable
public class FileStructure {

  public enum Domain {
    ISSUES("issues-", Domain.PB),
    COMPONENT("component-", Domain.PB),
    MEASURES("measures-", Domain.PB),
    DUPLICATIONS("duplications-", Domain.PB),
    CPD_TEXT_BLOCKS("cpd-text-block-", Domain.PB),
    SYNTAX_HIGHLIGHTINGS("syntax-highlightings-", Domain.PB),
    CHANGESETS("changesets-", Domain.PB),
    SYMBOLS("symbols-", Domain.PB),
    COVERAGES("coverages-", Domain.PB),
    TESTS("tests-", Domain.PB),
    COVERAGE_DETAILS("coverage-details-", Domain.PB),
    SOURCE("source-", ".txt");

    private static final String PB = ".pb";
    private final String filePrefix;
    private final String fileSuffix;

    Domain(String filePrefix, String fileSuffix) {
      this.filePrefix = filePrefix;
      this.fileSuffix = fileSuffix;
    }
  }

  private final File dir;

  public FileStructure(File dir) {
    if (!dir.exists() || !dir.isDirectory()) {
      throw new IllegalArgumentException("Directory of analysis report does not exist: " + dir);
    }
    this.dir = dir;
  }

  public File metadataFile() {
    return new File(dir, "metadata.pb");
  }

  public File analysisLog() {
    return new File(dir, "analysis.log");
  }

  public File activeRules() {
    return new File(dir, "activerules.pb");
  }

  public File fileFor(Domain domain, int componentRef) {
    return new File(dir, domain.filePrefix + componentRef + domain.fileSuffix);
  }

  public File contextProperties() {
    return new File(dir, "context-props.pb");
  }
  
  public File root() {
    return dir;
  }
}
