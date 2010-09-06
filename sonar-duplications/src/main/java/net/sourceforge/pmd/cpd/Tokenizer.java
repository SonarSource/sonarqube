/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

import java.io.IOException;

public interface Tokenizer {
    void tokenize(SourceCode tokens, Tokens tokenEntries) throws IOException;
}
