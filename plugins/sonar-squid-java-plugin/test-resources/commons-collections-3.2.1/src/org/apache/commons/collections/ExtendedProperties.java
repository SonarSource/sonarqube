/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.collections;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class extends normal Java properties by adding the possibility
 * to use the same key many times concatenating the value strings
 * instead of overwriting them.
 * <p>
 * <b>Please consider using the <code>PropertiesConfiguration</code> class in
 * Commons-Configuration as soon as it is released.</b>
 * <p>
 * The Extended Properties syntax is explained here:
 *
 * <ul>
 *  <li>
 *   Each property has the syntax <code>key = value</code>
 *  </li>
 *  <li>
 *   The <i>key</i> may use any character but the equal sign '='.
 *  </li>
 *  <li>
 *   <i>value</i> may be separated on different lines if a backslash
 *   is placed at the end of the line that continues below.
 *  </li>
 *  <li>
 *   If <i>value</i> is a list of strings, each token is separated
 *   by a comma ','.
 *  </li>
 *  <li>
 *   Commas in each token are escaped placing a backslash right before
 *   the comma.
 *  </li>
 *  <li>
 *   Backslashes are escaped by using two consecutive backslashes i.e. \\
 *  </li>
 *  <li>
 *   If a <i>key</i> is used more than once, the values are appended
 *   as if they were on the same line separated with commas.
 *  </li>
 *  <li>
 *   Blank lines and lines starting with character '#' are skipped.
 *  </li>
 *  <li>
 *   If a property is named "include" (or whatever is defined by
 *   setInclude() and getInclude() and the value of that property is
 *   the full path to a file on disk, that file will be included into
 *   the ConfigurationsRepository. You can also pull in files relative
 *   to the parent configuration file. So if you have something
 *   like the following:
 *
 *   include = additional.properties
 *
 *   Then "additional.properties" is expected to be in the same
 *   directory as the parent configuration file.
 * 
 *   Duplicate name values will be replaced, so be careful.
 *
 *  </li>
 * </ul>
 *
 * <p>Here is an example of a valid extended properties file:
 *
 * <p><pre>
 *      # lines starting with # are comments
 *
 *      # This is the simplest property
 *      key = value
 *
 *      # A long property may be separated on multiple lines
 *      longvalue = aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa \
 *                  aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
 *
 *      # This is a property with many tokens
 *      tokens_on_a_line = first token, second token
 *
 *      # This sequence generates exactly the same result
 *      tokens_on_multiple_lines = first token
 *      tokens_on_multiple_lines = second token
 *
 *      # commas may be escaped in tokens
 *      commas.escaped = Hi\, what'up?
 * </pre>
 *
 * <p><b>NOTE</b>: this class has <b>not</b> been written for
 * performance nor low memory usage.  In fact, it's way slower than it
 * could be and generates too much memory garbage.  But since
 * performance is not an issue during intialization (and there is not
 * much time to improve it), I wrote it this way.  If you don't like
 * it, go ahead and tune it up!
 *
 * @since Commons Collections 1.0
 * @version $Revision: 646777 $ $Date: 2008-04-10 13:33:15 +0100 (Thu, 10 Apr 2008) $
 * 
 * @author <a href="mailto:stefano@apache.org">Stefano Mazzocchi</a>
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @author <a href="mailto:daveb@miceda-data">Dave Bryson</a>
 * @author <a href="mailto:jvanzyl@periapt.com">Jason van Zyl</a>
 * @author <a href="mailto:geirm@optonline.net">Geir Magnusson Jr.</a>
 * @author <a href="mailto:leon@opticode.co.za">Leon Messerschmidt</a>
 * @author <a href="mailto:kjohnson@transparent.com">Kent Johnson</a>
 * @author <a href="mailto:dlr@finemaltcoding.com">Daniel Rall</a>
 * @author <a href="mailto:ipriha@surfeu.fi">Ilkka Priha</a>
 * @author Janek Bogucki
 * @author Mohan Kishore
 * @author Stephen Colebourne
 * @author Shinobu Kawai
 * @author <a href="mailto:hps@intermeta.de">Henning P. Schmiedehausen</a>
 */
public class ExtendedProperties extends Hashtable {
    
    /**
     * Default configurations repository.
     */
    private ExtendedProperties defaults;

    /**
     * The file connected to this repository (holding comments and
     * such).
     *
     * @serial
     */
    protected String file;

    /**
     * Base path of the configuration file used to create
     * this ExtendedProperties object.
     */
    protected String basePath;

    /**
     * File separator.
     */
    protected String fileSeparator = System.getProperty("file.separator");

    /**
     * Has this configuration been intialized.
     */
    protected boolean isInitialized = false;

    /**
     * This is the name of the property that can point to other
     * properties file for including other properties files.
     */
    protected static String include = "include";

    /**
     * These are the keys in the order they listed
     * in the configuration file. This is useful when
     * you wish to perform operations with configuration
     * information in a particular order.
     */
    protected ArrayList keysAsListed = new ArrayList();

    protected final static String START_TOKEN="${";
    protected final static String END_TOKEN="}";


    /**
     * Interpolate key names to handle ${key} stuff
     *
     * @param base string to interpolate
     * @return returns the key name with the ${key} substituted
     */
    protected String interpolate(String base) {
        // COPIED from [configuration] 2003-12-29
        return (interpolateHelper(base, null));
    }

    /**
     * Recursive handler for multiple levels of interpolation.
     *
     * When called the first time, priorVariables should be null.
     *
     * @param base string with the ${key} variables
     * @param priorVariables serves two purposes: to allow checking for
     * loops, and creating a meaningful exception message should a loop
     * occur.  It's 0'th element will be set to the value of base from
     * the first call.  All subsequent interpolated variables are added
     * afterward.
     *
     * @return the string with the interpolation taken care of
     */
    protected String interpolateHelper(String base, List priorVariables) {
        // COPIED from [configuration] 2003-12-29
        if (base == null) {
            return null;
        }

        // on the first call initialize priorVariables
        // and add base as the first element
        if (priorVariables == null) {
            priorVariables = new ArrayList();
            priorVariables.add(base);
        }

        int begin = -1;
        int end = -1;
        int prec = 0 - END_TOKEN.length();
        String variable = null;
        StringBuffer result = new StringBuffer();

        // FIXME: we should probably allow the escaping of the start token
        while (((begin = base.indexOf(START_TOKEN, prec + END_TOKEN.length())) > -1)
            && ((end = base.indexOf(END_TOKEN, begin)) > -1)) {
            result.append(base.substring(prec + END_TOKEN.length(), begin));
            variable = base.substring(begin + START_TOKEN.length(), end);

            // if we've got a loop, create a useful exception message and throw
            if (priorVariables.contains(variable)) {
                String initialBase = priorVariables.remove(0).toString();
                priorVariables.add(variable);
                StringBuffer priorVariableSb = new StringBuffer();

                // create a nice trace of interpolated variables like so:
                // var1->var2->var3
                for (Iterator it = priorVariables.iterator(); it.hasNext();) {
                    priorVariableSb.append(it.next());
                    if (it.hasNext()) {
                        priorVariableSb.append("->");
                    }
                }

                throw new IllegalStateException(
                    "infinite loop in property interpolation of " + initialBase + ": " + priorVariableSb.toString());
            }
            // otherwise, add this variable to the interpolation list.
            else {
                priorVariables.add(variable);
            }

            //QUESTION: getProperty or getPropertyDirect
            Object value = getProperty(variable);
            if (value != null) {
                result.append(interpolateHelper(value.toString(), priorVariables));

                // pop the interpolated variable off the stack
                // this maintains priorVariables correctness for
                // properties with multiple interpolations, e.g.
                // prop.name=${some.other.prop1}/blahblah/${some.other.prop2}
                priorVariables.remove(priorVariables.size() - 1);
            } else if (defaults != null && defaults.getString(variable, null) != null) {
                result.append(defaults.getString(variable));
            } else {
                //variable not defined - so put it back in the value
                result.append(START_TOKEN).append(variable).append(END_TOKEN);
            }
            prec = end;
        }
        result.append(base.substring(prec + END_TOKEN.length(), base.length()));

        return result.toString();
    }
    
    /**
     * Inserts a backslash before every comma and backslash. 
     */
    private static String escape(String s) {
        StringBuffer buf = new StringBuffer(s);
        for (int i = 0; i < buf.length(); i++) {
            char c = buf.charAt(i);
            if (c == ',' || c == '\\') {
                buf.insert(i, '\\');
                i++;
            }
        }
        return buf.toString();
    }
    
    /**
     * Removes a backslash from every pair of backslashes. 
     */
    private static String unescape(String s) {
        StringBuffer buf = new StringBuffer(s);
        for (int i = 0; i < buf.length() - 1; i++) {
            char c1 = buf.charAt(i);
            char c2 = buf.charAt(i + 1);
            if (c1 == '\\' && c2 == '\\') {
                buf.deleteCharAt(i);
            }
        }
        return buf.toString();
    }

    /**
     * Counts the number of successive times 'ch' appears in the
     * 'line' before the position indicated by the 'index'.
     */
    private static int countPreceding(String line, int index, char ch) {
        int i;
        for (i = index - 1; i >= 0; i--) {
            if (line.charAt(i) != ch) {
                break;
            }
        }
        return index - 1 - i;
    }

    /**
     * Checks if the line ends with odd number of backslashes 
     */
    private static boolean endsWithSlash(String line) {
        if (!line.endsWith("\\")) {
            return false;
        }
        return (countPreceding(line, line.length() - 1, '\\') % 2 == 0);
    }

    /**
     * This class is used to read properties lines.  These lines do
     * not terminate with new-line chars but rather when there is no
     * backslash sign a the end of the line.  This is used to
     * concatenate multiple lines for readability.
     */
    static class PropertiesReader extends LineNumberReader {
        /**
         * Constructor.
         *
         * @param reader A Reader.
         */
        public PropertiesReader(Reader reader) {
            super(reader);
        }

        /**
         * Read a property.
         *
         * @return a String property
         * @throws IOException if there is difficulty reading the source.
         */
        public String readProperty() throws IOException {
            StringBuffer buffer = new StringBuffer();
            String line = readLine();
            while (line != null) {
                line = line.trim();
                if ((line.length() != 0) && (line.charAt(0) != '#')) {
                    if (endsWithSlash(line)) {
                        line = line.substring(0, line.length() - 1);
                        buffer.append(line);
                    } else {
                        buffer.append(line);
                        return buffer.toString();  // normal method end
                    }
                }
                line = readLine();
            }
            return null;  // EOF reached
        }
    }

    /**
     * This class divides into tokens a property value.  Token
     * separator is "," but commas into the property value are escaped
     * using the backslash in front.
     */
    static class PropertiesTokenizer extends StringTokenizer {
        /**
         * The property delimiter used while parsing (a comma).
         */
        static final String DELIMITER = ",";

        /**
         * Constructor.
         *
         * @param string A String.
         */
        public PropertiesTokenizer(String string) {
            super(string, DELIMITER);
        }

        /**
         * Check whether the object has more tokens.
         *
         * @return True if the object has more tokens.
         */
        public boolean hasMoreTokens() {
            return super.hasMoreTokens();
        }

        /**
         * Get next token.
         *
         * @return A String.
         */
        public String nextToken() {
            StringBuffer buffer = new StringBuffer();

            while (hasMoreTokens()) {
                String token = super.nextToken();
                if (endsWithSlash(token)) {
                    buffer.append(token.substring(0, token.length() - 1));
                    buffer.append(DELIMITER);
                } else {
                    buffer.append(token);
                    break;
                }
            }

            return buffer.toString().trim();
        }
    }

    /**
     * Creates an empty extended properties object.
     */
    public ExtendedProperties() {
        super();
    }

    /**
     * Creates and loads the extended properties from the specified file.
     *
     * @param file  the filename to load
     * @throws IOException if a file error occurs
     */
    public ExtendedProperties(String file) throws IOException {
        this(file, null);
    }

    /**
     * Creates and loads the extended properties from the specified file.
     *
     * @param file  the filename to load
     * @param defaultFile  a second filename to load default values from
     * @throws IOException if a file error occurs
     */
    public ExtendedProperties(String file, String defaultFile) throws IOException {
        this.file = file;

        basePath = new File(file).getAbsolutePath();
        basePath = basePath.substring(0, basePath.lastIndexOf(fileSeparator) + 1);

        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            this.load(in);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {}
        }

        if (defaultFile != null) {
            defaults = new ExtendedProperties(defaultFile);
        }
    }

    /**
     * Indicate to client code whether property
     * resources have been initialized or not.
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Gets the property value for including other properties files.
     * By default it is "include".
     *
     * @return A String.
     */
    public String getInclude() {
        return include;
    }

    /**
     * Sets the property value for including other properties files.
     * By default it is "include".
     *
     * @param inc A String.
     */
    public void setInclude(String inc) {
        include = inc;
    }

    /**
     * Load the properties from the given input stream.
     *
     * @param input  the InputStream to load from
     * @throws IOException if an IO error occurs
     */
    public void load(InputStream input) throws IOException {
        load(input, null);
    }

    /**
     * Load the properties from the given input stream
     * and using the specified encoding.
     *
     * @param input  the InputStream to load from
     * @param enc  the encoding to use
     * @throws IOException if an IO error occurs
     */
    public synchronized void load(InputStream input, String enc) throws IOException {
        PropertiesReader reader = null;
        if (enc != null) {
            try {
                reader = new PropertiesReader(new InputStreamReader(input, enc));
                
            } catch (UnsupportedEncodingException ex) {
                // Another try coming up....
            }
        }
        
        if (reader == null) {
            try {
                reader = new PropertiesReader(new InputStreamReader(input, "8859_1"));
                
            } catch (UnsupportedEncodingException ex) {
                // ISO8859-1 support is required on java platforms but....
                // If it's not supported, use the system default encoding
                reader = new PropertiesReader(new InputStreamReader(input));
            }
        }

        try {
            while (true) {
                String line = reader.readProperty();
                if (line == null) {
                    return;  // EOF
                }
                int equalSign = line.indexOf('=');

                if (equalSign > 0) {
                    String key = line.substring(0, equalSign).trim();
                    String value = line.substring(equalSign + 1).trim();

                    // Configure produces lines like this ... just ignore them
                    if ("".equals(value)) {
                        continue;
                    }

                    if (getInclude() != null && key.equalsIgnoreCase(getInclude())) {
                        // Recursively load properties files.
                        File file = null;

                        if (value.startsWith(fileSeparator)) {
                            // We have an absolute path so we'll use this
                            file = new File(value);
                            
                        } else {
                            // We have a relative path, and we have two 
                            // possible forms here. If we have the "./" form
                            // then just strip that off first before continuing.
                            if (value.startsWith("." + fileSeparator)) {
                                value = value.substring(2);
                            }

                            file = new File(basePath + value);
                        }

                        if (file != null && file.exists() && file.canRead()) {
                            load(new FileInputStream(file));
                        }
                    } else {
                        addProperty(key, value);
                    }
                }
            }
        } finally {
            // Loading is initializing
            isInitialized = true;
        }
    }

    /**
     * Gets a property from the configuration.
     *
     * @param key property to retrieve
     * @return value as object. Will return user value if exists,
     *        if not then default value if exists, otherwise null
     */
    public Object getProperty(String key) {
        // first, try to get from the 'user value' store
        Object obj = this.get(key);

        if (obj == null) {
            // if there isn't a value there, get it from the
            // defaults if we have them
            if (defaults != null) {
                obj = defaults.get(key);
            }
        }

        return obj;
    }
    
    /**
     * Add a property to the configuration. If it already
     * exists then the value stated here will be added
     * to the configuration entry. For example, if
     *
     * <code>resource.loader = file</code>
     *
     * is already present in the configuration and you
     *
     * <code>addProperty("resource.loader", "classpath")</code>
     *
     * Then you will end up with a Vector like the
     * following:
     *
     * <code>["file", "classpath"]</code>
     *
     * @param key  the key to add
     * @param value  the value to add
     */
    public void addProperty(String key, Object value) {
        if (value instanceof String) {
            String str = (String) value;
            if (str.indexOf(PropertiesTokenizer.DELIMITER) > 0) {
                // token contains commas, so must be split apart then added
                PropertiesTokenizer tokenizer = new PropertiesTokenizer(str);
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    addPropertyInternal(key, unescape(token));
                }
            } else {
                // token contains no commas, so can be simply added
                addPropertyInternal(key, unescape(str));
            }
        } else {
            addPropertyInternal(key, value);
        }

        // Adding a property connotes initialization
        isInitialized = true;
    }

    /**
     * Adds a key/value pair to the map.  This routine does
     * no magic morphing.  It ensures the keylist is maintained
     *
     * @param key  the key to store at
     * @param value  the decoded object to store
     */
    private void addPropertyDirect(String key, Object value) {
        // safety check
        if (!containsKey(key)) {
            keysAsListed.add(key);
        }
        put(key, value);
    }

    /**
     * Adds a decoded property to the map w/o checking for commas - used
     * internally when a property has been broken up into
     * strings that could contain escaped commas to prevent
     * the inadvertent vectorization.
     * <p>
     * Thanks to Leon Messerschmidt for this one.
     *
     * @param key  the key to store at
     * @param value  the decoded object to store
     */
    private void addPropertyInternal(String key, Object value) {
        Object current = this.get(key);

        if (current instanceof String) {
            // one object already in map - convert it to a vector
            List values = new Vector(2);
            values.add(current);
            values.add(value);
            put(key, values);
            
        } else if (current instanceof List) {
            // already a list - just add the new token
            ((List) current).add(value);
            
        } else {
            // brand new key - store in keysAsListed to retain order
            if (!containsKey(key)) {
                keysAsListed.add(key);
            }
            put(key, value);
        }
    }

    /**
     * Set a property, this will replace any previously
     * set values. Set values is implicitly a call
     * to clearProperty(key), addProperty(key,value).
     *
     * @param key  the key to set
     * @param value  the value to set
     */
    public void setProperty(String key, Object value) {
        clearProperty(key);
        addProperty(key, value);
    }
    
    /**
     * Save the properties to the given output stream.
     * <p>
     * The stream is not closed, but it is flushed.
     *
     * @param output  an OutputStream, may be null
     * @param header  a textual comment to act as a file header
     * @throws IOException if an IO error occurs
     */
    public synchronized void save(OutputStream output, String header) throws IOException {
        if (output == null) {
            return;
        }
        PrintWriter theWrtr = new PrintWriter(output);
        if (header != null) {
            theWrtr.println(header);
        }
        
        Enumeration theKeys = keys();
        while (theKeys.hasMoreElements()) {
            String key = (String) theKeys.nextElement();
            Object value = get(key);
            if (value != null) {
                if (value instanceof String) {
                    StringBuffer currentOutput = new StringBuffer();
                    currentOutput.append(key);
                    currentOutput.append("=");
                    currentOutput.append(escape((String) value));
                    theWrtr.println(currentOutput.toString());
                    
                } else if (value instanceof List) {
                    List values = (List) value;
                    for (Iterator it = values.iterator(); it.hasNext(); ) {
                        String currentElement = (String) it.next();
                        StringBuffer currentOutput = new StringBuffer();
                        currentOutput.append(key);
                        currentOutput.append("=");
                        currentOutput.append(escape(currentElement));
                        theWrtr.println(currentOutput.toString());
                    }
                }
            }
            theWrtr.println();
            theWrtr.flush();
        }
    }

    /**
     * Combines an existing Hashtable with this Hashtable.
     * <p>
     * Warning: It will overwrite previous entries without warning.
     *
     * @param props  the properties to combine
     */
    public void combine(ExtendedProperties props) {
        for (Iterator it = props.getKeys(); it.hasNext();) {
            String key = (String) it.next();
            setProperty(key, props.get(key));
        }
    }
    
    /**
     * Clear a property in the configuration.
     *
     * @param key  the property key to remove along with corresponding value
     */
    public void clearProperty(String key) {
        if (containsKey(key)) {
            // we also need to rebuild the keysAsListed or else
            // things get *very* confusing
            for (int i = 0; i < keysAsListed.size(); i++) {
                if (( keysAsListed.get(i)).equals(key)) {
                    keysAsListed.remove(i);
                    break;
                }
            }
            remove(key);
        }
    }

    /**
     * Get the list of the keys contained in the configuration
     * repository.
     *
     * @return an Iterator over the keys
     */
    public Iterator getKeys() {
        return keysAsListed.iterator();
    }

    /**
     * Get the list of the keys contained in the configuration
     * repository that match the specified prefix.
     *
     * @param prefix  the prefix to match
     * @return an Iterator of keys that match the prefix
     */
    public Iterator getKeys(String prefix) {
        Iterator keys = getKeys();
        ArrayList matchingKeys = new ArrayList();

        while (keys.hasNext()) {
            Object key = keys.next();

            if (key instanceof String && ((String) key).startsWith(prefix)) {
                matchingKeys.add(key);
            }
        }
        return matchingKeys.iterator();
    }

    /**
     * Create an ExtendedProperties object that is a subset
     * of this one. Take into account duplicate keys
     * by using the setProperty() in ExtendedProperties.
     *
     * @param prefix  the prefix to get a subset for
     * @return a new independent ExtendedProperties
     */
    public ExtendedProperties subset(String prefix) {
        ExtendedProperties c = new ExtendedProperties();
        Iterator keys = getKeys();
        boolean validSubset = false;

        while (keys.hasNext()) {
            Object key = keys.next();

            if (key instanceof String && ((String) key).startsWith(prefix)) {
                if (!validSubset) {
                    validSubset = true;
                }

                /*
                 * Check to make sure that c.subset(prefix) doesn't
                 * blow up when there is only a single property
                 * with the key prefix. This is not a useful
                 * subset but it is a valid subset.
                 */
                String newKey = null;
                if (((String) key).length() == prefix.length()) {
                    newKey = prefix;
                } else {
                    newKey = ((String) key).substring(prefix.length() + 1);
                }

                /*
                 *  use addPropertyDirect() - this will plug the data as 
                 *  is into the Map, but will also do the right thing
                 *  re key accounting
                 */
                c.addPropertyDirect(newKey, get(key));
            }
        }

        if (validSubset) {
            return c;
        } else {
            return null;
        }
    }

    /**
     * Display the configuration for debugging purposes to System.out.
     */
    public void display() {
        Iterator i = getKeys();

        while (i.hasNext()) {
            String key = (String) i.next();
            Object value = get(key);
            System.out.println(key + " => " + value);
        }
    }

    /**
     * Get a string associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated string.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a String.
     */
    public String getString(String key) {
        return getString(key, null);
    }

    /**
     * Get a string associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated string if key is found,
     * default value otherwise.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a String.
     */
    public String getString(String key, String defaultValue) {
        Object value = get(key);

        if (value instanceof String) {
            return interpolate((String) value);
            
        } else if (value == null) {
            if (defaults != null) {
                return interpolate(defaults.getString(key, defaultValue));
            } else {
                return interpolate(defaultValue);
            }
        } else if (value instanceof List) {
            return interpolate((String) ((List) value).get(0));
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a String object");
        }
    }

    /**
     * Get a list of properties associated with the given
     * configuration key.
     *
     * @param key The configuration key.
     * @return The associated properties if key is found.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a String/List.
     * @throws IllegalArgumentException if one of the tokens is
     * malformed (does not contain an equals sign).
     */
    public Properties getProperties(String key) {
        return getProperties(key, new Properties());
    }

    /**
     * Get a list of properties associated with the given
     * configuration key.
     *
     * @param key The configuration key.
     * @return The associated properties if key is found.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a String/List.
     * @throws IllegalArgumentException if one of the tokens is
     * malformed (does not contain an equals sign).
     */
    public Properties getProperties(String key, Properties defaults) {
        /*
         * Grab an array of the tokens for this key.
         */
        String[] tokens = getStringArray(key);

        // Each token is of the form 'key=value'.
        Properties props = new Properties(defaults);
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            int equalSign = token.indexOf('=');
            if (equalSign > 0) {
                String pkey = token.substring(0, equalSign).trim();
                String pvalue = token.substring(equalSign + 1).trim();
                props.put(pkey, pvalue);
            } else {
                throw new IllegalArgumentException('\'' + token + "' does not contain " + "an equals sign");
            }
        }
        return props;
    }

    /**
     * Get an array of strings associated with the given configuration
     * key.
     *
     * @param key The configuration key.
     * @return The associated string array if key is found.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a String/List.
     */
    public String[] getStringArray(String key) {
        Object value = get(key);

        List values;
        if (value instanceof String) {
            values = new Vector(1);
            values.add(value);
            
        } else if (value instanceof List) {
            values = (List) value;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getStringArray(key);
            } else {
                return new String[0];
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a String/List object");
        }

        String[] tokens = new String[values.size()];
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = (String) values.get(i);
        }

        return tokens;
    }

    /**
     * Get a Vector of strings associated with the given configuration
     * key.
     *
     * @param key The configuration key.
     * @return The associated Vector.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Vector.
     */
    public Vector getVector(String key) {
        return getVector(key, null);
    }

    /**
     * Get a Vector of strings associated with the given configuration key.
     * <p>
     * The list is a copy of the internal data of this object, and as
     * such you may alter it freely.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated Vector.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Vector.
     */
    public Vector getVector(String key, Vector defaultValue) {
        Object value = get(key);

        if (value instanceof List) {
            return new Vector((List) value);
            
        } else if (value instanceof String) {
            Vector values = new Vector(1);
            values.add(value);
            put(key, values);
            return values;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getVector(key, defaultValue);
            } else {
                return ((defaultValue == null) ? new Vector() : defaultValue);
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a Vector object");
        }
    }

    /**
     * Get a List of strings associated with the given configuration key.
     * <p>
     * The list is a copy of the internal data of this object, and as
     * such you may alter it freely.
     *
     * @param key The configuration key.
     * @return The associated List object.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a List.
     * @since Commons Collections 3.2
     */
    public List getList(String key) {
        return getList(key, null);
    }

    /**
     * Get a List of strings associated with the given configuration key.
     * <p>
     * The list is a copy of the internal data of this object, and as
     * such you may alter it freely.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated List.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a List.
     * @since Commons Collections 3.2
     */
    public List getList(String key, List defaultValue) {
        Object value = get(key);

        if (value instanceof List) {
            return new ArrayList((List) value);
            
        } else if (value instanceof String) {
            List values = new ArrayList(1);
            values.add(value);
            put(key, values);
            return values;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getList(key, defaultValue);
            } else {
                return ((defaultValue == null) ? new ArrayList() : defaultValue);
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a List object");
        }
    }

    /**
     * Get a boolean associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated boolean.
     * @throws NoSuchElementException is thrown if the key doesn't
     * map to an existing object.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Boolean.
     */
    public boolean getBoolean(String key) {
        Boolean b = getBoolean(key, null);
        if (b != null) {
            return b.booleanValue();
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    /**
     * Get a boolean associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated boolean.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Boolean.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key, new Boolean(defaultValue)).booleanValue();
    }

    /**
     * Get a boolean associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated boolean if key is found and has valid
     * format, default value otherwise.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Boolean.
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {

        Object value = get(key);

        if (value instanceof Boolean) {
            return (Boolean) value;
            
        } else if (value instanceof String) {
            String s = testBoolean((String) value);
            Boolean b = new Boolean(s);
            put(key, b);
            return b;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getBoolean(key, defaultValue);
            } else {
                return defaultValue;
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a Boolean object");
        }
    }

    /**
     * Test whether the string represent by value maps to a boolean
     * value or not. We will allow <code>true</code>, <code>on</code>,
     * and <code>yes</code> for a <code>true</code> boolean value, and
     * <code>false</code>, <code>off</code>, and <code>no</code> for
     * <code>false</code> boolean values.  Case of value to test for
     * boolean status is ignored.
     *
     * @param value  the value to test for boolean state
     * @return <code>true</code> or <code>false</code> if the supplied
     * text maps to a boolean value, or <code>null</code> otherwise.
     */
    public String testBoolean(String value) {
        String s = value.toLowerCase();

        if (s.equals("true") || s.equals("on") || s.equals("yes")) {
            return "true";
        } else if (s.equals("false") || s.equals("off") || s.equals("no")) {
            return "false";
        } else {
            return null;
        }
    }

    /**
     * Get a byte associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated byte.
     * @throws NoSuchElementException is thrown if the key doesn't
     * map to an existing object.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Byte.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public byte getByte(String key) {
        Byte b = getByte(key, null);
        if (b != null) {
            return b.byteValue();
        } else {
            throw new NoSuchElementException('\'' + key + " doesn't map to an existing object");
        }
    }

    /**
     * Get a byte associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated byte.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Byte.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public byte getByte(String key, byte defaultValue) {
        return getByte(key, new Byte(defaultValue)).byteValue();
    }

    /**
     * Get a byte associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated byte if key is found and has valid
     * format, default value otherwise.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Byte.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public Byte getByte(String key, Byte defaultValue) {
        Object value = get(key);

        if (value instanceof Byte) {
            return (Byte) value;
            
        } else if (value instanceof String) {
            Byte b = new Byte((String) value);
            put(key, b);
            return b;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getByte(key, defaultValue);
            } else {
                return defaultValue;
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a Byte object");
        }
    }

    /**
     * Get a short associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated short.
     * @throws NoSuchElementException is thrown if the key doesn't
     * map to an existing object.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Short.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public short getShort(String key) {
        Short s = getShort(key, null);
        if (s != null) {
            return s.shortValue();
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    /**
     * Get a short associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated short.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Short.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public short getShort(String key, short defaultValue) {
        return getShort(key, new Short(defaultValue)).shortValue();
    }

    /**
     * Get a short associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated short if key is found and has valid
     * format, default value otherwise.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Short.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public Short getShort(String key, Short defaultValue) {
        Object value = get(key);

        if (value instanceof Short) {
            return (Short) value;
            
        } else if (value instanceof String) {
            Short s = new Short((String) value);
            put(key, s);
            return s;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getShort(key, defaultValue);
            } else {
                return defaultValue;
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a Short object");
        }
    }

    /**
     * The purpose of this method is to get the configuration resource
     * with the given name as an integer.
     *
     * @param name The resource name.
     * @return The value of the resource as an integer.
     */
    public int getInt(String name) {
        return getInteger(name);
    }

    /**
     * The purpose of this method is to get the configuration resource
     * with the given name as an integer, or a default value.
     *
     * @param name The resource name
     * @param def The default value of the resource.
     * @return The value of the resource as an integer.
     */
    public int getInt(String name, int def) {
        return getInteger(name, def);
    }

    /**
     * Get a int associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated int.
     * @throws NoSuchElementException is thrown if the key doesn't
     * map to an existing object.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Integer.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public int getInteger(String key) {
        Integer i = getInteger(key, null);
        if (i != null) {
            return i.intValue();
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    /**
     * Get a int associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated int.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Integer.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public int getInteger(String key, int defaultValue) {
        Integer i = getInteger(key, null);

        if (i == null) {
            return defaultValue;
        }
        return i.intValue();
    }

    /**
     * Get a int associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated int if key is found and has valid
     * format, default value otherwise.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Integer.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public Integer getInteger(String key, Integer defaultValue) {
        Object value = get(key);

        if (value instanceof Integer) {
            return (Integer) value;
            
        } else if (value instanceof String) {
            Integer i = new Integer((String) value);
            put(key, i);
            return i;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getInteger(key, defaultValue);
            } else {
                return defaultValue;
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a Integer object");
        }
    }

    /**
     * Get a long associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated long.
     * @throws NoSuchElementException is thrown if the key doesn't
     * map to an existing object.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Long.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public long getLong(String key) {
        Long l = getLong(key, null);
        if (l != null) {
            return l.longValue();
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    /**
     * Get a long associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated long.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Long.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public long getLong(String key, long defaultValue) {
        return getLong(key, new Long(defaultValue)).longValue();
    }

    /**
     * Get a long associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated long if key is found and has valid
     * format, default value otherwise.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Long.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public Long getLong(String key, Long defaultValue) {
        Object value = get(key);

        if (value instanceof Long) {
            return (Long) value;
            
        } else if (value instanceof String) {
            Long l = new Long((String) value);
            put(key, l);
            return l;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getLong(key, defaultValue);
            } else {
                return defaultValue;
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a Long object");
        }
    }

    /**
     * Get a float associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated float.
     * @throws NoSuchElementException is thrown if the key doesn't
     * map to an existing object.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Float.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public float getFloat(String key) {
        Float f = getFloat(key, null);
        if (f != null) {
            return f.floatValue();
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    /**
     * Get a float associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated float.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Float.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public float getFloat(String key, float defaultValue) {
        return getFloat(key, new Float(defaultValue)).floatValue();
    }

    /**
     * Get a float associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated float if key is found and has valid
     * format, default value otherwise.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Float.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public Float getFloat(String key, Float defaultValue) {
        Object value = get(key);

        if (value instanceof Float) {
            return (Float) value;
            
        } else if (value instanceof String) {
            Float f = new Float((String) value);
            put(key, f);
            return f;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getFloat(key, defaultValue);
            } else {
                return defaultValue;
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a Float object");
        }
    }

    /**
     * Get a double associated with the given configuration key.
     *
     * @param key The configuration key.
     * @return The associated double.
     * @throws NoSuchElementException is thrown if the key doesn't
     * map to an existing object.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Double.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public double getDouble(String key) {
        Double d = getDouble(key, null);
        if (d != null) {
            return d.doubleValue();
        } else {
            throw new NoSuchElementException('\'' + key + "' doesn't map to an existing object");
        }
    }

    /**
     * Get a double associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated double.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Double.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public double getDouble(String key, double defaultValue) {
        return getDouble(key, new Double(defaultValue)).doubleValue();
    }

    /**
     * Get a double associated with the given configuration key.
     *
     * @param key The configuration key.
     * @param defaultValue The default value.
     * @return The associated double if key is found and has valid
     * format, default value otherwise.
     * @throws ClassCastException is thrown if the key maps to an
     * object that is not a Double.
     * @throws NumberFormatException is thrown if the value mapped
     * by the key has not a valid number format.
     */
    public Double getDouble(String key, Double defaultValue) {
        Object value = get(key);

        if (value instanceof Double) {
            return (Double) value;
            
        } else if (value instanceof String) {
            Double d = new Double((String) value);
            put(key, d);
            return d;
            
        } else if (value == null) {
            if (defaults != null) {
                return defaults.getDouble(key, defaultValue);
            } else {
                return defaultValue;
            }
        } else {
            throw new ClassCastException('\'' + key + "' doesn't map to a Double object");
        }
    }

    /**
     * Convert a standard properties class into a configuration class.
     * <p>
     * NOTE: From Commons Collections 3.2 this method will pick up
     * any default parent Properties of the specified input object.
     *
     * @param props  the properties object to convert
     * @return new ExtendedProperties created from props
     */
    public static ExtendedProperties convertProperties(Properties props) {
        ExtendedProperties c = new ExtendedProperties();

        for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            String s = (String) e.nextElement();
            c.setProperty(s, props.getProperty(s));
        }

        return c;
    }

}
