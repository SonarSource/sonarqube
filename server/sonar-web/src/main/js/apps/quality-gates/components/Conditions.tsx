/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import { Button } from '../../../components/controls/buttons';
import ModalButton, { ModalProps } from '../../../components/controls/ModalButton';
import { Alert } from '../../../components/ui/Alert';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { Feature } from '../../../types/features';
import { MetricKey } from '../../../types/metrics';
import { Condition as ConditionType, Dict, Metric, QualityGate } from '../../../types/types';
import { getCaycConditions, getOthersConditions } from '../utils';
import CaycConditions from './CaycConditions';
import ConditionModal from './ConditionModal';
import ConditionsTable from './ConditionsTable';

interface Props extends WithAvailableFeaturesProps {
  canEdit: boolean;
  conditions: ConditionType[];
  metrics: Dict<Metric>;
  onAddCondition: (condition: ConditionType) => void;
  onRemoveCondition: (Condition: ConditionType) => void;
  onSaveCondition: (newCondition: ConditionType, oldCondition: ConditionType) => void;
  qualityGate: QualityGate;
  updatedConditionId?: string;
}

const FORBIDDEN_METRIC_TYPES = ['DATA', 'DISTRIB', 'STRING', 'BOOL'];
const FORBIDDEN_METRICS: string[] = [
  MetricKey.alert_status,
  MetricKey.releasability_rating,
  MetricKey.security_hotspots,
  MetricKey.new_security_hotspots,
];

export class Conditions extends React.PureComponent<Props> {
  renderConditionModal = ({ onClose }: ModalProps) => {
    const { metrics, qualityGate, conditions } = this.props;
    const availableMetrics = differenceWith(
      map(metrics, (metric) => metric).filter(
        (metric) =>
          !metric.hidden &&
          !FORBIDDEN_METRIC_TYPES.includes(metric.type) &&
          !FORBIDDEN_METRICS.includes(metric.key)
      ),
      conditions,
      (metric, condition) => metric.key === condition.metric
    );
    return (
      <ConditionModal
        header={translate('quality_gates.add_condition')}
        metrics={availableMetrics}
        onAddCondition={this.props.onAddCondition}
        onClose={onClose}
        qualityGate={qualityGate}
      />
    );
  };

  render() {
    const {
      qualityGate,
      metrics,
      canEdit,
      onAddCondition,
      onRemoveCondition,
      onSaveCondition,
      updatedConditionId,
      conditions,
    } = this.props;

    const existingConditions = conditions.filter((condition) => metrics[condition.metric]);
    const sortedConditions = sortBy(
      existingConditions,
      (condition) => metrics[condition.metric] && metrics[condition.metric].name
    );

    const sortedConditionsOnOverallMetrics = sortedConditions.filter(
      (condition) => !isDiffMetric(condition.metric)
    );
    const sortedConditionsOnNewMetrics = sortedConditions.filter((condition) =>
      isDiffMetric(condition.metric)
    );

    const duplicates: ConditionType[] = [];
    const savedConditions = existingConditions.filter((condition) => condition.id != null);
    savedConditions.forEach((condition) => {
      const sameCount = savedConditions.filter(
        (sample) => sample.metric === condition.metric
      ).length;
      if (sameCount > 1) {
        duplicates.push(condition);
      }
    });

    const uniqDuplicates = uniqBy(duplicates, (d) => d.metric).map((condition) => ({
      ...condition,
      metric: metrics[condition.metric],
    }));

    return (
      <div className="quality-gate-section">
        {canEdit && (
          <div className="pull-right">
            <ModalButton modal={this.renderConditionModal}>
              {({ onClick }) => (
                <Button data-test="quality-gates__add-condition" onClick={onClick}>
                  {translate('quality_gates.add_condition')}
                </Button>
              )}
            </ModalButton>
          </div>
        )}

        <header className="display-flex-center">
          <h2 className="big">{translate('quality_gates.conditions')}</h2>
          <DocumentationTooltip
            className="spacer-left"
            content={translate('quality_gates.conditions.help')}
            links={[
              {
                href: '/user-guide/clean-as-you-code/',
                label: translate('quality_gates.conditions.help.link'),
              },
            ]}
          />
        </header>

        {uniqDuplicates.length > 0 && (
          <Alert variant="warning">
            <p>{translate('quality_gates.duplicated_conditions')}</p>
            <ul className="list-styled spacer-top">
              {uniqDuplicates.map((d) => (
                <li key={d.metric.key}>{getLocalizedMetricName(d.metric)}</li>
              ))}
            </ul>
          </Alert>
        )}

        {sortedConditionsOnNewMetrics.length > 0 && (
          <div className="big-spacer-top">
            <h3 className="medium text-normal">
              {translate('quality_gates.conditions.new_code', 'long')}
            </h3>
            {this.props.hasFeature(Feature.BranchSupport) && (
              <p className="spacer-top spacer-bottom">
                {translate('quality_gates.conditions.new_code', 'description')}
              </p>
            )}

            <CaycConditions
              qualityGate={qualityGate}
              metrics={metrics}
              canEdit={canEdit}
              onRemoveCondition={onRemoveCondition}
              onAddCondition={onAddCondition}
              onSaveCondition={onSaveCondition}
              updatedConditionId={updatedConditionId}
              conditions={getCaycConditions(sortedConditionsOnNewMetrics)}
              scope="new-cayc"
            />

            <h4>{translate('quality_gates.other_conditions')}</h4>
            <ConditionsTable
              qualityGate={qualityGate}
              metrics={metrics}
              canEdit={canEdit}
              onRemoveCondition={onRemoveCondition}
              onSaveCondition={onSaveCondition}
              updatedConditionId={updatedConditionId}
              conditions={getOthersConditions(sortedConditionsOnNewMetrics)}
              scope="new"
            />
          </div>
        )}

        {sortedConditionsOnOverallMetrics.length > 0 && (
          <div className="big-spacer-top">
            <h3 className="medium text-normal">
              {translate('quality_gates.conditions.overall_code', 'long')}
            </h3>

            {this.props.hasFeature(Feature.BranchSupport) && (
              <p className="spacer-top spacer-bottom">
                {translate('quality_gates.conditions.overall_code', 'description')}
              </p>
            )}

            <ConditionsTable
              qualityGate={qualityGate}
              metrics={metrics}
              canEdit={canEdit}
              onRemoveCondition={onRemoveCondition}
              onSaveCondition={onSaveCondition}
              updatedConditionId={updatedConditionId}
              conditions={sortedConditionsOnOverallMetrics}
              scope="overall"
            />
          </div>
        )}

        {existingConditions.length === 0 && (
          <div className="big-spacer-top">{translate('quality_gates.no_conditions')}</div>
        )}
      </div>
    );
  }
}

export default withMetricsContext(withAvailableFeatures(Conditions));
