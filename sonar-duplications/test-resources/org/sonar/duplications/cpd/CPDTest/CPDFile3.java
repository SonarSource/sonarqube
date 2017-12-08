/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd.fork;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.pmd.cpd.CPDListener;
import net.sourceforge.pmd.cpd.CPDNullListener;
import net.sourceforge.pmd.cpd.Language;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.Tokens;

import org.apache.commons.io.FileUtils;
import org.sonar.duplications.cpd.CPD;
import org.sonar.duplications.cpd.Match;
import org.sonar.duplications.cpd.MatchAlgorithm;

public class CPDFile2 {


  public void method1(){
    CPD cpd = new CPD(20, cpdLanguage);
    cpd.setEncoding(Charset.defaultCharset().name());
    cpd.setLoadSourceCodeSlices(false);
    cpd.add(FileUtils.toFile(CPD.class.getResource("/org/sonar/duplications/cpd/CPDTest/CPDFile1.java")));
    cpd.add(FileUtils.toFile(CPD.class.getResource("/org/sonar/duplications/cpd/CPDTest/CPDFile2.java")));
    cpd.go();

    List<Match> matches = getMatches(cpd);
    assertThat(matches.size(), is(1));

    org.sonar.duplications.cpd.Match match = matches.get(0);
    assertThat(match.getLineCount(), is(26));
    assertThat(match.getFirstMark().getBeginLine(), is(16));
    assertThat(match.getSourceCodeSlice(), is(nullValue()));
  }
  
  public void method1Duplicated(){
    CPD cpd = new CPD(20, cpdLanguage);
    cpd.setEncoding(Charset.defaultCharset().name());
    cpd.setLoadSourceCodeSlices(false);
    cpd.add(FileUtils.toFile(CPD.class.getResource("/org/sonar/duplications/cpd/CPDTest/CPDFile1.java")));
    cpd.add(FileUtils.toFile(CPD.class.getResource("/org/sonar/duplications/cpd/CPDTest/CPDFile2.java")));
    cpd.go();

    List<Match> matches = getMatches(cpd);
    assertThat(matches.size(), is(1));

    org.sonar.duplications.cpd.Match match = matches.get(0);
    assertThat(match.getLineCount(), is(26));
    assertThat(match.getFirstMark().getBeginLine(), is(16));
    assertThat(match.getSourceCodeSlice(), is(nullValue()));
  }

}
