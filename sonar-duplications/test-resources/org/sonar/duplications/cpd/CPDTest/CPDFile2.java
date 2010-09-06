/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd.fork;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.pmd.cpd.CPDListener;
import net.sourceforge.pmd.cpd.CPDNullListener;
import net.sourceforge.pmd.cpd.Language;
import net.sourceforge.pmd.cpd.SourceCode;
import net.sourceforge.pmd.cpd.Tokens;
import org.sonar.duplications.cpd.MatchAlgorithm;

public class CPDFile2 {

  private Map<String, SourceCode> source = new HashMap<String, SourceCode>();
  private CPDListener listener = new CPDNullListener();
  private Tokens tokens = new Tokens();
  private int minimumTileSize;
  private MatchAlgorithm matchAlgorithm;
  private Language language;
  private boolean skipDuplicates;
  public static boolean debugEnable = false;
  private boolean loadSourceCodeSlices = true;
  private String encoding = System.getProperty("file.encoding");

  public CPD(int minimumTileSize, Language language) {
    this.minimumTileSize = minimumTileSize;
    this.language = language;
  }

  public void skipDuplicates() {
    this.skipDuplicates = true;
  }

  public void setCpdListener(CPDListener cpdListener) {
    this.listener = cpdListener;
  }
}
