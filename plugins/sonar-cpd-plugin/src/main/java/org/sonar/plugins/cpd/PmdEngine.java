/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.cpd;

import java.io.IOException;
import java.nio.charset.Charset;

import net.sourceforge.pmd.cpd.AbstractLanguage;
import net.sourceforge.pmd.cpd.TokenEntry;

import org.apache.commons.configuration.Configuration;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.duplications.cpd.CPD;

import com.google.common.annotations.VisibleForTesting;

public class PmdEngine extends CpdEngine {

  private final CpdMapping[] mappings;

  public PmdEngine() {
    this.mappings = null;
  }

  public PmdEngine(CpdMapping[] mappings) {
    this.mappings = mappings;
  }

  @Override
  public boolean isLanguageSupported(Language language) {
    return getMapping(language) != null;
  }

  private CpdMapping getMapping(Language language) {
    if (mappings != null) {
      for (CpdMapping cpdMapping : mappings) {
        if (cpdMapping.getLanguage().equals(language)) {
          return cpdMapping;
        }
      }
    }
    return null;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    CpdMapping mapping = getMapping(project.getLanguage());
    CPD cpd = executeCPD(project, mapping, project.getFileSystem().getSourceCharset());
    saveResults(cpd, mapping, project, context);
  }

  private void saveResults(CPD cpd, CpdMapping mapping, Project project, SensorContext context) {
    CpdAnalyser cpdAnalyser = new CpdAnalyser(project, context, mapping);
    cpdAnalyser.analyse(cpd.getMatches());
  }

  private CPD executeCPD(Project project, CpdMapping mapping, Charset encoding) {
    try {
      CPD cpd = configureCPD(project, mapping, encoding);
      cpd.go();
      return cpd;

    } catch (Exception e) {
      throw new CpdException(e);
    }
  }

  private CPD configureCPD(Project project, CpdMapping mapping, Charset encoding) throws IOException {
    // To avoid a cpd bug generating error as "java.lang.IndexOutOfBoundsException: Index: 259, Size: 248"
    // See http://sourceforge.net/tracker/?func=detail&atid=479921&aid=1947823&group_id=56262 for more details
    TokenEntry.clearImages();

    int minTokens = getMinimumTokens(project);
    AbstractLanguage cpdLanguage = new AbstractLanguage(mapping.getTokenizer()) {
    };

    CPD cpd = new CPD(minTokens, cpdLanguage);
    cpd.setEncoding(encoding.name());
    cpd.setLoadSourceCodeSlices(false);
    cpd.add(project.getFileSystem().getSourceFiles(project.getLanguage()));
    return cpd;
  }

  @VisibleForTesting
  int getMinimumTokens(Project project) {
    Configuration conf = project.getConfiguration();
    return conf.getInt("sonar.cpd." + project.getLanguageKey() + ".minimumTokens",
        conf.getInt("sonar.cpd.minimumTokens", CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE));
  }

}
