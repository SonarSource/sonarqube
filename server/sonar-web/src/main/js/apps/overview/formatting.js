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

  'ncloc': 'SHORT_INT',
  'classes': 'SHORT_INT',
  'lines': 'SHORT_INT',
  'generated_ncloc': 'SHORT_INT',
  'generated_lines': 'SHORT_INT',
  'directories': 'SHORT_INT',
  'files': 'SHORT_INT',
  'functions': 'SHORT_INT',
  'statements': 'SHORT_INT',
  'public_api': 'SHORT_INT',

  'complexity': 'SHORT_INT',
  'class_complexity': 'SHORT_INT',
  'file_complexity': 'SHORT_INT',
  'function_complexity': 'SHORT_INT',

  'comment_lines_density': 'PERCENT',
  'comment_lines': 'SHORT_INT',
  'commented_out_code_lines': 'SHORT_INT',
  'public_documented_api_density': 'PERCENT',
  'public_undocumented_api': 'SHORT_INT'
};

export function formatMeasure (value, metric) {
  let type = METRIC_TYPES[metric];
  return type ? window.formatMeasure(value, type) : value;
}
