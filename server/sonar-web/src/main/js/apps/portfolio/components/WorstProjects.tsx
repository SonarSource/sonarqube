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
import * as React from 'react';
import { Link } from 'react-router';
import { max } from 'lodash';
import { SubComponent } from '../types';
import { Component } from '../../../app/types';
import Measure from '../../../components/measure/Measure';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import { getProjectUrl } from '../../../helpers/urls';

interface Props {
  component: Component;
  subComponents: SubComponent[];
  total: number;
}

export default function WorstProjects({ component, subComponents, total }: Props) {
  const count = subComponents.length;

  if (!count) {
    return null;
  }

  const maxLoc = max(
    subComponents.map(component => Number(component.measures['ncloc'] || 0))
  ) as number;

  const projectsPageUrl = { pathname: '/code', query: { id: component.key } };

  return (
    <div className="panel panel-white portfolio-sub-components" id="portfolio-sub-components">
      <table className="data zebra">
        <thead>
          <tr>
            <th>&nbsp;</th>
            <th className="text-center" style={{ width: 90 }}>
              {translate('metric_domain.Releasability')}
            </th>
            <th className="text-center" style={{ width: 90 }}>
              {translate('metric_domain.Reliability')}
            </th>
            <th className="text-center" style={{ width: 90 }}>
              {translate('metric_domain.Security')}
            </th>
            <th className="text-center" style={{ width: 90 }}>
              {translate('metric_domain.Maintainability')}
            </th>
            <th className="text-center" style={{ width: 90 }}>
              {translate('metric.ncloc.name')}
            </th>
          </tr>
        </thead>
        <tbody>
          {subComponents.map(component => (
            <tr key={component.key}>
              <td>
                <Link
                  to={getProjectUrl(component.refKey || component.key)}
                  className="link-with-icon">
                  <QualifierIcon qualifier={component.qualifier} /> {component.name}
                </Link>
              </td>
              {component.qualifier === 'TRK' ? (
                <td className="text-center">
                  <Measure
                    measure={{
                      metric: { key: 'alert_status', type: 'LEVEL' },
                      value: component.measures['alert_status']
                    }}
                  />
                </td>
              ) : (
                <td className="text-center">
                  <Measure
                    measure={{
                      metric: { key: 'releasability_rating', type: 'RATING' },
                      value: component.measures['releasability_rating']
                    }}
                  />
                </td>
              )}
              <td className="text-center">
                <Measure
                  measure={{
                    metric: { key: 'reliability_rating', type: 'RATING' },
                    value: component.measures['reliability_rating']
                  }}
                />
              </td>
              <td className="text-center">
                <Measure
                  measure={{
                    metric: { key: 'security_rating', type: 'RATING' },
                    value: component.measures['security_rating']
                  }}
                />
              </td>
              <td className="text-center">
                <Measure
                  measure={{
                    metric: { key: 'sqale_rating', type: 'RATING' },
                    value: component.measures['sqale_rating']
                  }}
                />
              </td>
              <td className="text-right">
                <span className="note">
                  <Measure
                    measure={{
                      metric: { key: 'ncloc', type: 'SHORT_INT' },
                      value: component.measures['ncloc']
                    }}
                  />
                </span>
                {maxLoc > 0 && (
                  <svg width="50" height="16" className="spacer-left">
                    <rect
                      className="bar-chart-bar"
                      x="0"
                      y="3"
                      width={getBarWidth(Number(component.measures['ncloc'] || 0), maxLoc, 50)}
                      height="10"
                    />
                  </svg>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {total > count && (
        <footer className="spacer-top note text-center">
          {translateWithParameters(
            'x_of_y_shown',
            formatMeasure(count, 'INT'),
            formatMeasure(total, 'INT')
          )}
          <Link to={projectsPageUrl} className="spacer-left">
            {translate('show_more')}
          </Link>
        </footer>
      )}
    </div>
  );
}

function getBarWidth(value: number, max: number, maxWidth: number): number {
  return Math.max(1, Math.round(value / max * maxWidth));
}
