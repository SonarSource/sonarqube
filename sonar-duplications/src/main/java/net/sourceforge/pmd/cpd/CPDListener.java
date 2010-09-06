/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

import java.io.File;

public interface CPDListener {

    public static final int INIT = 0;
    public static final int HASH = 1;
    public static final int MATCH = 2;
    public static final int GROUPING = 3;
    public static final int DONE = 4;

    void addedFile(int fileCount, File file);

    void phaseUpdate(int phase);
}
