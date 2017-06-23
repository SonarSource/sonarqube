/*
 * $Id: CommonsMultipartRequestHandler.java 524895 2007-04-02 19:29:21Z germuska $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts.upload;

import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ModuleConfig;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * <p> This class implements the <code>MultipartRequestHandler</code>
 * interface by providing a wrapper around the Jakarta Commons FileUpload
 * library. </p>
 *
 * @version $Rev: 524895 $ $Date: 2007-04-02 21:29:21 +0200 (Mon, 02 Apr 2007) $
 * @since Struts 1.1
 */
public class CommonsMultipartRequestHandler implements MultipartRequestHandler {
    // ----------------------------------------------------- Manifest Constants

    /**
     * <p> The default value for the maximum allowable size, in bytes, of an
     * uploaded file. The value is equivalent to 250MB. </p>
     */
    public static final long DEFAULT_SIZE_MAX = 250 * 1024 * 1024;

    /**
     * <p> The default value for the threshold which determines whether an
     * uploaded file will be written to disk or cached in memory. The value is
     * equivalent to 250KB. </p>
     */
    public static final int DEFAULT_SIZE_THRESHOLD = 256 * 1024;

    // ----------------------------------------------------- Instance Variables

    /**
     * <p> Commons Logging instance. </p>
     */
    protected static Log log =
        LogFactory.getLog(CommonsMultipartRequestHandler.class);

    /**
     * <p> The combined text and file request parameters. </p>
     */
    private Hashtable elementsAll;

    /**
     * <p> The file request parameters. </p>
     */
    private Hashtable elementsFile;

    /**
     * <p> The text request parameters. </p>
     */
    private Hashtable elementsText;

    /**
     * <p> The action mapping  with which this handler is associated. </p>
     */
    private ActionMapping mapping;

    /**
     * <p> The servlet with which this handler is associated. </p>
     */
    private ActionServlet servlet;

    // ---------------------------------------- MultipartRequestHandler Methods

    /**
     * <p> Retrieves the servlet with which this handler is associated. </p>
     *
     * @return The associated servlet.
     */
    public ActionServlet getServlet() {
        return this.servlet;
    }

    /**
     * <p> Sets the servlet with which this handler is associated. </p>
     *
     * @param servlet The associated servlet.
     */
    public void setServlet(ActionServlet servlet) {
        this.servlet = servlet;
    }

    /**
     * <p> Retrieves the action mapping with which this handler is associated.
     * </p>
     *
     * @return The associated action mapping.
     */
    public ActionMapping getMapping() {
        return this.mapping;
    }

    /**
     * <p> Sets the action mapping with which this handler is associated.
     * </p>
     *
     * @param mapping The associated action mapping.
     */
    public void setMapping(ActionMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * <p> Parses the input stream and partitions the parsed items into a set
     * of form fields and a set of file items. In the process, the parsed
     * items are translated from Commons FileUpload <code>FileItem</code>
     * instances to Struts <code>FormFile</code> instances. </p>
     *
     * @param request The multipart request to be processed.
     * @throws ServletException if an unrecoverable error occurs.
     */
    public void handleRequest(HttpServletRequest request)
        throws ServletException {
        // Get the app config for the current request.
        ModuleConfig ac =
            (ModuleConfig) request.getAttribute(Globals.MODULE_KEY);

        // Create and configure a DIskFileUpload instance.
        DiskFileUpload upload = new DiskFileUpload();

        // The following line is to support an "EncodingFilter"
        // see http://issues.apache.org/bugzilla/show_bug.cgi?id=23255
        upload.setHeaderEncoding(request.getCharacterEncoding());

        // Set the maximum size before a FileUploadException will be thrown.
        upload.setSizeMax(getSizeMax(ac));

        // Set the maximum size that will be stored in memory.
        upload.setSizeThreshold((int) getSizeThreshold(ac));

        // Set the the location for saving data on disk.
        upload.setRepositoryPath(getRepositoryPath(ac));

        // Create the hash tables to be populated.
        elementsText = new Hashtable();
        elementsFile = new Hashtable();
        elementsAll = new Hashtable();

        // Parse the request into file items.
        List items = null;

        try {
            items = upload.parseRequest(request);
        } catch (DiskFileUpload.SizeLimitExceededException e) {
            // Special handling for uploads that are too big.
            request.setAttribute(MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED,
                Boolean.TRUE);

            return;
        } catch (FileUploadException e) {
            log.error("Failed to parse multipart request", e);
            throw new ServletException(e);
        }

        // Partition the items into form fields and files.
        Iterator iter = items.iterator();

        while (iter.hasNext()) {
            FileItem item = (FileItem) iter.next();

            if (item.isFormField()) {
                addTextParameter(request, item);
            } else {
                addFileParameter(item);
            }
        }
    }

    /**
     * <p> Returns a hash table containing the text (that is, non-file)
     * request parameters. </p>
     *
     * @return The text request parameters.
     */
    public Hashtable getTextElements() {
        return this.elementsText;
    }

    /**
     * <p> Returns a hash table containing the file (that is, non-text)
     * request parameters. </p>
     *
     * @return The file request parameters.
     */
    public Hashtable getFileElements() {
        return this.elementsFile;
    }

    /**
     * <p> Returns a hash table containing both text and file request
     * parameters. </p>
     *
     * @return The text and file request parameters.
     */
    public Hashtable getAllElements() {
        return this.elementsAll;
    }

    /**
     * <p> Cleans up when a problem occurs during request processing. </p>
     */
    public void rollback() {
        Iterator iter = elementsFile.values().iterator();

        Object o;
        while (iter.hasNext()) {
            o = iter.next();
            if (o instanceof List) {
                for (Iterator i = ((List)o).iterator(); i.hasNext(); ) {
                    ((FormFile)i.next()).destroy();
                }
            } else {
                ((FormFile)o).destroy();
            }
        }
    }

    /**
     * <p> Cleans up at the end of a request. </p>
     */
    public void finish() {
        rollback();
    }

    // -------------------------------------------------------- Support Methods

    /**
     * <p> Returns the maximum allowable size, in bytes, of an uploaded file.
     * The value is obtained from the current module's controller
     * configuration. </p>
     *
     * @param mc The current module's configuration.
     * @return The maximum allowable file size, in bytes.
     */
    protected long getSizeMax(ModuleConfig mc) {
        return convertSizeToBytes(mc.getControllerConfig().getMaxFileSize(),
            DEFAULT_SIZE_MAX);
    }

    /**
     * <p> Returns the size threshold which determines whether an uploaded
     * file will be written to disk or cached in memory. </p>
     *
     * @param mc The current module's configuration.
     * @return The size threshold, in bytes.
     */
    protected long getSizeThreshold(ModuleConfig mc) {
        return convertSizeToBytes(mc.getControllerConfig().getMemFileSize(),
            DEFAULT_SIZE_THRESHOLD);
    }

    /**
     * <p> Converts a size value from a string representation to its numeric
     * value. The string must be of the form nnnm, where nnn is an arbitrary
     * decimal value, and m is a multiplier. The multiplier must be one of
     * 'K', 'M' and 'G', representing kilobytes, megabytes and gigabytes
     * respectively. </p><p> If the size value cannot be converted, for
     * example due to invalid syntax, the supplied default is returned
     * instead. </p>
     *
     * @param sizeString  The string representation of the size to be
     *                    converted.
     * @param defaultSize The value to be returned if the string is invalid.
     * @return The actual size in bytes.
     */
    protected long convertSizeToBytes(String sizeString, long defaultSize) {
        int multiplier = 1;

        if (sizeString.endsWith("K")) {
            multiplier = 1024;
        } else if (sizeString.endsWith("M")) {
            multiplier = 1024 * 1024;
        } else if (sizeString.endsWith("G")) {
            multiplier = 1024 * 1024 * 1024;
        }

        if (multiplier != 1) {
            sizeString = sizeString.substring(0, sizeString.length() - 1);
        }

        long size = 0;

        try {
            size = Long.parseLong(sizeString);
        } catch (NumberFormatException nfe) {
            log.warn("Invalid format for file size ('" + sizeString
                + "'). Using default.");
            size = defaultSize;
            multiplier = 1;
        }

        return (size * multiplier);
    }

    /**
     * <p> Returns the path to the temporary directory to be used for uploaded
     * files which are written to disk. The directory used is determined from
     * the first of the following to be non-empty. <ol> <li>A temp dir
     * explicitly defined either using the <code>tempDir</code> servlet init
     * param, or the <code>tempDir</code> attribute of the &lt;controller&gt;
     * element in the Struts config file.</li> <li>The container-specified
     * temp dir, obtained from the <code>javax.servlet.context.tempdir</code>
     * servlet context attribute.</li> <li>The temp dir specified by the
     * <code>java.io.tmpdir</code> system property.</li> (/ol> </p>
     *
     * @param mc The module config instance for which the path should be
     *           determined.
     * @return The path to the directory to be used to store uploaded files.
     */
    protected String getRepositoryPath(ModuleConfig mc) {
        // First, look for an explicitly defined temp dir.
        String tempDir = mc.getControllerConfig().getTempDir();

        // If none, look for a container specified temp dir.
        if ((tempDir == null) || (tempDir.length() == 0)) {
            if (servlet != null) {
                ServletContext context = servlet.getServletContext();
                File tempDirFile =
                    (File) context.getAttribute("javax.servlet.context.tempdir");

                tempDir = tempDirFile.getAbsolutePath();
            }

            // If none, pick up the system temp dir.
            if ((tempDir == null) || (tempDir.length() == 0)) {
                tempDir = System.getProperty("java.io.tmpdir");
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("File upload temp dir: " + tempDir);
        }

        return tempDir;
    }

    /**
     * <p> Adds a regular text parameter to the set of text parameters for
     * this request and also to the list of all parameters. Handles the case
     * of multiple values for the same parameter by using an array for the
     * parameter value. </p>
     *
     * @param request The request in which the parameter was specified.
     * @param item    The file item for the parameter to add.
     */
    protected void addTextParameter(HttpServletRequest request, FileItem item) {
        String name = item.getFieldName();
        String value = null;
        boolean haveValue = false;
        String encoding = null;

        if (item instanceof DiskFileItem) {
            encoding = ((DiskFileItem)item).getCharSet();
            if (log.isDebugEnabled()) {
                log.debug("DiskFileItem.getCharSet=[" + encoding + "]");
            }
        }

        if (encoding == null) {
            encoding = request.getCharacterEncoding();
            if (log.isDebugEnabled()) {
                log.debug("request.getCharacterEncoding=[" + encoding + "]");
            }
        }

        if (encoding != null) {
            try {
                value = item.getString(encoding);
                haveValue = true;
            } catch (Exception e) {
                // Handled below, since haveValue is false.
            }
        }

        if (!haveValue) {
            try {
                value = item.getString("ISO-8859-1");
            } catch (java.io.UnsupportedEncodingException uee) {
                value = item.getString();
            }

            haveValue = true;
        }

        if (request instanceof MultipartRequestWrapper) {
            MultipartRequestWrapper wrapper = (MultipartRequestWrapper) request;

            wrapper.setParameter(name, value);
        }

        String[] oldArray = (String[]) elementsText.get(name);
        String[] newArray;

        if (oldArray != null) {
            newArray = new String[oldArray.length + 1];
            System.arraycopy(oldArray, 0, newArray, 0, oldArray.length);
            newArray[oldArray.length] = value;
        } else {
            newArray = new String[] { value };
        }

        elementsText.put(name, newArray);
        elementsAll.put(name, newArray);
    }

    /**
     * <p> Adds a file parameter to the set of file parameters for this
     * request and also to the list of all parameters. </p>
     *
     * @param item The file item for the parameter to add.
     */
    protected void addFileParameter(FileItem item) {
        FormFile formFile = new CommonsFormFile(item);

        String name = item.getFieldName();
        if (elementsFile.containsKey(name)) {
            Object o = elementsFile.get(name);
            if (o instanceof List) {
                ((List)o).add(formFile);
            } else {
                List list = new ArrayList();
                list.add((FormFile)o);
                list.add(formFile);
                elementsFile.put(name, list);
                elementsAll.put(name, list);
            }
        } else {
            elementsFile.put(name, formFile);
            elementsAll.put(name, formFile);
        }
    }

    // ---------------------------------------------------------- Inner Classes

    /**
     * <p> This class implements the Struts <code>FormFile</code> interface by
     * wrapping the Commons FileUpload <code>FileItem</code> interface. This
     * implementation is <i>read-only</i>; any attempt to modify an instance
     * of this class will result in an <code>UnsupportedOperationException</code>.
     * </p>
     */
    static class CommonsFormFile implements FormFile, Serializable {
        /**
         * <p> The <code>FileItem</code> instance wrapped by this object.
         * </p>
         */
        FileItem fileItem;

        /**
         * Constructs an instance of this class which wraps the supplied file
         * item. </p>
         *
         * @param fileItem The Commons file item to be wrapped.
         */
        public CommonsFormFile(FileItem fileItem) {
            this.fileItem = fileItem;
        }

        /**
         * <p> Returns the content type for this file. </p>
         *
         * @return A String representing content type.
         */
        public String getContentType() {
            return fileItem.getContentType();
        }

        /**
         * <p> Sets the content type for this file. <p> NOTE: This method is
         * not supported in this implementation. </p>
         *
         * @param contentType A string representing the content type.
         */
        public void setContentType(String contentType) {
            throw new UnsupportedOperationException(
                "The setContentType() method is not supported.");
        }

        /**
         * <p> Returns the size, in bytes, of this file. </p>
         *
         * @return The size of the file, in bytes.
         */
        public int getFileSize() {
            return (int) fileItem.getSize();
        }

        /**
         * <p> Sets the size, in bytes, for this file. <p> NOTE: This method
         * is not supported in this implementation. </p>
         *
         * @param filesize The size of the file, in bytes.
         */
        public void setFileSize(int filesize) {
            throw new UnsupportedOperationException(
                "The setFileSize() method is not supported.");
        }

        /**
         * <p> Returns the (client-side) file name for this file. </p>
         *
         * @return The client-size file name.
         */
        public String getFileName() {
            return getBaseFileName(fileItem.getName());
        }

        /**
         * <p> Sets the (client-side) file name for this file. <p> NOTE: This
         * method is not supported in this implementation. </p>
         *
         * @param fileName The client-side name for the file.
         */
        public void setFileName(String fileName) {
            throw new UnsupportedOperationException(
                "The setFileName() method is not supported.");
        }

        /**
         * <p> Returns the data for this file as a byte array. Note that this
         * may result in excessive memory usage for large uploads. The use of
         * the {@link #getInputStream() getInputStream} method is encouraged
         * as an alternative. </p>
         *
         * @return An array of bytes representing the data contained in this
         *         form file.
         * @throws FileNotFoundException If some sort of file representation
         *                               cannot be found for the FormFile
         * @throws IOException           If there is some sort of IOException
         */
        public byte[] getFileData()
            throws FileNotFoundException, IOException {
            return fileItem.get();
        }

        /**
         * <p> Get an InputStream that represents this file.  This is the
         * preferred method of getting file data. </p>
         *
         * @throws FileNotFoundException If some sort of file representation
         *                               cannot be found for the FormFile
         * @throws IOException           If there is some sort of IOException
         */
        public InputStream getInputStream()
            throws FileNotFoundException, IOException {
            return fileItem.getInputStream();
        }

        /**
         * <p> Destroy all content for this form file. Implementations should
         * remove any temporary files or any temporary file data stored
         * somewhere </p>
         */
        public void destroy() {
            fileItem.delete();
        }

        /**
         * <p> Returns the base file name from the supplied file path. On the
         * surface, this would appear to be a trivial task. Apparently,
         * however, some Linux JDKs do not implement <code>File.getName()</code>
         * correctly for Windows paths, so we attempt to take care of that
         * here. </p>
         *
         * @param filePath The full path to the file.
         * @return The base file name, from the end of the path.
         */
        protected String getBaseFileName(String filePath) {
            // First, ask the JDK for the base file name.
            String fileName = new File(filePath).getName();

            // Now check for a Windows file name parsed incorrectly.
            int colonIndex = fileName.indexOf(":");

            if (colonIndex == -1) {
                // Check for a Windows SMB file path.
                colonIndex = fileName.indexOf("\\\\");
            }

            int backslashIndex = fileName.lastIndexOf("\\");

            if ((colonIndex > -1) && (backslashIndex > -1)) {
                // Consider this filename to be a full Windows path, and parse it
                // accordingly to retrieve just the base file name.
                fileName = fileName.substring(backslashIndex + 1);
            }

            return fileName;
        }

        /**
         * <p> Returns the (client-side) file name for this file. </p>
         *
         * @return The client-size file name.
         */
        public String toString() {
            return getFileName();
        }
    }
}
