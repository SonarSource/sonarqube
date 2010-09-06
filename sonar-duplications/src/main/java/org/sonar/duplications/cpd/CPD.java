/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package org.sonar.duplications.cpd;

import net.sourceforge.pmd.cpd.*;
import net.sourceforge.pmd.util.FileFinder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class CPD {

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
    TokenEntry.clearImages(); // workaround for bug 1947823
    this.minimumTileSize = minimumTileSize;
    this.language = language;
  }

  public void skipDuplicates() {
    this.skipDuplicates = true;
  }

  public void setCpdListener(CPDListener cpdListener) {
    this.listener = cpdListener;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setLoadSourceCodeSlices(boolean loadSourceCodeSlices) {
    this.loadSourceCodeSlices = loadSourceCodeSlices;
  }

  public void go() {
    TokenEntry.clearImages();
    matchAlgorithm = new MatchAlgorithm(source, tokens, minimumTileSize, listener);
    matchAlgorithm.setLoadSourceCodeSlices(loadSourceCodeSlices);
    matchAlgorithm.findMatches();
  }

  public Iterator<Match> getMatches() {
    return matchAlgorithm.matches();
  }

  public void add(File file) throws IOException {
    add(1, file);
  }

  public void addAllInDirectory(String dir) throws IOException {
    addDirectory(dir, false);
  }

  public void addRecursively(String dir) throws IOException {
    addDirectory(dir, true);
  }

  public void add(List<File> files) throws IOException {
    for (File f : files) {
      add(files.size(), f);
    }
  }

  private void addDirectory(String dir, boolean recurse) throws IOException {
    if ( !(new File(dir)).exists()) {
      throw new FileNotFoundException("Couldn't find directory " + dir);
    }
    FileFinder finder = new FileFinder();
    // TODO - could use SourceFileSelector here
    add(finder.findFilesFrom(dir, language.getFileFilter(), recurse));
  }

  private Set<String> current = new HashSet<String>();

  private void add(int fileCount, File file) throws IOException {

    if (skipDuplicates) {
      // TODO refactor this thing into a separate class
      String signature = file.getName() + '_' + file.length();
      if (current.contains(signature)) {
        System.err.println("Skipping " + file.getAbsolutePath()
            + " since it appears to be a duplicate file and --skip-duplicate-files is set");
        return;
      }
      current.add(signature);
    }

    if ( !file.getCanonicalPath().equals(new File(file.getAbsolutePath()).getCanonicalPath())) {
      System.err.println("Skipping " + file + " since it appears to be a symlink");
      return;
    }

    listener.addedFile(fileCount, file);
    SourceCode sourceCode = new SourceCode(new FileCodeLoaderWithoutCache(file, encoding));
    language.getTokenizer().tokenize(sourceCode, tokens);
    source.put(sourceCode.getFileName(), sourceCode);
  }


}
