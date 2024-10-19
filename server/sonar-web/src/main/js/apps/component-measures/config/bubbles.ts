/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { MetricKey } from '~sonar-aligned/types/metrics';

export type BubblesByDomain = Record<
  string,
  {
    colors?: string[];
    size: string;
    x: string;
    y: string;
    yDomain?: [number, number];
  }
>;

export const newTaxonomyBubbles: BubblesByDomain = {
  Reliability: {
    x: MetricKey.ncloc,
    y: MetricKey.software_quality_reliability_remediation_effort,
    size: MetricKey.reliability_issues,
    colors: [MetricKey.software_quality_reliability_rating],
  },
  Security: {
    x: MetricKey.ncloc,
    y: MetricKey.software_quality_security_remediation_effort,
    size: MetricKey.security_issues,
    colors: [MetricKey.software_quality_security_rating],
  },
  Maintainability: {
    x: MetricKey.ncloc,
    y: MetricKey.software_quality_maintainability_remediation_effort,
    size: MetricKey.maintainability_issues,
    colors: [MetricKey.software_quality_maintainability_rating],
  },
  Coverage: {
    x: MetricKey.complexity,
    y: MetricKey.coverage,
    size: MetricKey.uncovered_lines,
    yDomain: [100, 0],
  },
  Duplications: {
    x: MetricKey.ncloc,
    y: MetricKey.duplicated_lines,
    size: MetricKey.duplicated_blocks,
  },
  project_overview: {
    x: MetricKey.software_quality_maintainability_remediation_effort,
    y: MetricKey.coverage,
    size: MetricKey.ncloc,
    colors: [
      MetricKey.software_quality_reliability_rating,
      MetricKey.software_quality_security_rating,
    ],
    yDomain: [100, 0],
  },
};

export const newTaxonomyWithoutRatingsBubbles: BubblesByDomain = {
  Reliability: {
    x: MetricKey.ncloc,
    y: MetricKey.reliability_remediation_effort,
    size: MetricKey.reliability_issues,
    colors: [MetricKey.reliability_rating],
  },
  Security: {
    x: MetricKey.ncloc,
    y: MetricKey.security_remediation_effort,
    size: MetricKey.security_issues,
    colors: [MetricKey.security_rating],
  },
  Maintainability: {
    x: MetricKey.ncloc,
    y: MetricKey.sqale_index,
    size: MetricKey.maintainability_issues,
    colors: [MetricKey.sqale_rating],
  },
  Coverage: {
    x: MetricKey.complexity,
    y: MetricKey.coverage,
    size: MetricKey.uncovered_lines,
    yDomain: [100, 0],
  },
  Duplications: {
    x: MetricKey.ncloc,
    y: MetricKey.duplicated_lines,
    size: MetricKey.duplicated_blocks,
  },
  project_overview: {
    x: MetricKey.sqale_index,
    y: MetricKey.coverage,
    size: MetricKey.ncloc,
    colors: [MetricKey.reliability_rating, MetricKey.security_rating],
    yDomain: [100, 0],
  },
};

export const legacyBubbles: BubblesByDomain = {
  Reliability: {
    x: MetricKey.ncloc,
    y: MetricKey.reliability_remediation_effort,
    size: MetricKey.bugs,
    colors: [MetricKey.reliability_rating],
  },
  Security: {
    x: MetricKey.ncloc,
    y: MetricKey.security_remediation_effort,
    size: MetricKey.vulnerabilities,
    colors: [MetricKey.security_rating],
  },
  Maintainability: {
    x: MetricKey.ncloc,
    y: MetricKey.sqale_index,
    size: MetricKey.code_smells,
    colors: [MetricKey.sqale_rating],
  },
  Coverage: {
    x: MetricKey.complexity,
    y: MetricKey.coverage,
    size: MetricKey.uncovered_lines,
    yDomain: [100, 0],
  },
  Duplications: {
    x: MetricKey.ncloc,
    y: MetricKey.duplicated_lines,
    size: MetricKey.duplicated_blocks,
  },
  project_overview: {
    x: MetricKey.sqale_index,
    y: MetricKey.coverage,
    size: MetricKey.ncloc,
    colors: [MetricKey.reliability_rating, MetricKey.security_rating],
    yDomain: [100, 0],
  },
};
