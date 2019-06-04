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
package org.sonar.api.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;

/**
 * Reads different types of URI. Supported schemes are http and file.
 *
 * @since 3.2
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public class UriReader {

  private final Map<String, SchemeProcessor> processorsByScheme = new HashMap<>();

  public UriReader(SchemeProcessor[] processors) {
    Stream.concat(Stream.of(new FileProcessor()), Arrays.stream(processors)).forEach(processor -> {
      for (String scheme : processor.getSupportedSchemes()) {
        processorsByScheme.put(scheme.toLowerCase(Locale.ENGLISH), processor);
      }
    });
  }

  /**
   * Reads all bytes from uri. It throws an unchecked exception if an error occurs.
   */
  public byte[] readBytes(URI uri) {
    return searchForSupportedProcessor(uri).readBytes(uri);
  }

  /**
   * Reads all characters from uri, using the given character set.
   * It throws an unchecked exception if an error occurs.
   */
  public String readString(URI uri, Charset charset) {
    return searchForSupportedProcessor(uri).readString(uri, charset);
  }

  /**
   * Returns a detailed description of the given uri. For example HTTP URIs are completed
   * with the configured HTTP proxy.
   */
  public String description(URI uri) {
    SchemeProcessor reader = searchForSupportedProcessor(uri);

    return reader.description(uri);
  }

  SchemeProcessor searchForSupportedProcessor(URI uri) {
    SchemeProcessor processor = processorsByScheme.get(uri.getScheme().toLowerCase(Locale.ENGLISH));
    Preconditions.checkArgument(processor != null, "URI schema is not supported: " + uri.getScheme());
    return processor;
  }

  public abstract static class SchemeProcessor {
    protected abstract String[] getSupportedSchemes();

    protected abstract byte[] readBytes(URI uri);

    protected abstract String readString(URI uri, Charset charset);

    protected abstract String description(URI uri);
  }

  /**
   * This implementation is not exposed in API and is kept private.
   */
  private static class FileProcessor extends SchemeProcessor {

    @Override
    public String[] getSupportedSchemes() {
      return new String[] {"file"};
    }

    @Override
    protected byte[] readBytes(URI uri) {
      try {
        return Files.readAllBytes(Paths.get(uri));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected String readString(URI uri, Charset charset) {
      try {
        return new String(Files.readAllBytes(Paths.get(uri)), charset);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected String description(URI uri) {
      return new File(uri).getAbsolutePath();
    }
  }
}
