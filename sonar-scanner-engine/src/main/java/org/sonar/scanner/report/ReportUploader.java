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
package org.sonar.scanner.report;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.util.Protobuf;
import org.sonar.scanner.bootstrap.ScannerWsClient;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata.Builder;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;

public class ReportUploader {

  private static final String CHARACTERISTIC = "characteristic";

  @Parameter(names = "--help", help = true)
  private boolean help;

  @Parameter(names = {"--reportDir", "-r"}, converter = PathConverter.class, required = true)
  Path reportDir;
  @Parameter(names = {"--serverUrl"})
  String serverUrl;
  @Parameter(names = {"--projectKey", "-k"}, required = true)
  String projectKey;
  @Parameter(names = {"--projectName", "-n"}, required = true)
  String projectName;
  @Parameter(names = {"--branchName", "-b"})
  String branchName;
  @Parameter(names = {"--branchType", "-t"})
  String branchType;
  @Parameter(names = {"--login", "-u"})
  String login;
  @Parameter(names = {"--password", "-p"})
  String password;

  public static void main(String... args) throws IOException {
    ReportUploader main = new ReportUploader();
    JCommander jCommander = JCommander.newBuilder()
      .addObject(main)
      .build();
    jCommander.parse(args);
    if (main.help) {
      jCommander.usage();
      return;
    }
    main.run();
  }

  public void run() throws IOException {
    patchReport();

    Path reportZip = createZip();

    uploadReport(reportZip);
  }

  private void uploadReport(Path reportZip) {
    String url = defaultIfBlank(serverUrl, CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE);
    HttpConnector.Builder connectorBuilder = HttpConnector.newBuilder();

    connectorBuilder
      .readTimeoutMilliseconds(60_000)
      .connectTimeoutMilliseconds(10_000)
      .userAgent("Report Uploader")
      .url(url)
      .credentials(login, password);

    WsClient wsClient = WsClientFactories.getDefault().newClient(connectorBuilder.build());

    PostRequest.Part filePart = new PostRequest.Part(MediaTypes.ZIP, reportZip.toFile());
    PostRequest post = new PostRequest("api/ce/submit")
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam("projectKey", projectKey)
      .setParam("projectName", projectName)
      .setPart("report", filePart);

    if (branchName != null) {
      post.setParam(CHARACTERISTIC, "branch=" + branchName)
        .setParam(CHARACTERISTIC, "branchType=" + Objects.requireNonNull(branchType));
    }

    try {
      wsClient.wsConnector().call(post).failIfNotSuccessful();
    } catch (HttpException e) {
      throw MessageException.of(String.format("Failed to upload report - %d: %s", e.code(), ScannerWsClient.tryParseAsJsonError(e.content())));
    }
  }

  private Path createZip() throws IOException {
    Path reportZip = Files.createTempFile("scanner-report", ".zip");
    reportZip.toFile().deleteOnExit();
    ZipUtils.zipDir(reportDir.toFile(), reportZip.toFile());
    return reportZip;
  }

  private void patchReport() {
    int rootComponentRef = patchMetadata();
    patchRootComponent(rootComponentRef);
  }

  private int patchMetadata() {
    File metadataFile = new File(reportDir.toFile(), "metadata.pb");
    Metadata metadata = Protobuf.read(metadataFile, ScannerReport.Metadata.parser());

    Builder patchedMetadata = ScannerReport.Metadata.newBuilder(metadata);

    patchedMetadata.setProjectKey(projectKey);

    Protobuf.write(patchedMetadata.build(), metadataFile);

    return metadata.getRootComponentRef();
  }

  private void patchRootComponent(int rootComponentRef) {
    File rootComponentFile = new File(reportDir.toFile(), "component-" + rootComponentRef + ".pb");
    ScannerReport.Component rootComponent = Protobuf.read(rootComponentFile, ScannerReport.Component.parser());

    org.sonar.scanner.protocol.output.ScannerReport.Component.Builder patchedRootComponent = ScannerReport.Component.newBuilder(rootComponent);
    patchedRootComponent.setKey(projectKey);
    patchedRootComponent.setName(projectName);

    Protobuf.write(patchedRootComponent.build(), rootComponentFile);
  }

  public class PathConverter implements IStringConverter<Path> {
    @Override
    public Path convert(String value) {
      return Paths.get(value);
    }
  }

}
