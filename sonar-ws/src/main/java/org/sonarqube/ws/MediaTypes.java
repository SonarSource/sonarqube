/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.ws;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

/**
 * @since 5.3
 */
public final class MediaTypes {

  public static final String JSON = "application/json";
  public static final String XML = "application/xml";
  public static final String TXT = "text/plain";
  public static final String PROTOBUF = "application/x-protobuf";
  public static final String ZIP = "application/zip";
  public static final String JAVASCRIPT = "application/javascript";
  public static final String HTML = "text/html";
  public static final String DEFAULT = "application/octet-stream";
  public static final String SVG = "image/svg+xml";

  private static final Map<String, String> MAP = new ImmutableMap.Builder<String, String>()
    .put("js", JAVASCRIPT)
    .put("json", JSON)
    .put("zip", ZIP)
    .put("tgz", "application/tgz")
    .put("ps", "application/postscript")
    .put("jnlp", "application/jnlp")
    .put("jar", "application/java-archive")
    .put("xls", "application/vnd.ms-excel")
    .put("ppt", "application/vnd.ms-powerpoint")
    .put("tar", "application/x-tar")
    .put("xml", XML)
    .put("dtd", "application/xml-dtd")
    .put("xslt", "application/xslt+xml")
    .put("bmp", "image/bmp")
    .put("gif", "image/gif")
    .put("jpg", "image/jpeg")
    .put("jpeg", "image/jpeg")
    .put("tiff", "image/tiff")
    .put("png", "image/png")
    .put("svg", SVG)
    .put("ico", "image/x-icon")
    .put("txt", TXT)
    .put("csv", "text/csv")
    .put("properties", TXT)
    .put("rtf", "text/rtf")
    .put("html", HTML)
    .put("css", "text/css")
    .put("tsv", "text/tab-separated-values")
    .build();

  private MediaTypes() {
    // only static methods
  }

  public static String getByFilename(String filename) {
    String extension = FilenameUtils.getExtension(filename);
    String mime = null;
    if (!Strings.isNullOrEmpty(extension)) {
      mime = MAP.get(extension.toLowerCase(Locale.ENGLISH));
    }
    return mime != null ? mime : DEFAULT;
  }
}
