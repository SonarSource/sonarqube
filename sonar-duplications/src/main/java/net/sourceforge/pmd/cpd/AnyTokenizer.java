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

/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * This class does a best-guess try-anything tokenization.
 *
 * @author jheintz
 */
public class AnyTokenizer implements Tokenizer {
    public static final String TOKENS = " \t!#$%^&*(){}-=+<>/\\`~;:";

    public void tokenize(SourceCode sourceCode, Tokens tokenEntries) {
        StringBuffer sb = sourceCode.getCodeBuffer();
        BufferedReader reader = new BufferedReader(new CharArrayReader(sb.toString().toCharArray()));
        try {
            int lineNumber = 1;
            String line = reader.readLine();
            while (line != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, TOKENS, true);
                try {
                    String token = tokenizer.nextToken();
                    while (token != null) {
                        if (!token.equals(" ") && !token.equals("\t")) {
                            tokenEntries.add(new TokenEntry(token, sourceCode.getFileName(), lineNumber));
                        }
                        token = tokenizer.nextToken();
                    }
                } catch (NoSuchElementException ex) {
                    // done with tokens
                }
                // advance iteration variables
                line = reader.readLine();
                lineNumber++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception ex) {
            }
            tokenEntries.add(TokenEntry.getEOF());
        }
    }
}
