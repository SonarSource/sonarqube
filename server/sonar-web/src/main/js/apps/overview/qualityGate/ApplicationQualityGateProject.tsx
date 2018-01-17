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
import { Link } from 'react-router';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { formatMeasure, isDiffMetric } from '../../../helpers/measures';
import { getProjectUrl } from '../../../helpers/urls';
import './ApplicationQualityGateProject.css';
import { Metric } from '../../../app/types';
import { ApplicationProject, ConditionAnalysis } from '../../../api/quality-gates';

interface Props {
  metrics: { [key: string]: Metric };
  project: ApplicationProject;
}

export default class ApplicationQualityGateProject extends React.PureComponent<Props> {
  renderCondition = (condition: ConditionAnalysis) => {
    const metric = this.props.metrics[condition.metric];
    const metricName = getLocalizedMetricName(metric);
    const threshold = condition.errorThreshold || condition.warningThreshold;
    const isDiff = isDiffMetric(condition.metric);

    return (
      <li className={classNames({ 'is-on-leak': isDiff })} key={condition.metric}>
        <span className="text-limited">
          <strong>{formatMeasure(condition.value, metric.type)}</strong> {metricName}
          {!isDiff && condition.onLeak && ' ' + translate('quality_gates.conditions.leak')}
        </span>
        <span
          className={classNames('big-spacer-left', {
            'text-danger': condition.status === 'ERROR',
            'text-warning': condition.status === 'WARN'
          })}>
          {translate('quality_gates.operator', condition.comparator, 'short')}{' '}
          {formatMeasure(threshold, metric.type)}
        </span>
      </li>
    );
  };

  render() {
    const { project } = this.props;

    return (
      <Link
        className={classNames(
          'overview-quality-gate-condition',
          'overview-quality-gate-condition-' + project.status.toLowerCase()
        )}
        to={getProjectUrl(project.key)}>
        <div className="application-quality-gate-project">
          <h4>{project.name}</h4>
          <ul className="application-quality-gate-project-conditions">
            {project.conditions.filter(c => c.status !== 'OK').map(this.renderCondition)}
          </ul>
        </div>
      </Link>
    );
  }
}
