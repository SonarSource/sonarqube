/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.telemetry;

import java.util.Map;
import org.sonar.api.server.ServerSide;
import org.sonar.db.alm.setting.ALM;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * Onboarding aggregate counts surfaced in {@code api/system/info} (and, from there, the downloadable
 * support diagnostics bundle a customer's admin can send to support). This intentionally reuses the
 * telemetry providers' own DB queries rather than re-deriving them, so the numbers a customer's support
 * file shows can never drift from the numbers eventually sent as telemetry.
 *
 * <p>Support files never carry the same per-project granularity as the telemetry payloads or the onboarding
 * dashboard — per the SQS LTA Onboarding Telemetry Gap Sheet, aggregated counts are what's wanted here so
 * that customers who have opted out of telemetry (and therefore never send these numbers automatically)
 * still give support visibility into their onboarding state when they open a support case.
 *
 * <p>Deliberately excludes {@link TelemetryOnboardingDiscoveredRepoCountByAlmProvider}: that provider makes
 * a live API call per configured ALM setting, which is fine on the telemetry daemon's own schedule but would
 * make the System Info page (generated synchronously on request) slow or unreliable to load.
 */
@ServerSide
public class OnboardingSystemInfoSection implements SystemInfoSection, Global {

  private final TelemetryOnboardingCountsProvider countsProvider;
  private final TelemetryOnboardingBoundProjectsByAlmProvider boundProjectsByAlmProvider;
  private final TelemetryOnboardingLastAnalysisBucketProvider lastAnalysisBucketProvider;

  public OnboardingSystemInfoSection(TelemetryOnboardingCountsProvider countsProvider,
    TelemetryOnboardingBoundProjectsByAlmProvider boundProjectsByAlmProvider,
    TelemetryOnboardingLastAnalysisBucketProvider lastAnalysisBucketProvider) {
    this.countsProvider = countsProvider;
    this.boundProjectsByAlmProvider = boundProjectsByAlmProvider;
    this.lastAnalysisBucketProvider = lastAnalysisBucketProvider;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder section = ProtobufSystemInfo.Section.newBuilder();
    section.setName("Onboarding");

    Map<String, Integer> counts = countsProvider.getValues();
    setAttribute(section, "Total Projects", valueOf(counts, TelemetryOnboardingCountsProvider.KEY_TOTAL_PROJECTS));
    setAttribute(section, "Analysed Projects", valueOf(counts, TelemetryOnboardingCountsProvider.KEY_ANALYSED_PROJECTS));
    setAttribute(section, "ALM Imported Projects", valueOf(counts, TelemetryOnboardingCountsProvider.KEY_ALM_IMPORTED_PROJECTS));
    setAttribute(section, "Configured ALM Integrations", valueOf(counts, TelemetryOnboardingCountsProvider.KEY_CONFIGURED_ALM));

    Map<String, Integer> boundByAlm = boundProjectsByAlmProvider.getValues();
    for (ALM alm : ALM.values()) {
      setAttribute(section, "Bound Projects (" + alm.getId() + ")", valueOf(boundByAlm, alm.getId()));
    }
    setAttribute(section, "Not Bound Projects", valueOf(boundByAlm, TelemetryOnboardingBoundProjectsByAlmProvider.KEY_NOT_BOUND));
    setAttribute(section, "Not Bound but Analysed Projects",
      valueOf(boundByAlm, TelemetryOnboardingBoundProjectsByAlmProvider.KEY_NOT_BOUND_SCANNED));

    Map<String, Integer> lastAnalysisBuckets = lastAnalysisBucketProvider.getValues();
    setAttribute(section, "Projects Analysed - Last 7 Days",
      valueOf(lastAnalysisBuckets, TelemetryOnboardingLastAnalysisBucketProvider.BUCKET_LE_7D));
    setAttribute(section, "Projects Analysed - Last 30 Days",
      valueOf(lastAnalysisBuckets, TelemetryOnboardingLastAnalysisBucketProvider.BUCKET_LE_30D));
    setAttribute(section, "Projects Analysed - Last 180 Days",
      valueOf(lastAnalysisBuckets, TelemetryOnboardingLastAnalysisBucketProvider.BUCKET_LE_180D));
    setAttribute(section, "Projects Analysed - Over 180 Days",
      valueOf(lastAnalysisBuckets, TelemetryOnboardingLastAnalysisBucketProvider.BUCKET_GT_180D));
    setAttribute(section, "Projects Never Analysed",
      valueOf(lastAnalysisBuckets, TelemetryOnboardingLastAnalysisBucketProvider.BUCKET_NEVER));

    return section.build();
  }

  private static long valueOf(Map<String, Integer> values, String key) {
    return values.getOrDefault(key, 0);
  }
}
