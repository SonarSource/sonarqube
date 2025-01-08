/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.sonarqube.ws.WsUtils.isNullOrEmpty;

/**
 * @since 5.3
 */
public final class MediaTypes {
  public static final String CSV = "text/csv";
  public static final String DEFAULT = "application/octet-stream";
  public static final String HTML = "text/html";
  public static final String JAVASCRIPT = "text/javascript";
  public static final String JSON = "application/json";
  public static final String PROTOBUF = "application/x-protobuf";
  public static final String SVG = "image/svg+xml";
  public static final String TXT = "text/plain";
  public static final String XML = "application/xml";
  public static final String ZIP = "application/zip";
  private static final String BMP = "image/bmp";
  private static final String CSS = "text/css";
  private static final String DTD = "application/xml-dtd";
  private static final String GIF = "image/gif";
  private static final String ICO = "image/x-icon";
  private static final String JAR = "application/java-archive";
  private static final String JNLP = "application/jnlp";
  private static final String JPEG = "image/jpeg";
  private static final String JPG = "image/jpeg";
  private static final String PNG = "image/png";
  private static final String POSTSCRIPT = "application/postscript";
  private static final String PPT = "application/vnd.ms-powerpoint";
  private static final String RTF = "text/rtf";
  private static final String TAR = "application/x-tar";
  private static final String TIFF = "image/tiff";
  private static final String TGZ = "application/tgz";
  private static final String TSV = "text/tab-separated-values";
  private static final String WOFF2 = "application/font-woff2";
  private static final String XLS = "application/vnd.ms-excel";
  private static final String XSLT = "application/xslt+xml";

  private static final Map<String, String> MAP;
  static {
    MAP = new HashMap<>(27);
    MAP.put("js", JAVASCRIPT);
    MAP.put("json", JSON);
    MAP.put("zip", ZIP);
    MAP.put("tgz", TGZ);
    MAP.put("ps", POSTSCRIPT);
    MAP.put("jnlp", JNLP);
    MAP.put("jar", JAR);
    MAP.put("xls", XLS);
    MAP.put("ppt", PPT);
    MAP.put("tar", TAR);
    MAP.put("xml", XML);
    MAP.put("dtd", DTD);
    MAP.put("xslt", XSLT);
    MAP.put("bmp", BMP);
    MAP.put("gif", GIF);
    MAP.put("jpg", JPG);
    MAP.put("jpeg", JPEG);
    MAP.put("tiff", TIFF);
    MAP.put("png", PNG);
    MAP.put("svg", SVG);
    MAP.put("ico", ICO);
    MAP.put("txt", TXT);
    MAP.put("csv", CSV);
    MAP.put("properties", TXT);
    MAP.put("rtf", RTF);
    MAP.put("html", HTML);
    MAP.put("css", CSS);
    MAP.put("tsv", TSV);
    MAP.put("woff2", WOFF2);
  }

  private MediaTypes() {
    // only static methods
  }

  public static String getByFilename(String filename) {
    String extension = FilenameUtils.getExtension(filename);
    String mime = null;
    if (!isNullOrEmpty(extension)) {
      mime = MAP.get(extension.toLowerCase(Locale.ENGLISH));
    }
    return mime != null ? mime : DEFAULT;
  }
}
