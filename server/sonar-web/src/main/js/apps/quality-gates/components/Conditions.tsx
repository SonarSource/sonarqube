/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { FormattedMessage } from 'react-intl';
import withAvailableFeatures, {
  WithAvailableFeaturesProps,
} from '../../../app/components/available-features/withAvailableFeatures';
import withMetricsContext from '../../../app/components/metrics/withMetricsContext';
import DocLink from '../../../components/common/DocLink';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import { Button } from '../../../components/controls/buttons';
import ModalButton, { ModalProps } from '../../../components/controls/ModalButton';
import { Alert } from '../../../components/ui/Alert';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { Feature } from '../../../types/features';
import { MetricKey } from '../../../types/metrics';
import {
  CaycStatus,
  Condition as ConditionType,
  Dict,
  Metric,
  QualityGate,
} from '../../../types/types';
import ConditionModal from './ConditionModal';
import CaycReviewUpdateConditionsModal from './ConditionReviewAndUpdateModal';
import ConditionsTable from './ConditionsTable';

interface Props extends WithAvailableFeaturesProps {
  metrics: Dict<Metric>;
  onAddCondition: (condition: ConditionType) => void;
  onRemoveCondition: (Condition: ConditionType) => void;
  onSaveCondition: (newCondition: ConditionType, oldCondition: ConditionType) => void;
  qualityGate: QualityGate;
  updatedConditionId?: string;
}

interface State {
  unlockEditing: boolean;
}

const FORBIDDEN_METRIC_TYPES = ['DATA', 'DISTRIB', 'STRING', 'BOOL'];
const FORBIDDEN_METRICS: string[] = [
  MetricKey.alert_status,
  MetricKey.releasability_rating,
  MetricKey.security_hotspots,
  MetricKey.new_security_hotspots,
];

export class Conditions extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      unlockEditing: props.qualityGate.caycStatus === CaycStatus.NonCompliant,
    };
  }

  componentDidUpdate(prevProps: Readonly<Props>): void {
    const { qualityGate } = this.props;
    if (prevProps.qualityGate.name !== qualityGate.name) {
      this.setState({ unlockEditing: qualityGate.caycStatus === CaycStatus.NonCompliant });
    }
  }

  unlockEditing = () => {
    this.setState({ unlockEditing: true });
  };

  lockEditing = () => {
    this.setState({ unlockEditing: false });
  };

  renderConditionModal = ({ onClose }: ModalProps) => {
    const { metrics, qualityGate } = this.props;
    const { conditions = [] } = qualityGate;
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

  renderCaycModal = ({ onClose }: ModalProps) => {
    const { qualityGate, metrics } = this.props;
    const { conditions = [] } = qualityGate;
    const canEdit = Boolean(qualityGate.actions?.manageConditions);
    return (
      <CaycReviewUpdateConditionsModal
        qualityGate={qualityGate}
        metrics={metrics}
        canEdit={canEdit}
        onRemoveCondition={this.props.onRemoveCondition}
        onSaveCondition={this.props.onSaveCondition}
        onAddCondition={this.props.onAddCondition}
        lockEditing={this.lockEditing}
        updatedConditionId={this.props.updatedConditionId}
        conditions={conditions}
        scope="new-cayc"
        onClose={onClose}
      />
    );
  };

  render() {
    const { qualityGate, metrics, onRemoveCondition, onSaveCondition, updatedConditionId } =
      this.props;
    const canEdit = Boolean(qualityGate.actions?.manageConditions);
    const { unlockEditing } = this.state;
    const { conditions = [] } = qualityGate;
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
        {qualityGate.caycStatus !== CaycStatus.NonCompliant && (
          <Alert className="big-spacer-top big-spacer-bottom cayc-success-banner" variant="success">
            <h4 className="spacer-bottom cayc-success-header">
              {translate('quality_gates.cayc.banner.title')}
            </h4>
            <div className="cayc-warning-description">
              <FormattedMessage
                id="quality_gates.cayc.banner.description1"
                defaultMessage={translate('quality_gates.cayc.banner.description1')}
                values={{
                  cayc_link: (
                    <DocLink to="/user-guide/clean-as-you-code/">
                      {translate('quality_gates.cayc')}
                    </DocLink>
                  ),
                }}
              />
              <br />
              {translate('quality_gates.cayc.banner.description2')}
            </div>
            <ul className="big-spacer-top big-spacer-left spacer-bottom cayc-warning-description">
              <li>{translate('quality_gates.cayc.banner.list_item1')}</li>
              <li>{translate('quality_gates.cayc.banner.list_item2')}</li>
              <li>{translate('quality_gates.cayc.banner.list_item3')}</li>
              <li>{translate('quality_gates.cayc.banner.list_item4')}</li>
              <li>{translate('quality_gates.cayc.banner.list_item5')}</li>
              <li>{translate('quality_gates.cayc.banner.list_item6')}</li>
            </ul>
          </Alert>
        )}

        {qualityGate.caycStatus === CaycStatus.OverCompliant && (
          <Alert className="big-spacer-top big-spacer-bottom cayc-success-banner" variant="info">
            <h4 className="spacer-bottom cayc-over-compliant-header">
              {translate('quality_gates.cayc_over_compliant.banner.title')}
            </h4>
            <div className="cayc-warning-description spacer-top">
              <FormattedMessage
                id="quality_gates.cayc_over_compliant.banner.description"
                defaultMessage={translate('quality_gates.cayc_over_compliant.banner.description')}
                values={{
                  link: (
                    <DocLink to="/user-guide/clean-as-you-code/#potential-drawbacks">
                      {translate('quality_gates.cayc_over_compliant.banner.link')}
                    </DocLink>
                  ),
                }}
              />
            </div>
          </Alert>
        )}

        {qualityGate.caycStatus === CaycStatus.NonCompliant && (
          <Alert className="big-spacer-top big-spacer-bottom" variant="warning">
            <h4 className="spacer-bottom cayc-warning-header">
              {translate('quality_gates.cayc_missing.banner.title')}
            </h4>
            <div className="cayc-warning-description spacer-bottom">
              <FormattedMessage
                id="quality_gates.cayc_missing.banner.description"
                defaultMessage={translate('quality_gates.cayc_missing.banner.description')}
                values={{
                  cayc_link: (
                    <DocLink to="/user-guide/clean-as-you-code/">
                      {translate('quality_gates.cayc')}
                    </DocLink>
                  ),
                }}
              />
            </div>
            {canEdit && (
              <ModalButton modal={this.renderCaycModal}>
                {({ onClick }) => (
                  <Button className="big-spacer-top spacer-bottom" onClick={onClick}>
                    {translate('quality_gates.cayc_condition.review_update')}
                  </Button>
                )}
              </ModalButton>
            )}
          </Alert>
        )}

        {(qualityGate.caycStatus === CaycStatus.NonCompliant || unlockEditing) && canEdit && (
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
            <ConditionsTable
              qualityGate={qualityGate}
              metrics={metrics}
              canEdit={canEdit}
              onRemoveCondition={onRemoveCondition}
              onSaveCondition={onSaveCondition}
              updatedConditionId={updatedConditionId}
              conditions={sortedConditionsOnNewMetrics}
              showEdit={this.state.unlockEditing}
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

        {qualityGate.caycStatus !== CaycStatus.NonCompliant && !unlockEditing && canEdit && (
          <div className="big-spacer-top big-spacer-bottom cayc-warning-description it__qg-unfollow-cayc">
            <p>
              <FormattedMessage
                id="quality_gates.cayc_unfollow.description"
                defaultMessage={translate('quality_gates.cayc_unfollow.description')}
                values={{
                  cayc_link: (
                    <DocLink to="/user-guide/clean-as-you-code/">
                      {translate('quality_gates.cayc')}
                    </DocLink>
                  ),
                }}
              />
            </p>
            <Button className="big-spacer-top spacer-bottom" onClick={this.unlockEditing}>
              {translate('quality_gates.cayc.unlock_edit')}
            </Button>
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
