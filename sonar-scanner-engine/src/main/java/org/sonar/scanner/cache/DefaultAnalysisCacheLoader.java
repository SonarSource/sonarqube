/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.core.util.Protobuf;
import org.sonar.scanner.bootstrap.DefaultScannerWsClient;
import org.sonar.scanner.protocol.internal.ScannerInternal.SensorCacheEntry;
import org.sonar.scanner.protocol.internal.SensorCacheData;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.WsResponse;

import static org.sonar.core.util.FileUtils.humanReadableByteCountSI;

/**
 * Loads plugin cache into the local storage
 */
public class DefaultAnalysisCacheLoader implements AnalysisCacheLoader {
  private static final Logger LOG = Loggers.get(DefaultAnalysisCacheLoader.class);
  private static final String LOG_MSG = "Load analysis cache";
  static final String CONTENT_ENCODING = "Content-Encoding";
  static final String CONTENT_LENGTH = "Content-Length";
  static final String ACCEPT_ENCODING = "Accept-Encoding";
  private static final String URL = "api/analysis_cache/get";

  private final DefaultScannerWsClient wsClient;
  private final InputProject project;
  private final BranchConfiguration branchConfiguration;

  public DefaultAnalysisCacheLoader(DefaultScannerWsClient wsClient, InputProject project, BranchConfiguration branchConfiguration) {
    this.project = project;
    this.branchConfiguration = branchConfiguration;
    this.wsClient = wsClient;
  }

  @Override
  public Optional<SensorCacheData> load() {
    String url = URL + "?project=" + project.key();
    if (branchConfiguration.referenceBranchName() != null) {
      url = url + "&branch=" + branchConfiguration.referenceBranchName();
    }

    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
    GetRequest request = new GetRequest(url).setHeader(ACCEPT_ENCODING, "gzip");

    try (WsResponse response = wsClient.call(request); InputStream is = response.contentStream()) {
      Optional<String> contentEncoding = response.header(CONTENT_ENCODING);
      Optional<Integer> length = response.header(CONTENT_LENGTH).map(Integer::parseInt);
      boolean hasGzipEncoding = contentEncoding.isPresent() && contentEncoding.get().equals("gzip");

      SensorCacheData cache = hasGzipEncoding ? decompress(is) : read(is);
      if (length.isPresent()) {
        profiler.stopInfo(LOG_MSG + String.format(" (%s)", humanReadableByteCountSI(length.get())));
      } else {
        profiler.stopInfo(LOG_MSG);
      }
      return Optional.of(cache);
    } catch (HttpException e) {
      if (e.code() == 404) {
        profiler.stopInfo(LOG_MSG + " (404)");
        return Optional.empty();
      }
      throw MessageException.of("Failed to download analysis cache: " + DefaultScannerWsClient.createErrorMessage(e));
    } catch (Exception e) {
      throw new IllegalStateException("Failed to download analysis cache", e);
    }
  }

  public SensorCacheData decompress(InputStream is) throws IOException {
    try (GZIPInputStream gzipInputStream = new GZIPInputStream(is)) {
      return read(gzipInputStream);
    }
  }

  public SensorCacheData read(InputStream is) {
    Iterable<SensorCacheEntry> it = () -> Protobuf.readStream(is, SensorCacheEntry.parser());
    return new SensorCacheData(StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList()));
  }
}

