/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { differenceWith, map, sortBy, uniqBy } from 'lodash';
import * as React from 'react';
import withAppStateContext from '../../../app/components/app-state/withAppStateContext';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import { Button } from '../../../components/controls/buttons';
import ModalButton from '../../../components/controls/ModalButton';
import { Alert } from '../../../components/ui/Alert';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { MetricKey } from '../../../types/metrics';
import {
  AppState,
  Condition as ConditionType,
  Dict,
  Metric,
  QualityGate
} from '../../../types/types';
import Condition from './Condition';
import ConditionModal from './ConditionModal';

interface Props {
  appState: AppState;
  canEdit: boolean;
  conditions: ConditionType[];
  metrics: Dict<Metric>;
  onAddCondition: (condition: ConditionType) => void;
  onRemoveCondition: (Condition: ConditionType) => void;
  onSaveCondition: (newCondition: ConditionType, oldCondition: ConditionType) => void;
  qualityGate: QualityGate;
  updatedConditionId?: number;
}

const FORBIDDEN_METRIC_TYPES = ['DATA', 'DISTRIB', 'STRING', 'BOOL'];
const FORBIDDEN_METRICS: string[] = [
  MetricKey.alert_status,
  MetricKey.releasability_rating,
  MetricKey.security_hotspots,
  MetricKey.new_security_hotspots
];

export class Conditions extends React.PureComponent<Props> {
  renderConditionsTable = (conditions: ConditionType[], scope: 'new' | 'overall') => {
    const {
      qualityGate,
      metrics,
      canEdit,
      onRemoveCondition,
      onSaveCondition,
      updatedConditionId
    } = this.props;
    return (
      <table className="data zebra" data-test={`quality-gates__conditions-${scope}`}>
        <thead>
          <tr>
            <th className="nowrap" style={{ width: 300 }}>
              {translate('quality_gates.conditions.metric')}
            </th>
            <th className="nowrap">{translate('quality_gates.conditions.operator')}</th>
            <th className="nowrap">{translate('quality_gates.conditions.value')}</th>
            {canEdit && (
              <>
                <th className="thin">{translate('edit')}</th>
                <th className="thin">{translate('delete')}</th>
              </>
            )}
          </tr>
        </thead>
        <tbody>
          {conditions.map(condition => (
            <Condition
              canEdit={canEdit}
              condition={condition}
              key={condition.id}
              metric={metrics[condition.metric]}
              onRemoveCondition={onRemoveCondition}
              onSaveCondition={onSaveCondition}
              qualityGate={qualityGate}
              updated={condition.id === updatedConditionId}
            />
          ))}
        </tbody>
      </table>
    );
  };

  render() {
    const { appState, conditions, metrics, canEdit } = this.props;

    const existingConditions = conditions.filter(condition => metrics[condition.metric]);
    const sortedConditions = sortBy(
      existingConditions,
      condition => metrics[condition.metric] && metrics[condition.metric].name
    );

    const sortedConditionsOnOverallMetrics = sortedConditions.filter(
      condition => !isDiffMetric(condition.metric)
    );
    const sortedConditionsOnNewMetrics = sortedConditions.filter(condition =>
      isDiffMetric(condition.metric)
    );

    const duplicates: ConditionType[] = [];
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
      <div className="quality-gate-section">
        {canEdit && (
          <div className="pull-right">
            <ModalButton
              modal={({ onClose }) => (
                <ConditionModal
                  header={translate('quality_gates.add_condition')}
                  metrics={availableMetrics}
                  onAddCondition={this.props.onAddCondition}
                  onClose={onClose}
                  qualityGate={this.props.qualityGate}
                />
              )}>
              {({ onClick }) => (
                <Button data-test="quality-gates__add-condition" onClick={onClick}>
                  {translate('quality_gates.add_condition')}
                </Button>
              )}
            </ModalButton>
          </div>
        )}

        <header className="display-flex-center spacer-bottom">
          <h3>{translate('quality_gates.conditions')}</h3>
          <DocumentationTooltip
            className="spacer-left"
            content={translate('quality_gates.conditions.help')}
            links={[
              {
                href: '/documentation/user-guide/clean-as-you-code/',
                label: translate('quality_gates.conditions.help.link')
              }
            ]}
          />
        </header>

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

        {sortedConditionsOnNewMetrics.length > 0 && (
          <div className="big-spacer-top">
            <h4>{translate('quality_gates.conditions.new_code.long')}</h4>

            {appState.branchesEnabled && (
              <p className="spacer-top spacer-bottom">
                {translate('quality_gates.conditions.new_code.description')}
              </p>
            )}

            {this.renderConditionsTable(sortedConditionsOnNewMetrics, 'new')}
          </div>
        )}

        {sortedConditionsOnOverallMetrics.length > 0 && (
          <div className="big-spacer-top">
            <h4>{translate('quality_gates.conditions.overall_code.long')}</h4>

            {appState.branchesEnabled && (
              <p className="spacer-top spacer-bottom">
                {translate('quality_gates.conditions.overall_code.description')}
              </p>
            )}

            {this.renderConditionsTable(sortedConditionsOnOverallMetrics, 'overall')}
          </div>
        )}

        {existingConditions.length === 0 && (
          <div className="big-spacer-top">{translate('quality_gates.no_conditions')}</div>
        )}
      </div>
    );
  }
}

export default withMetricsContext(withAppStateContext(Conditions));
