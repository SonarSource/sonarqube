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
import { SOFTWARE_QUALITY_RATING_METRICS_MAP } from '../../../helpers/constants';

interface Domains {
  [domain: string]: { categories?: string[]; order: string[] };
}

const NEW_CODE_CATEGORY = 'new_code_category';
const OVERALL_CATEGORY = 'overall_category';

export const domains: Domains = {
  Reliability: {
    categories: [NEW_CODE_CATEGORY, OVERALL_CATEGORY],
    order: [
      NEW_CODE_CATEGORY,
      MetricKey.new_software_quality_reliability_issues,
      MetricKey.new_bugs,
      MetricKey.new_reliability_rating,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.new_reliability_rating],
      MetricKey.new_reliability_remediation_effort,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.new_reliability_remediation_effort],

      OVERALL_CATEGORY,
      MetricKey.software_quality_reliability_issues,
      MetricKey.bugs,
      MetricKey.reliability_rating,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.reliability_rating],
      MetricKey.reliability_remediation_effort,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.reliability_remediation_effort],
    ],
  },

  Security: {
    categories: [NEW_CODE_CATEGORY, OVERALL_CATEGORY],
    order: [
      NEW_CODE_CATEGORY,
      MetricKey.new_software_quality_security_issues,
      MetricKey.new_vulnerabilities,
      MetricKey.new_security_rating,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.new_security_rating],
      MetricKey.new_security_remediation_effort,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.new_security_remediation_effort],

      OVERALL_CATEGORY,
      MetricKey.software_quality_security_issues,
      MetricKey.vulnerabilities,
      MetricKey.security_rating,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.security_rating],
      MetricKey.security_remediation_effort,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.security_remediation_effort],
    ],
  },

  SecurityReview: {
    categories: [NEW_CODE_CATEGORY, OVERALL_CATEGORY],
    order: [
      NEW_CODE_CATEGORY,
      MetricKey.new_security_hotspots,
      MetricKey.new_security_review_rating,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.new_security_review_rating],
      MetricKey.new_security_hotspots_reviewed,

      OVERALL_CATEGORY,
      MetricKey.security_hotspots,
      MetricKey.security_review_rating,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.security_review_rating],
      MetricKey.security_hotspots_reviewed,
    ],
  },

  Maintainability: {
    categories: [NEW_CODE_CATEGORY, OVERALL_CATEGORY],
    order: [
      NEW_CODE_CATEGORY,
      MetricKey.new_software_quality_maintainability_issues,
      MetricKey.new_code_smells,
      MetricKey.new_technical_debt,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.new_technical_debt],
      MetricKey.new_sqale_debt_ratio,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.new_sqale_debt_ratio],
      MetricKey.new_maintainability_rating,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.new_maintainability_rating],

      OVERALL_CATEGORY,
      MetricKey.software_quality_maintainability_issues,
      MetricKey.code_smells,
      MetricKey.sqale_index,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.sqale_index],
      MetricKey.sqale_debt_ratio,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.sqale_debt_ratio],
      MetricKey.sqale_rating,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.sqale_rating],
      MetricKey.effort_to_reach_maintainability_rating_a,
      SOFTWARE_QUALITY_RATING_METRICS_MAP[MetricKey.effort_to_reach_maintainability_rating_a],
    ],
  },

  Coverage: {
    categories: [NEW_CODE_CATEGORY, OVERALL_CATEGORY, 'tests_category'],
    order: [
      NEW_CODE_CATEGORY,
      MetricKey.new_coverage,
      MetricKey.new_lines_to_cover,
      MetricKey.new_uncovered_lines,
      MetricKey.new_line_coverage,
      MetricKey.new_conditions_to_cover,
      MetricKey.new_uncovered_conditions,
      MetricKey.new_branch_coverage,

      OVERALL_CATEGORY,
      MetricKey.coverage,
      MetricKey.lines_to_cover,
      MetricKey.uncovered_lines,
      MetricKey.line_coverage,
      MetricKey.conditions_to_cover,
      MetricKey.uncovered_conditions,
      MetricKey.branch_coverage,

      'tests_category',
      MetricKey.tests,
      MetricKey.test_errors,
      MetricKey.test_failures,
      MetricKey.skipped_tests,
      MetricKey.test_success_density,
      MetricKey.test_execution_time,
    ],
  },

  Duplications: {
    categories: [NEW_CODE_CATEGORY, OVERALL_CATEGORY],
    order: [
      NEW_CODE_CATEGORY,
      MetricKey.new_duplicated_lines_density,
      MetricKey.new_duplicated_lines,
      MetricKey.new_duplicated_blocks,

      OVERALL_CATEGORY,
      MetricKey.duplicated_lines_density,
      MetricKey.duplicated_lines,
      MetricKey.duplicated_blocks,
      MetricKey.duplicated_files,
    ],
  },

  Size: {
    order: [
      MetricKey.new_lines,

      MetricKey.ncloc,
      MetricKey.lines,
      MetricKey.statements,
      MetricKey.functions,
      MetricKey.classes,
      MetricKey.files,
      MetricKey.directories,
    ],
  },

  Complexity: {
    order: [
      MetricKey.complexity,
      MetricKey.function_complexity,
      MetricKey.file_complexity,
      MetricKey.class_complexity,
    ],
  },

  Releasability: {
    order: [MetricKey.releasability_rating, MetricKey.releasability_effort, MetricKey.alert_status],
  },

  Issues: {
    categories: [NEW_CODE_CATEGORY, OVERALL_CATEGORY],
    order: [
      NEW_CODE_CATEGORY,
      MetricKey.new_violations,
      MetricKey.new_accepted_issues,

      OVERALL_CATEGORY,
      MetricKey.violations,
      MetricKey.confirmed_issues,
      MetricKey.accepted_issues,
      MetricKey.false_positive_issues,
    ],
  },
};
