const METRIC_TYPES = {
  'violations': 'SHORT_INT',
  'blocker_violations': 'SHORT_INT',
  'critical_violations': 'SHORT_INT',
  'major_violations': 'SHORT_INT',
  'minor_violations': 'SHORT_INT',
  'info_violations': 'SHORT_INT',
  'confirmed_issues': 'SHORT_INT',
  'false_positive_issues': 'SHORT_INT',
  'open_issues': 'SHORT_INT',
  'reopened_issues': 'SHORT_INT',
  'sqale_index': 'SHORT_WORK_DUR',
  'sqale_debt_ratio': 'PERCENT',
  'sqale_rating': 'RATING',
  'lines': 'SHORT_INT',

  'coverage': 'PERCENT',
  'line_coverage': 'PERCENT',
  'branch_coverage': 'PERCENT',
  'lines_to_cover': 'SHORT_INT',
  'conditions_to_cover': 'SHORT_INT',
  'uncovered_lines': 'SHORT_INT',
  'uncovered_conditions': 'SHORT_INT',

  'it_coverage': 'PERCENT',
  'it_line_coverage': 'PERCENT',
  'it_branch_coverage': 'PERCENT',
  'it_lines_to_cover': 'SHORT_INT',
  'it_conditions_to_cover': 'SHORT_INT',
  'it_uncovered_lines': 'SHORT_INT',
  'it_uncovered_conditions': 'SHORT_INT',

  'overall_coverage': 'PERCENT',
  'overall_line_coverage': 'PERCENT',
  'overall_branch_coverage': 'PERCENT',
  'overall_lines_to_cover': 'SHORT_INT',
  'overall_conditions_to_cover': 'SHORT_INT',
  'overall_uncovered_lines': 'SHORT_INT',
  'overall_uncovered_conditions': 'SHORT_INT',

  'tests': 'SHORT_INT',
  'skipped_tests': 'SHORT_INT',
  'test_errors': 'SHORT_INT',
  'test_failures': 'SHORT_INT',
  'test_execution_time': 'MILLISEC',
  'test_success_density': 'PERCENT',

  'duplicated_blocks': 'INT',
  'duplicated_files': 'INT',
  'duplicated_lines': 'INT',
  'duplicated_lines_density': 'PERCENT',

  'complexity': 'INT'
};

export function formatMeasure (value, metric) {
  let type = METRIC_TYPES[metric];
  return type ? window.formatMeasure(value, type) : value;
}
