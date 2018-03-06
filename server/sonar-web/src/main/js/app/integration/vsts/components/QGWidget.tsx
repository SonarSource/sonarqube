/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import * as classNames from 'classnames';
import { MeasureComponent } from '../../../../api/measures';
import { Metric } from '../../../types';
import { getPathUrlAsString, getProjectUrl } from '../../../../helpers/urls';

interface Props {
  component: MeasureComponent;
  metrics: Metric[];
}

const QG_LEVELS: { [level: string]: string } = {
  ERROR: 'Failed',
  WARN: 'Warning',
  OK: 'Passed',
  NONE: 'None'
};

export default function QGWidget({ component, metrics }: Props) {
  const qgMetric = metrics && metrics.find(m => m.key === 'alert_status');
  const qgMeasure = component && component.measures.find(m => m.metric === 'alert_status');

  if (!qgMeasure || !qgMeasure.value) {
    return <p>Project Quality Gate not computed.</p>;
  }

  return (
    <div className={classNames('widget dark-widget clickable', 'level-' + qgMeasure.value)}>
      <a href={getPathUrlAsString(getProjectUrl(component.key))} target="_blank">
        <h2 className="title truncated-text-ellipsis">{component.name}</h2>
        <div className="big-value truncated-text-ellipsis">{QG_LEVELS[qgMeasure.value]}</div>
        <div className="footer truncated-text-ellipsis">
          {qgMetric ? qgMetric.name : 'Quality Gate'}
        </div>
      </a>
    </div>
  );
}
