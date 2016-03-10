/*
 * SonarQube
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
import sortBy from 'lodash/sortBy';
import partition from 'lodash/partition';
import React from 'react';
import { Link } from 'react-router';

import domains from '../config/domains';
import { formatLeak } from '../utils';
import { formatMeasure } from '../../../helpers/measures';
import { translateWithParameters } from '../../../helpers/l10n';

export default function AllMeasuresDomain ({ domain, component, displayLeakHeader, leakPeriodLabel }) {
  const hasLeak = !!leakPeriodLabel;
  const { measures } = domain;
  const knownMetrics = domains[domain.name] || [];

  const [knownMeasures, otherMeasures] =
      partition(measures, measure => knownMetrics.indexOf(measure.metric.key) !== -1);

  const finalMeasures = [
    ...sortBy(knownMeasures, measure => knownMetrics.indexOf(measure.metric.key)),
    ...sortBy(otherMeasures, measure => measure.metric.name)
  ];

  return (
      <li>
        <header className="page-header">
          <h3 className="page-title">{domain.name}</h3>
          {displayLeakHeader && hasLeak && (
              <div className="measures-domains-leak-header">
                {translateWithParameters('overview.leak_period_x', leakPeriodLabel)}
              </div>
          )}
        </header>

        <ul className="domain-measures">
          {finalMeasures.map(measure => (
              <li key={measure.metric.key} id={`measure-${measure.metric.key}`}>
                <Link to={{ pathname: measure.metric.key, query: { id: component.key } }}>
                  <div className="domain-measures-name">
                    <span id={`measure-${measure.metric.key}-name`}>
                      {measure.metric.name}
                    </span>
                  </div>
                  <div className="domain-measures-value">
                    {measure.value != null && (
                        <span id={`measure-${measure.metric.key}-value`}>
                          {formatMeasure(measure.value, measure.metric.type)}
                        </span>
                    )}
                  </div>
                  {hasLeak && measure.leak != null && (
                      <div className="domain-measures-value domain-measures-leak">
                        <span id={`measure-${measure.metric.key}-leak`}>
                          {formatLeak(measure.leak, measure.metric)}
                        </span>
                      </div>
                  )}
                </Link>
              </li>
          ))}
        </ul>
      </li>
  );
}
