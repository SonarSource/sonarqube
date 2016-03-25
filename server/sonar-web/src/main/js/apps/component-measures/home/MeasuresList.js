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

import Measure from '../components/Measure';
import { formatLeak } from '../utils';

const MeasuresList = ({ measures, hasLeak, component }) => {
  return (
      <ul className="domain-measures">
        {measures.map(measure => (
            <li key={measure.metric.key} id={`measure-${measure.metric.key}`}>
              <Link to={{ pathname: `metric/${measure.metric.key}`, query: { id: component.key } }}>
                <div className="domain-measures-name">
                    <span id={`measure-${measure.metric.key}-name`}>
                      {measure.metric.name}
                    </span>
                </div>
                <div className="domain-measures-value">
                  {measure.value != null && (
                      <span id={`measure-${measure.metric.key}-value`}>
                        <Measure measure={measure}/>
                      </span>
                  )}
                </div>
                {hasLeak && measure.leak != null && measure.metric.type !== 'RATING' && (
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
  );
};

export default MeasuresList;
