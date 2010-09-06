/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

import java.io.*;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class SourceCode {

  public static final String EOL = System.getProperty("line.separator", "\n");

    public static abstract class CodeLoader {
        private SoftReference<List<String>> code;

        public List<String> getCode() {
            List<String> c = null;
            if (code != null) {
                c = code.get();
            }
            if (c != null) {
                return c;
            }
            this.code = new SoftReference<List<String>>(load());
            return code.get();
        }

        public abstract String getFileName();

        protected abstract Reader getReader() throws Exception;

        protected List<String> load() {
            LineNumberReader lnr = null;
            try {
                lnr = new LineNumberReader(getReader());
                List<String> lines = new ArrayList<String>();
                String currentLine;
                while ((currentLine = lnr.readLine()) != null) {
                    lines.add(currentLine);
                }
                return lines;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Problem while reading " + getFileName() + ":" + e.getMessage());
            } finally {
                try {
                    if (lnr != null)
                        lnr.close();
                } catch (Exception e) {
                    throw new RuntimeException("Problem while reading " + getFileName() + ":" + e.getMessage());
                }
            }
        }
    }

    public static class FileCodeLoader extends CodeLoader {
        private File file;
        private String encoding;

        public FileCodeLoader(File file, String encoding) {
            this.file = file;
            this.encoding = encoding;
        }

        public Reader getReader() throws Exception {
            return new InputStreamReader(new FileInputStream(file), encoding);
        }

        public String getFileName() {
            return this.file.getAbsolutePath();
        }
    }

    public static class StringCodeLoader extends CodeLoader {
        public static final String DEFAULT_NAME = "CODE_LOADED_FROM_STRING";

        private String source_code;

        private String name;

        public StringCodeLoader(String code) {
            this(code, DEFAULT_NAME);
        }

        public StringCodeLoader(String code, String name) {
            this.source_code = code;
            this.name = name;
        }

        public Reader getReader() {
            return new StringReader(source_code);
        }

        public String getFileName() {
            return name;
        }
    }

    private CodeLoader cl;

    public SourceCode(CodeLoader cl) {
        this.cl = cl;
    }

    public List<String> getCode() {
        return cl.getCode();
    }

    public StringBuffer getCodeBuffer() {
        StringBuffer sb = new StringBuffer();
        List<String> lines = cl.getCode();
        for ( String line : lines ) {
            sb.append(line);
            sb.append(EOL);
        }
        return sb;
    }

    public String getSlice(int startLine, int endLine) {
        StringBuffer sb = new StringBuffer();
        List lines = cl.getCode();
	for (int i = (startLine == 0 ? startLine :startLine - 1); i < endLine && i < lines.size(); i++) {
		if (sb.length() != 0) {
                sb.append(EOL);
            }
            sb.append((String) lines.get(i));
        }
        return sb.toString();
    }

    public String getFileName() {
        return cl.getFileName();
    }
}
