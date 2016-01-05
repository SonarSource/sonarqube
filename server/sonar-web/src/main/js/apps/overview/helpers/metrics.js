/*
 * SonarQube :: Web
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
