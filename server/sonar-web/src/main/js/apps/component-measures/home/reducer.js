/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { groupBy, partition, sortBy, toPairs } from 'lodash';
import { RECEIVE_MEASURES } from './actions';
import { getLocalizedMetricName } from '../../../helpers/l10n';

const initialState = {
  measures: undefined,
  domains: undefined,
  periods: undefined
};

function groupByDomains(measures) {
  const KNOWN_DOMAINS = [
    'Releasability',
    'Reliability',
    'Security',
    'Maintainability',
    'Coverage',
    'Duplications',
    'Size',
    'Complexity'
  ];

  const domains = sortBy(
    toPairs(groupBy(measures, measure => measure.metric.domain)).map(r => {
      const [name, measures] = r;
      const sortedMeasures = sortBy(measures, measure => getLocalizedMetricName(measure.metric));

      return { name, measures: sortedMeasures };
    }),
    'name'
  );
  const [knownDomains, unknownDomains] = partition(domains, domain =>
    KNOWN_DOMAINS.includes(domain.name)
  );
  return [
    ...sortBy(knownDomains, domain => KNOWN_DOMAINS.indexOf(domain.name)),
    ...sortBy(unknownDomains, domain => domain.name)
  ];
}

export default function(state = initialState, action = {}) {
  switch (action.type) {
    case RECEIVE_MEASURES:
      return {
        ...state,
        measures: action.measures,
        domains: groupByDomains(action.measures),
        periods: action.periods
      };
    default:
      return state;
  }
}
