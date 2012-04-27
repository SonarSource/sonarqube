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
package org.sonar.plugins.pmd;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closeables;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDException;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSets;
import net.sourceforge.pmd.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class PmdTemplate {
  private static final Logger LOG = LoggerFactory.getLogger(PmdTemplate.class);

  private static final Map<String, String> JAVA_VERSIONS = ImmutableMap.of(
      "1.1", "1.3",
      "1.2", "1.3",
      "5", "1.5",
      "6", "1.6");

  private final PMD pmd;

  public PmdTemplate(String javaVersion) {
    pmd = new PMD();
    setJavaVersion(pmd, javaVersion);
  }

  public void process(File file, Charset encoding, RuleSets rulesets, RuleContext ruleContext) {
    ruleContext.setSourceCodeFilename(file.getAbsolutePath());

    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(file);

      pmd.processFile(inputStream, encoding.displayName(), rulesets, ruleContext);
    } catch (PMDException e) {
      LOG.error("Fail to execute PMD. Following file is ignored: " + file, e.getCause());
    } catch (Exception e) {
      LOG.error("Fail to execute PMD. Following file is ignored: " + file, e);
    } finally {
      Closeables.closeQuietly(inputStream);
    }
  }

  @VisibleForTesting
  static void setJavaVersion(PMD pmd, String javaVersion) {
    String version = normalize(javaVersion);
    if (version == null) {
      return; // Do nothing
    }

    SourceType sourceType = SourceType.getSourceTypeForId("java " + version);
    if (sourceType == null) {
      throw new SonarException("Unsupported Java version for PMD: " + version);
    }

    LOG.info("Java version: " + version);
    pmd.setJavaVersion(sourceType);
  }

  private static String normalize(String version) {
    return Functions.forMap(JAVA_VERSIONS, version).apply(version);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
