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
package org.sonar.scanner.bootstrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sonar.core.component.DefaultResourceTypes;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.sarif.SarifSerializerImpl;
import org.sonar.scanner.cpd.JavaCpdBlockIndexerSensor;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;
import org.sonar.scanner.externalissue.ExternalIssueReportParser;
import org.sonar.scanner.externalissue.ExternalIssueReportValidator;
import org.sonar.scanner.externalissue.ExternalIssuesImportSensor;
import org.sonar.scanner.externalissue.sarif.DefaultSarif210Importer;
import org.sonar.scanner.externalissue.sarif.LocationMapper;
import org.sonar.scanner.externalissue.sarif.RegionMapper;
import org.sonar.scanner.externalissue.sarif.ResultMapper;
import org.sonar.scanner.externalissue.sarif.RuleMapper;
import org.sonar.scanner.externalissue.sarif.RunMapper;
import org.sonar.scanner.externalissue.sarif.SarifIssuesImportSensor;
import org.sonar.scanner.genericcoverage.GenericCoverageSensor;
import org.sonar.scanner.genericcoverage.GenericTestExecutionSensor;
import org.sonar.scanner.source.ZeroCoverageSensor;

public class BatchComponents {
  private BatchComponents() {
    // only static stuff
  }

  public static Collection<Object> all() {
    List<Object> components = new ArrayList<>();
    components.add(DefaultResourceTypes.get());
    components.addAll(CorePropertyDefinitions.all());
    components.add(ZeroCoverageSensor.class);
    components.add(JavaCpdBlockIndexerSensor.class);

    // Generic coverage
    components.add(GenericCoverageSensor.class);
    components.addAll(GenericCoverageSensor.properties());
    components.add(GenericTestExecutionSensor.class);
    components.addAll(GenericTestExecutionSensor.properties());
    components.add(TestPlanBuilder.class);

    // External issues
    components.add(ExternalIssueReportValidator.class);
    components.add(ExternalIssueReportParser.class);
    components.add(ExternalIssuesImportSensor.class);
    components.add(ExternalIssuesImportSensor.properties());
    components.add(SarifSerializerImpl.class);

    // Sarif issues
    components.add(SarifIssuesImportSensor.class);
    components.add(SarifIssuesImportSensor.properties());
    components.add(DefaultSarif210Importer.class);
    components.add(RunMapper.class);
    components.add(ResultMapper.class);
    components.add(LocationMapper.class);
    components.add(RegionMapper.class);
    components.add(RuleMapper.class);

    return components;
  }
}
