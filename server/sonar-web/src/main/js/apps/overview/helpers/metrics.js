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

export function filterMetrics (metrics) {
  return metrics.filter(metric => {
    return isNotHidden(metric) && hasSimpleType(metric) && isNotDifferential(metric);
  });
}

export function filterMetricsForDomains (metrics, domains) {
  return filterMetrics(metrics).filter(metric => hasRightDomain(metric, domains));
}


export function getShortType (type) {
  if (type === 'INT') {
    return 'SHORT_INT';
  } else if (type === 'WORK_DUR') {
    return 'SHORT_WORK_DUR';
  }
  return type;
}


export function getMetricName (metricKey) {
  return window.t('overview.metric', metricKey);
}
