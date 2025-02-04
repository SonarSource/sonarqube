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
package org.sonar.ce.task.projectanalysis.step;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.step.ComputationStep;

public class BuildSoftwareCompositionAnalysisStep implements ComputationStep {
  private static final Logger LOG = LoggerFactory.getLogger(BuildSoftwareCompositionAnalysisStep.class);

  private final ScannerReportReader reportReader;
  private final OkHttpClient okHttpClient;

  // TODO: Remove tideliftUploadUrl and tideliftApiKey when the sonar cloud proxy is ready.
  private String tideliftUploadUrl = "https://api.tidelift.com/sonar-api/v1/releases/parse-dependency-files";
  private String tideliftApiKey = System.getenv("TIDELIFT_API_KEY");

  public BuildSoftwareCompositionAnalysisStep(ScannerReportReader reportReader, OkHttpClient okHttpClient) {
    this.reportReader = reportReader;
    this.okHttpClient = okHttpClient;
  }

  public void setTideliftUploadUrl(String tideliftUploadUrl) {
    this.tideliftUploadUrl = tideliftUploadUrl;
  }

  public void setTideliftApiKey(String tideliftApiKey) {
    this.tideliftApiKey = tideliftApiKey;
  }

  @Override
  public String getDescription() {
    return "Compute software composition analysis";
  }

  @Override
  public void execute(Context context) {
    LOG.info("BuildSoftwareCompositionAnalysisStep start");
    if (tideliftApiKey == null) {
      LOG.warn("TIDELIFT_API_KEY is not set");
      return;
    }

    var zipFile = reportReader.readDependencyFilesZip();
    if (zipFile == null) {
      LOG.warn("No dependency files found");
      return;
    }
    try {
      analyzeDependencyFiles(zipFile);
    } catch (IOException e) {
      LOG.error("Invalid dependency-files.zip", e);
    }
    LOG.info("BuildSoftwareCompositionAnalysisStep end");
  }

  private void analyzeDependencyFiles(File zipFile) throws IOException {
    MultipartBody multipartBody = new MultipartBody.Builder()
      .setType(MultipartBody.FORM)
      .addFormDataPart(
        "files[]",
        "dependency-files.zip",
        RequestBody.create(zipFile, MediaType.parse("application/zip")))
      .build();

    Request request = new Request.Builder()
      .url(tideliftUploadUrl)
      .addHeader("Authorization", tideliftApiKey)
      .post(multipartBody)
      .build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (response.isSuccessful()) {
        var json = response.body().string();
        // TODO: instead of logging, save to a Holder to be consumed by a Persist ComputationStep
        LOG.info("Processing successful! Tidelift Response:{}", json);
        GsonAnalysisResponse analysisResponse = new Gson().fromJson(json, GsonAnalysisResponse.class);

        analysisResponse.releases.forEach(release -> LOG.info("Release: {} VulnerabilityCount: {}", release.purl, release.violations.stream().count()));

      } else {
        LOG.warn("Processing failed. Tidelift Response code:{} message:{}", response.code(), response.message());
      }
    }
  }

  public record GsonAnalysisResponse(List<GsonRelease> releases, List<GsonRelease> missing_releases) {
  }

  public record GsonRelease(String platform, String name, String version, String purl, GsonLicense license,
    List<GsonViolation> violations, Boolean direct, String type, String requirement,
    String manifest_source, String lockfile_source) {
  }

  public record GsonLicense(String expression, String source) {
  }

  public record GsonViolation(String catalog_standard, GsonVulnerability vulnerability) {
  }

  public record GsonVulnerability(String id, String severity, String description, String date, String url,
    String severity_rating, String epss_percentile, String epss_probability,
    boolean known_exploited) {
  }
}
