function hasRightDomain (metric, domains) {
  return domains.indexOf(metric.domain) !== -1;
}

function isNotHidden (metric) {
  return !metric.hidden;
}

function hasSimpleType (metric) {
  return metric.type !== 'DATA' && metric.type !== 'DISTRIB';
}

function isNotDifferential (metric) {
  return metric.key.indexOf('new_') !== 0;
}

export function filterMetricsForDomains (metrics, domains) {
  return metrics.filter(metric => {
    return hasRightDomain(metric, domains) && isNotHidden(metric) && hasSimpleType(metric) && isNotDifferential(metric);
  });
}
