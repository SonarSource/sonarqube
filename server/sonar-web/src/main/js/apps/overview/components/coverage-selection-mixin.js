export const CoverageSelectionMixin = {
  getCoverageMetricPrefix (measures) {
    if (measures['coverage'] != null && measures['it_coverage'] != null && measures['overall_coverage'] != null) {
      return 'overall_';
    } else if (measures['coverage'] != null) {
      return '';
    } else {
      return 'it_';
    }
  }
};
