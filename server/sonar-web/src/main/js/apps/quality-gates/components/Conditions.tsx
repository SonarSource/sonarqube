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
import { differenceWith, map, sortBy, uniqBy } from 'lodash';
import AddConditionSelect from './AddConditionSelect';
import Condition from './Condition';
import DocTooltip from '../../../components/docs/DocTooltip';
import { translate, getLocalizedMetricName } from '../../../helpers/l10n';
import { Condition as ICondition, Metric, QualityGate } from '../../../app/types';
import { parseError } from '../../../helpers/request';

interface Props {
  canEdit: boolean;
  conditions: ICondition[];
  metrics: { [key: string]: Metric };
  onAddCondition: (metric: string) => void;
  onSaveCondition: (newCondition: ICondition, oldCondition: ICondition) => void;
  onRemoveCondition: (Condition: ICondition) => void;
  organization?: string;
  qualityGate: QualityGate;
}

interface State {
  error?: string;
}

export default class Conditions extends React.PureComponent<Props, State> {
  state: State = {};

  componentWillUpdate(nextProps: Props) {
    if (nextProps.qualityGate !== this.props.qualityGate) {
      this.setState({ error: undefined });
    }
  }

  getConditionKey = (condition: ICondition, index: number) => {
    return condition.id ? condition.id : `new-${index}`;
  };

  handleError = (error: any) => {
    parseError(error).then(
      message => {
        this.setState({ error: message });
      },
      () => {}
    );
  };

  handleResetError = () => {
    this.setState({ error: undefined });
  };

  render() {
    const { qualityGate, conditions, metrics, canEdit, organization } = this.props;

    const existingConditions = conditions.filter(condition => metrics[condition.metric]);

    const sortedConditions = sortBy(
      existingConditions,
      condition => metrics[condition.metric] && metrics[condition.metric].name
    );

    const duplicates: ICondition[] = [];
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

    const availableMetrics = differenceWith(
      map(metrics, metric => metric).filter(
        metric => !metric.hidden && !['DATA', 'DISTRIB'].includes(metric.type)
      ),
      conditions,
      (metric, condition) => metric.key === condition.metric
    );

    return (
      <div className="quality-gate-section" id="quality-gate-conditions">
        <header className="display-flex-center spacer-bottom">
          <h3>{translate('quality_gates.conditions')}</h3>
          <DocTooltip className="spacer-left" doc="quality-gates/quality-gate-conditions" />
        </header>

        <div className="big-spacer-bottom">{translate('quality_gates.introduction')}</div>

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
          <table className="data zebra zebra-hover" id="quality-gate-conditions">
            <thead>
              <tr>
                <th className="nowrap">{translate('quality_gates.conditions.metric')}</th>
                <th className="thin nowrap">{translate('quality_gates.conditions.leak')}</th>
                <th className="thin nowrap">{translate('quality_gates.conditions.operator')}</th>
                <th className="thin nowrap">{translate('quality_gates.conditions.warning')}</th>
                <th className="thin nowrap">{translate('quality_gates.conditions.error')}</th>
                {canEdit && <th />}
              </tr>
            </thead>
            <tbody>
              {sortedConditions.map((condition, index) => (
                <Condition
                  canEdit={canEdit}
                  condition={condition}
                  key={this.getConditionKey(condition, index)}
                  metric={metrics[condition.metric]}
                  onAddCondition={this.props.onAddCondition}
                  onError={this.handleError}
                  onRemoveCondition={this.props.onRemoveCondition}
                  onResetError={this.handleResetError}
                  onSaveCondition={this.props.onSaveCondition}
                  organization={organization}
                  qualityGate={qualityGate}
                />
              ))}
            </tbody>
          </table>
        ) : (
          <div className="big-spacer-top">{translate('quality_gates.no_conditions')}</div>
        )}

        {canEdit && (
          <AddConditionSelect
            metrics={availableMetrics}
            onAddCondition={this.props.onAddCondition}
          />
        )}
      </div>
    );
  }
}
