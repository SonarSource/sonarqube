/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StringUtil {

	public static final String[] EMPTY_STRINGS = new String[0];
    private static final boolean supportsUTF8 = System.getProperty("net.sourceforge.pmd.supportUTF8", "no").equals("yes");
    private static final String[] ENTITIES;

    static {
        ENTITIES = new String[256 - 126];
        for (int i = 126; i <= 255; i++) {
            ENTITIES[i - 126] = "&#" + i + ';';
        }
    }

    public static String replaceString(String original, char oldChar, String newString) {
        
    	String fixedNew = newString == null ? "" : newString;

        StringBuffer desc = new StringBuffer();
        int index = original.indexOf(oldChar);
        int last = 0;
        while (index != -1) {
            desc.append(original.substring(last, index));
            desc.append(fixedNew);
            last = index + 1;
            index = original.indexOf(oldChar, last);
        }
        desc.append(original.substring(last));
        return desc.toString();
    }

    public static String replaceString(String original, String oldString, String newString) {
    	
    	String fixedNew = newString == null ? "" : newString;
    	
        StringBuffer desc = new StringBuffer();
        int index = original.indexOf(oldString);
        int last = 0;
        while (index != -1) {
            desc.append(original.substring(last, index));
            desc.append(fixedNew);
            last = index + oldString.length();
            index = original.indexOf(oldString, last);
        }
        desc.append(original.substring(last));
        return desc.toString();
    }

    /**
     * Appends to a StringBuffer the String src where non-ASCII and
     * XML special chars are escaped.
     *
     * @param buf The destination XML stream
     * @param src The String to append to the stream
     */
    public static void appendXmlEscaped(StringBuffer buf, String src) {
        appendXmlEscaped(buf, src, supportsUTF8);
    }

    public static String htmlEncode(String string) {
        String encoded = StringUtil.replaceString(string, '&', "&amp;");
        encoded = StringUtil.replaceString(encoded, '<', "&lt;");
        return StringUtil.replaceString(encoded, '>', "&gt;");
    }
    
    // TODO - unify the method above with the one below
    
    private static void appendXmlEscaped(StringBuffer buf, String src, boolean supportUTF8) {
        char c;
        for (int i = 0; i < src.length(); i++) {
            c = src.charAt(i);
            if (c > '~') {// 126
                if (!supportUTF8) {
                    if (c <= 255) {
                        buf.append(ENTITIES[c - 126]);
                    } else {
                        buf.append("&u").append(Integer.toHexString(c)).append(';');
                    }
                } else {
                    buf.append(c);
                }
            } else if (c == '&')
                buf.append("&amp;");
            else if (c == '"')
                buf.append("&quot;");
            else if (c == '<')
                buf.append("&lt;");
            else if (c == '>')
                buf.append("&gt;");
            else
                buf.append(c);
        }
    }

	/**
	 * Parses the input source using the delimiter specified. This method is much
	 * faster than using the StringTokenizer or String.split(char) approach and
	 * serves as a replacement for String.split() for JDK1.3 that doesn't have it.
     *
     * FIXME - we're on JDK 1.4 now, can we replace this with String.split?
	 *
	 * @param source String
	 * @param delimiter char
	 * @return String[]
	 */
	public static String[] substringsOf(String source, char delimiter) {

		if (source == null || source.length() == 0) {
            return EMPTY_STRINGS;
        }
		
		int delimiterCount = 0;
		int length = source.length();
		char[] chars = source.toCharArray();

		for (int i=0; i<length; i++) {
			if (chars[i] == delimiter) delimiterCount++;
			}

		if (delimiterCount == 0) return new String[] { source };

		String results[] = new String[delimiterCount+1];

		int i = 0;
		int offset = 0;

		while (offset <= length) {
			int pos = source.indexOf(delimiter, offset);
			if (pos < 0) pos = length;
			results[i++] = pos == offset ? "" : source.substring(offset, pos);
			offset = pos + 1;
			}

		return results;
	}
	
	/**
	 * Much more efficient than StringTokenizer.
	 * 
	 * @param str String
	 * @param separator char
	 * @return String[]
	 */
	  public static String[] substringsOf(String str, String separator) {
		  
	        if (str == null || str.length() == 0) {
	            return EMPTY_STRINGS;
	        }

	        int index = str.indexOf(separator);
	        if (index == -1) {
	            return new String[]{str};
	        }

	        List<String> list = new ArrayList<String>();
	        int currPos = 0;
	        int len = separator.length();
	        while (index != -1) {
	            list.add(str.substring(currPos, index));
	            currPos = index + len;
	            index = str.indexOf(separator, currPos);
	        }
	        list.add(str.substring(currPos));
	        return list.toArray(new String[list.size()]);
	    }
	
	
	/**
	 * Copies the elements returned by the iterator onto the string buffer
	 * each delimited by the separator.
	 *
	 * @param sb StringBuffer
	 * @param iter Iterator
	 * @param separator String
	 */
	public static void asStringOn(StringBuffer sb, Iterator iter, String separator) {
		
	    if (!iter.hasNext()) return;
	    
	    sb.append(iter.next());
	    
	    while (iter.hasNext()) {
	    	sb.append(separator);
	        sb.append(iter.next());
	    }
	}
	/**
	 * Return the length of the shortest string in the array.
	 * If any one of them is null then it returns 0.
	 * 
	 * @param strings String[]
	 * @return int
	 */
	public static int lengthOfShortestIn(String[] strings) {
		
		int minLength = Integer.MAX_VALUE;
		
		for (int i=0; i<strings.length; i++) {
			if (strings[i] == null) return 0;
			minLength = Math.min(minLength, strings[i].length());
		}
		
		return minLength;
	}
	
	/**
	 * Determine the maximum number of common leading whitespace characters
	 * the strings share in the same sequence. Useful for determining how
	 * many leading characters can be removed to shift all the text in the
	 * strings to the left without misaligning them.
	 * 
	 * @param strings String[]
	 * @return int
	 */
	public static int maxCommonLeadingWhitespaceForAll(String[] strings) {
		
		int shortest = lengthOfShortestIn(strings);
		if (shortest == 0) return 0;
		
		char[] matches = new char[shortest];
		
		String str;
		for (int m=0; m<matches.length; m++) {
			matches[m] = strings[0].charAt(m);
			if (!Character.isWhitespace(matches[m])) return m;
			for (int i=0; i<strings.length; i++) {
				str = strings[i];
				if (str.charAt(m) != matches[m])  return m; 
				}
		}
		
		return shortest;
	}
	
	/**
	 * Trims off the leading characters off the strings up to the trimDepth 
	 * specified. Returns the same strings if trimDepth = 0
	 * 
	 * @param strings
	 * @param trimDepth
	 * @return String[]
	 */
	public static String[] trimStartOn(String[] strings, int trimDepth) {
		
		if (trimDepth == 0) return strings;
		
		String[] results = new String[strings.length];
		for (int i=0; i<strings.length; i++) {
			results[i] = strings[i].substring(trimDepth);
		}
		return results;
   }
	
    /**
     * Left pads a string.
     * @param s The String to pad
     * @param length The desired minimum length of the resulting padded String
     * @return The resulting left padded String
     */
    public static String lpad(String s, int length) {
         String res = s;
         if (length - s.length() > 0) {
             char [] arr = new char[length - s.length()];
             java.util.Arrays.fill(arr, ' ');
             res = new StringBuffer(length).append(arr).append(s).toString();
         }
         return res;
    }
    
    /**
     * Are the two String values the same.
     * The Strings can be optionally trimmed before checking.
     * The Strings can be optionally compared ignoring case.
     * The Strings can be have embedded whitespace standardized before comparing.
     * Two null values are treated as equal.
     * 
     * @param s1 The first String.
     * @param s2 The second String.
     * @param trim Indicates if the Strings should be trimmed before comparison.
     * @param ignoreCase Indicates if the case of the Strings should ignored during comparison.
     * @param standardizeWhitespace Indicates if the embedded whitespace should be standardized before comparison.
     * @return <code>true</code> if the Strings are the same, <code>false</code> otherwise.
     */
    public static boolean isSame(String s1, String s2, boolean trim, boolean ignoreCase, boolean standardizeWhitespace) {
		if (s1 == s2) {
			return true;
		} else if (s1 == null || s2 == null) {
			return false;
		} else {
			if (trim) {
				s1 = s1.trim();
				s2 = s2.trim();
			}
			if (standardizeWhitespace) {
				// Replace all whitespace with a standard single space character.
				s1 = s1.replaceAll("\\s+", " ");
				s2 = s2.replaceAll("\\s+", " ");
			}
			return ignoreCase ? s1.equalsIgnoreCase(s2) : s1.equals(s2);
		}
    }
}
