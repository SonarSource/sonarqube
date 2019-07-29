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
import { differenceWith, map, sortBy, uniqBy } from 'lodash';
import * as React from 'react';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import ModalButton from 'sonar-ui-common/components/controls/ModalButton';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { getLocalizedMetricName, translate } from 'sonar-ui-common/helpers/l10n';
import { isDiffMetric } from 'sonar-ui-common/helpers/measures';
import DocTooltip from '../../../components/docs/DocTooltip';
import { withAppState } from '../../../components/hoc/withAppState';
import Condition from './Condition';
import ConditionModal from './ConditionModal';

interface Props {
  appState: Pick<T.AppState, 'branchesEnabled'>;
  canEdit: boolean;
  conditions: T.Condition[];
  metrics: T.Dict<T.Metric>;
  onAddCondition: (condition: T.Condition) => void;
  onRemoveCondition: (Condition: T.Condition) => void;
  onSaveCondition: (newCondition: T.Condition, oldCondition: T.Condition) => void;
  organization?: string;
  qualityGate: T.QualityGate;
}

const FORBIDDEN_METRIC_TYPES = ['DATA', 'DISTRIB', 'STRING', 'BOOL'];
const FORBIDDEN_METRICS = ['alert_status', 'releasability_rating', 'security_review_rating'];

export class Conditions extends React.PureComponent<Props> {
  renderConditionsTable = (conditions: T.Condition[], scope: 'new' | 'overall') => {
    const {
      qualityGate,
      metrics,
      canEdit,
      onRemoveCondition,
      onSaveCondition,
      organization
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
              organization={organization}
              qualityGate={qualityGate}
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
                  organization={this.props.organization}
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
          <DocTooltip
            className="spacer-left"
            doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/quality-gates/quality-gate-conditions.md')}
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

export default withAppState(Conditions);
