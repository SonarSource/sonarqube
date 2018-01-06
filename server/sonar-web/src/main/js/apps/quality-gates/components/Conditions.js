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
import React from 'react';
import { sortBy, uniqBy } from 'lodash';
import ConditionsAlert from './ConditionsAlert';
import AddConditionForm from './AddConditionForm';
import Condition from './Condition';
import { translate, getLocalizedMetricName } from '../../../helpers/l10n';

function getKey(condition, index) {
  return condition.id ? condition.id : `new-${index}`;
}

export default class Conditions extends React.PureComponent {
  state = {
    error: null
  };

  componentWillUpdate(nextProps) {
    if (nextProps.qualityGate !== this.props.qualityGate) {
      this.setState({ error: null });
    }
  }

  handleError(error) {
    try {
      error.response.json().then(r => {
        const message = r.errors.map(e => e.msg).join('. ');
        this.setState({ error: message });
      });
    } catch (ex) {
      this.setState({ error: translate('default_error_message') });
    }
  }

  handleResetError() {
    this.setState({ error: null });
  }

  render() {
    const {
      qualityGate,
      conditions,
      metrics,
      edit,
      onAddCondition,
      onSaveCondition,
      onDeleteCondition,
      organization
    } = this.props;

    const existingConditions = conditions.filter(condition => metrics[condition.metric]);

    const sortedConditions = sortBy(
      existingConditions,
      condition => metrics[condition.metric] && metrics[condition.metric].name
    );

    const duplicates = [];
    const savedConditions = existingConditions.filter(condition => condition.id != null);
    savedConditions.forEach(condition => {
      const sameCount = savedConditions.filter(
        sample => sample.metric === condition.metric && sample.period === condition.period
      ).length;
      if (sameCount > 1) {
        duplicates.push(condition);
      }
    });

    const uniqDuplicates = uniqBy(duplicates, d => d.metric).map(condition => ({
      ...condition,
      metric: metrics[condition.metric]
    }));
    return (
      <div id="quality-gate-conditions" className="quality-gate-section">
        <h3 className="spacer-bottom">{translate('quality_gates.conditions')}</h3>

        <ConditionsAlert />

        {this.state.error && <div className="alert alert-danger">{this.state.error}</div>}

        {uniqDuplicates.length > 0 && (
          <div className="alert alert-warning">
            <p>{translate('quality_gates.duplicated_conditions')}</p>
            <ul className="list-styled spacer-top">
              {uniqDuplicates.map(d => (
                <li key={d.metric.key}>{getLocalizedMetricName(d.metric)}</li>
              ))}
            </ul>
          </div>
        )}

        {sortedConditions.length ? (
          <table id="quality-gate-conditions" className="data zebra zebra-hover">
            <thead>
              <tr>
                <th className="nowrap">{translate('quality_gates.conditions.metric')}</th>
                <th className="thin nowrap">{translate('quality_gates.conditions.leak')}</th>
                <th className="thin nowrap">{translate('quality_gates.conditions.operator')}</th>
                <th className="thin nowrap">{translate('quality_gates.conditions.warning')}</th>
                <th className="thin nowrap">{translate('quality_gates.conditions.error')}</th>
                {edit && <th />}
              </tr>
            </thead>
            <tbody>
              {sortedConditions.map((condition, index) => (
                <Condition
                  key={getKey(condition, index)}
                  qualityGate={qualityGate}
                  condition={condition}
                  metric={metrics[condition.metric]}
                  edit={edit}
                  onSaveCondition={onSaveCondition}
                  onDeleteCondition={onDeleteCondition}
                  onError={this.handleError.bind(this)}
                  onResetError={this.handleResetError.bind(this)}
                  organization={organization}
                />
              ))}
            </tbody>
          </table>
        ) : (
          <div className="big-spacer-top">{translate('quality_gates.no_conditions')}</div>
        )}

        {edit && <AddConditionForm metrics={metrics} onSelect={onAddCondition} />}
      </div>
    );
  }
}
