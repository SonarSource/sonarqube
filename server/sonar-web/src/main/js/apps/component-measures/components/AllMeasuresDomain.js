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
import React from 'react';
import { Link } from 'react-router';

import { formatLeak } from '../utils';
import { formatMeasure } from '../../../helpers/measures';
import { translateWithParameters } from '../../../helpers/l10n';

export default function AllMeasuresDomain ({ domain, component, displayLeakHeader, leakPeriodLabel }) {
  return (
      <li>
        <header className="page-header">
          <h3 className="page-title">{domain.name}</h3>
          {displayLeakHeader && (
              <div className="measures-domains-leak-header">
                {translateWithParameters('overview.leak_period_x', leakPeriodLabel)}
              </div>
          )}
        </header>

        <ul className="domain-measures">
          {domain.measures.map(measure => (
              <li key={measure.metric.key}>
                <div className="domain-measures-name">
                  {measure.metric.name}
                </div>
                <div className="domain-measures-value">
                  {measure.value != null && (
                      <Link to={{ pathname: measure.metric.key, query: { id: component.key } }}>
                        {formatMeasure(measure.value, measure.metric.type)}
                      </Link>
                  )}
                </div>
                <div className="domain-measures-value domain-measures-leak">
                  {measure.leak != null && (
                      <Link to={{ pathname: measure.metric.key, query: { id: component.key } }}>
                        {formatLeak(measure.leak, measure.metric)}
                      </Link>
                  )}
                </div>
              </li>
          ))}
        </ul>
      </li>
  );
}
