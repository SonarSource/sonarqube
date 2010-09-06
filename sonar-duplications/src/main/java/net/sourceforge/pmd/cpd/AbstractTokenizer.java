/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 * @author Zev Blut zb@ubit.com
 * @author Romain PELISSE belaran@gmail.com
 */
package net.sourceforge.pmd.cpd;

import java.util.List;

public abstract class AbstractTokenizer implements Tokenizer
{

	protected List<String> stringToken;			// List<String>, should be setted by children classes
	protected List<String> ignorableCharacter; 	// List<String>, should be setted by children classes
												// FIXME:Maybe an array of 'char' would be better for perfomance ?
	protected List<String> ignorableStmt; 		// List<String>, should be setted by children classes
	protected char ONE_LINE_COMMENT_CHAR = '#'; // Most script language ( shell, ruby, python,...) use this symbol for comment line

	private List<String> code;
	private int lineNumber = 0;
	private String currentLine;

	protected boolean spanMultipleLinesString = true;	// Most language does, so default is true

	private boolean downcaseString = true;

    public void tokenize(SourceCode tokens, Tokens tokenEntries) {
        this.code = tokens.getCode();

        for ( this.lineNumber = 0; lineNumber < this.code.size(); lineNumber++ ) {
        	this.currentLine = this.code.get(this.lineNumber);
            int loc = 0;
            while ( loc < currentLine.length() ) {
                StringBuffer token = new StringBuffer();
                loc = getTokenFromLine(token,loc);
                if (token.length() > 0 && !isIgnorableString(token.toString())) {
                    if (downcaseString) {
                        token = new StringBuffer(token.toString().toLowerCase());
                    }
                }
            }
        }
        tokenEntries.add(TokenEntry.getEOF());
    }

    private int getTokenFromLine(StringBuffer token, int loc) {
        for (int j = loc; j < this.currentLine.length(); j++) {
            char tok = this.currentLine.charAt(j);
            if (!Character.isWhitespace(tok) && !ignoreCharacter(tok)) {
                if (isComment(tok)) {
                    if (token.length() > 0) {
                        return j;
                    } else {
                        return getCommentToken(token, loc);
                    }
                } else if (isString(tok)) {
                    if (token.length() > 0) {
                        return j; // we need to now parse the string as a seperate token.
                    } else {
                        // we are at the start of a string
                        return parseString(token, j, tok);
                    }
                } else {
                    token.append(tok);
                }
            } else {
                if (token.length() > 0) {
                    return j;
                }
            }
            loc = j;
        }
        return loc + 1;
    }

    private int parseString(StringBuffer token, int loc, char stringDelimiter) {
        boolean escaped = false;
        boolean done = false;
        char tok = ' '; // this will be replaced.
        while ((loc < currentLine.length()) && ! done) {
            tok = currentLine.charAt(loc);
            if (escaped && tok == stringDelimiter) // Found an escaped string
                escaped = false;
            else if (tok == stringDelimiter && (token.length() > 0)) // We are done, we found the end of the string...
                done = true;
            else if (tok == '\\') // Found an escaped char
                escaped = true;
            else	// Adding char...
                escaped = false;
            //Adding char to String:" + token.toString());
            token.append(tok);
            loc++;
        }
        // Handling multiple lines string
        if ( 	! done &&	// ... we didn't find the end of the string
        		loc >= currentLine.length() && // ... we have reach the end of the line ( the String is incomplete, for the moment at least)
        		this.spanMultipleLinesString && // ... the language allow multiple line span Strings
        		++this.lineNumber < this.code.size() // ... there is still more lines to parse
        	) {
        	// parsing new line
        	this.currentLine = this.code.get(this.lineNumber);
        	// Warning : recursive call !
        	loc = this.parseString(token, loc, stringDelimiter);
        }
        return loc + 1;
    }

    private boolean ignoreCharacter(char tok)
    {
    	return this.ignorableCharacter.contains("" + tok);
    }

    private boolean isString(char tok)
    {
    	return this.stringToken.contains("" + tok);
    }

    private boolean isComment(char tok)
    {
        return tok == ONE_LINE_COMMENT_CHAR;
    }

    private int getCommentToken(StringBuffer token, int loc)
    {
        while (loc < this.currentLine.length())
        {
            token.append(this.currentLine.charAt(loc++));
        }
        return loc;
    }

    private boolean isIgnorableString(String token)
    {
    	return this.ignorableStmt.contains(token);
    }
}
