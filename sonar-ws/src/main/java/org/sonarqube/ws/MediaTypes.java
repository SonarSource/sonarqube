/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

  public static final String JSON = "application/json";
  public static final String XML = "application/xml";
  public static final String TXT = "text/plain";
  public static final String PROTOBUF = "application/x-protobuf";
  public static final String ZIP = "application/zip";
  public static final String JAVASCRIPT = "application/javascript";
  public static final String HTML = "text/html";
  public static final String DEFAULT = "application/octet-stream";
  public static final String SVG = "image/svg+xml";

  private static final Map<String, String> MAP = new HashMap<>();

  static {
    MAP.put("js", JAVASCRIPT);
    MAP.put("json", JSON);
    MAP.put("zip", ZIP);
    MAP.put("tgz", "application/tgz");
    MAP.put("ps", "application/postscript");
    MAP.put("jnlp", "application/jnlp");
    MAP.put("jar", "application/java-archive");
    MAP.put("xls", "application/vnd.ms-excel");
    MAP.put("ppt", "application/vnd.ms-powerpoint");
    MAP.put("tar", "application/x-tar");
    MAP.put("xml", XML);
    MAP.put("dtd", "application/xml-dtd");
    MAP.put("xslt", "application/xslt+xml");
    MAP.put("bmp", "image/bmp");
    MAP.put("gif", "image/gif");
    MAP.put("jpg", "image/jpeg");
    MAP.put("jpeg", "image/jpeg");
    MAP.put("tiff", "image/tiff");
    MAP.put("png", "image/png");
    MAP.put("svg", SVG);
    MAP.put("ico", "image/x-icon");
    MAP.put("txt", TXT);
    MAP.put("csv", "text/csv");
    MAP.put("properties", TXT);
    MAP.put("rtf", "text/rtf");
    MAP.put("html", HTML);
    MAP.put("css", "text/css");
    MAP.put("tsv", "text/tab-separated-values");
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
