/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Condition from './Condition';
import ConditionModal from './ConditionModal';
import DocTooltip from '../../../components/docs/DocTooltip';
import { translate, getLocalizedMetricName } from '../../../helpers/l10n';
import ModalButton from '../../../components/controls/ModalButton';
import { Button } from '../../../components/ui/buttons';
import { Alert } from '../../../components/ui/Alert';

interface Props {
  canEdit: boolean;
  conditions: T.Condition[];
  metrics: T.Dict<T.Metric>;
  onAddCondition: (condition: T.Condition) => void;
  onSaveCondition: (newCondition: T.Condition, oldCondition: T.Condition) => void;
  onRemoveCondition: (Condition: T.Condition) => void;
  organization?: string;
  qualityGate: T.QualityGate;
}

const FORBIDDEN_METRIC_TYPES = ['DATA', 'DISTRIB', 'STRING', 'BOOL'];
const FORBIDDEN_METRICS = ['alert_status', 'releasability_rating'];

export default class Conditions extends React.PureComponent<Props> {
  getConditionKey = (condition: T.Condition, index: number) => {
    return condition.id ? condition.id : `new-${index}`;
  };

  render() {
    const { qualityGate, conditions, metrics, canEdit, organization } = this.props;

    const existingConditions = conditions.filter(condition => metrics[condition.metric]);

    const sortedConditions = sortBy(
      existingConditions,
      condition => metrics[condition.metric] && metrics[condition.metric].name
    );

    const duplicates: T.Condition[] = [];
    const savedConditions = existingConditions.filter(condition => condition.id != null);
    savedConditions.forEach(condition => {
      const sameCount = savedConditions.filter(sample => sample.metric === condition.metric).length;
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
        metric =>
          !metric.hidden &&
          !FORBIDDEN_METRIC_TYPES.includes(metric.type) &&
          !FORBIDDEN_METRICS.includes(metric.key)
      ),
      conditions,
      (metric, condition) => metric.key === condition.metric
    );

    return (
      <div className="quality-gate-section" id="quality-gate-conditions">
        {canEdit && (
          <div className="pull-right">
            <ModalButton
              modal={({ onClose }) => (
                <ConditionModal
                  header={translate('quality_gates.add_condition')}
                  metrics={availableMetrics}
                  onAddCondition={this.props.onAddCondition}
                  onClose={onClose}
                  organization={this.props.organization}
                  qualityGate={this.props.qualityGate}
                />
              )}>
              {({ onClick }) => (
                <Button onClick={onClick}>{translate('quality_gates.add_condition')}</Button>
              )}
            </ModalButton>
          </div>
        )}
        <header className="display-flex-center spacer-bottom">
          <h3>{translate('quality_gates.conditions')}</h3>
          <DocTooltip
            className="spacer-left"
            doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/quality-gates/quality-gate-conditions.md')}
          />
        </header>

        <div className="big-spacer-bottom">{translate('quality_gates.introduction')}</div>

        {uniqDuplicates.length > 0 && (
          <Alert variant="warning">
            <p>{translate('quality_gates.duplicated_conditions')}</p>
            <ul className="list-styled spacer-top">
              {uniqDuplicates.map(d => (
                <li key={d.metric.key}>{getLocalizedMetricName(d.metric)}</li>
              ))}
            </ul>
          </Alert>
        )}

        {sortedConditions.length ? (
          <table className="data zebra zebra-hover" id="quality-gate-conditions">
            <thead>
              <tr>
                <th className="nowrap">
                  <div className="display-inline-flex-center">
                    {translate('quality_gates.conditions.metric')}
                    <DocTooltip
                      className="spacer-left"
                      doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/quality-gates/metric.md')}
                    />
                  </div>
                </th>
                <th className="thin nowrap">{translate('quality_gates.conditions.operator')}</th>
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
                  onRemoveCondition={this.props.onRemoveCondition}
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
      </div>
    );
  }
}
