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
  'lines': 'SHORT_INT'
};

export function formatMeasure (value, metric) {
  let type = METRIC_TYPES[metric];
  return type ? window.formatMeasure(value, type) : value;
}
