/*
 * $Id: FormFile.java 471754 2006-11-06 14:55:09Z husted $
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p> This interface represents a file that has been uploaded by a client. It
 * is the only interface or class in upload package which is typically
 * referenced directly by a Struts application. </p>
 */
public interface FormFile {
    /**
     * <p> Returns the content type for this file. </p>
     *
     * @return A String representing content type.
     */
    public String getContentType();

    /**
     * <p> Sets the content type for this file. </p>
     *
     * @param contentType The content type for the file.
     */
    public void setContentType(String contentType);

    /**
     * <p> Returns the size of this file. </p>
     *
     * @return The size of the file, in bytes.
     */
    public int getFileSize();

    /**
     * <p> Sets the file size. </p>
     *
     * @param fileSize The size of the file, in bytes,
     */
    public void setFileSize(int fileSize);

    /**
     * <p> Returns the file name of this file. This is the base name of the
     * file, as supplied by the user when the file was uploaded. </p>
     *
     * @return The base file name.
     */
    public String getFileName();

    /**
     * <p> Sets the file name of this file. </p>
     *
     * @param fileName The base file name.
     */
    public void setFileName(String fileName);

    /**
     * <p> Returns the data for the entire file as byte array. Care is needed
     * when using this method, since a large upload could easily exhaust
     * available memory. The preferred method for accessing the file data is
     * {@link #getInputStream() getInputStream}. </p>
     *
     * @return The file data as a byte array.
     * @throws FileNotFoundException if the uploaded file is not found.
     * @throws IOException           if an error occurred while reading the
     *                               file.
     */
    public byte[] getFileData()
        throws FileNotFoundException, IOException;

    /**
     * <p> Returns an input stream for this file. The caller must close the
     * stream when it is no longer needed. </p>
     *
     * @throws FileNotFoundException if the uploaded file is not found.
     * @throws IOException           if an error occurred while reading the
     *                               file.
     */
    public InputStream getInputStream()
        throws FileNotFoundException, IOException;

    /**
     * <p> Destroys all content for the uploaded file, including any
     * underlying data files. </p>
     */
    public void destroy();
}
