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
package org.sonar.duplications.cpd;

import net.sourceforge.pmd.cpd.AbstractLanguage;
import net.sourceforge.pmd.cpd.JavaTokenizer;
import net.sourceforge.pmd.cpd.TokenEntry;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class CPDTest {

  @Test
  public void testSetLoadSourceCodeSlicesToFalse() throws IOException {
    TokenEntry.clearImages();
    AbstractLanguage cpdLanguage = new AbstractLanguage(new JavaTokenizer()) {
    };
    CPD cpd = new CPD(20, cpdLanguage);
    cpd.setEncoding(Charset.defaultCharset().name());
    cpd.setLoadSourceCodeSlices(false);
    cpd.add(new File("test-resources/org/sonar/duplications/cpd/CPDTest/CPDFile1.java"));
    cpd.add(new File("test-resources/org/sonar/duplications/cpd/CPDTest/CPDFile2.java"));
    cpd.go();

    List<Match> matches = getMatches(cpd);
    assertThat(matches.size(), is(1));

    org.sonar.duplications.cpd.Match match = matches.get(0);
    assertThat(match.getLineCount(), is(26));
    assertThat(match.getFirstMark().getBeginLine(), is(16));
    assertThat(match.getSourceCodeSlice(), is(nullValue()));
  }
  
  @Test
  public void testDuplicationOnSameFile() throws IOException {
    TokenEntry.clearImages();
    AbstractLanguage cpdLanguage = new AbstractLanguage(new JavaTokenizer()) {
    };
    CPD cpd = new CPD(20, cpdLanguage);
    cpd.setEncoding(Charset.defaultCharset().name());
    cpd.setLoadSourceCodeSlices(false);
    cpd.add(new File("test-resources/org/sonar/duplications/cpd/CPDTest/CPDFile3.java"));
    cpd.go();

    List<Match> matches = getMatches(cpd);
    assertThat(matches.size(), is(1));

    org.sonar.duplications.cpd.Match match = matches.get(0);
    assertThat(match.getLineCount(), is(16));
    assertThat(match.getFirstMark().getBeginLine(), is(29));
    assertThat(match.getSourceCodeSlice(), is(nullValue()));
  }

  private List<Match> getMatches(CPD cpd) {
    List<Match> matches = new ArrayList<org.sonar.duplications.cpd.Match>();
    Iterator<Match> matchesIter = cpd.getMatches();
    while (matchesIter.hasNext()) {
      matches.add(matchesIter.next());
    }
    return matches;
  }

}
