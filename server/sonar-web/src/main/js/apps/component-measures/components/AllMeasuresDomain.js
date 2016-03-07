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
  const hasLeak = !!leakPeriodLabel;

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
          {domain.measures.map(measure => (
              <li key={measure.metric.key}>
                <Link to={{ pathname: measure.metric.key, query: { id: component.key } }}>
                  <div className="domain-measures-name">
                    <span>{measure.metric.name}</span>
                  </div>
                  <div className="domain-measures-value">
                    {measure.value != null && (
                        <span>
                          {formatMeasure(measure.value, measure.metric.type)}
                        </span>
                    )}
                  </div>
                  {hasLeak && (
                      <div className="domain-measures-value domain-measures-leak">
                        {measure.leak != null && (
                            <span>
                          {formatLeak(measure.leak, measure.metric)}
                        </span>
                        )}
                      </div>
                  )}
                </Link>
              </li>
          ))}
        </ul>
      </li>
  );
}
