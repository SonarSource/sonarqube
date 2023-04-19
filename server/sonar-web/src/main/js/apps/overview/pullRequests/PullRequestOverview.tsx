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
import {
  BasicSeparator,
  Card,
  DeferredSpinner,
  HelperHintIcon,
  LargeCenteredLayout,
  Link,
  TextMuted,
} from 'design-system';
import { differenceBy, uniq } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { getMeasuresWithMetrics } from '../../../api/measures';
import { BranchStatusContextInterface } from '../../../app/components/branch-status/BranchStatusContext';
import withBranchStatus from '../../../app/components/branch-status/withBranchStatus';
import withBranchStatusActions from '../../../app/components/branch-status/withBranchStatusActions';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { getQualityGateUrl, getQualityGatesUrl } from '../../../helpers/urls';
import { BranchStatusData, PullRequest } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { Component, MeasureEnhanced } from '../../../types/types';
import IgnoredConditionWarning from '../components/IgnoredConditionWarning';
import IssueLabel from '../components/IssueLabel';
import IssueRating from '../components/IssueRating';
import MeasurementLabel from '../components/MeasurementLabel';
import QualityGateConditions from '../components/QualityGateConditions';
import QualityGateStatusHeader from '../components/QualityGateStatusHeader';
import QualityGateStatusPassedView from '../components/QualityGateStatusPassedView';
import { QualityGateStatusTitle } from '../components/QualityGateStatusTitle';
import SonarLintPromotion from '../components/SonarLintPromotion';
import '../styles.css';
import { MeasurementType, PR_METRICS } from '../utils';
import AfterMergeEstimate from './AfterMergeEstimate';

interface Props extends BranchStatusData, Pick<BranchStatusContextInterface, 'fetchBranchStatus'> {
  branchLike: PullRequest;
  component: Component;
}

interface State {
  loading: boolean;
  measures: MeasureEnhanced[];
}

export class PullRequestOverview extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: false,
    measures: [],
  };

  componentDidMount() {
    this.mounted = true;
    if (this.props.conditions === undefined) {
      this.fetchBranchStatusData();
    } else {
      this.fetchBranchData();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (this.conditionsHaveChanged(prevProps)) {
      this.fetchBranchData();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  conditionsHaveChanged = (prevProps: Props) => {
    const prevConditions = prevProps.conditions ?? [];
    const newConditions = this.props.conditions ?? [];
    const diff = differenceBy(
      prevConditions.filter((c) => c.level === 'ERROR'),
      newConditions.filter((c) => c.level === 'ERROR'),
      (c) => c.metric
    );

    return (
      (prevProps.conditions === undefined && this.props.conditions !== undefined) || diff.length > 0
    );
  };

  fetchBranchStatusData = () => {
    const {
      branchLike,
      component: { key },
    } = this.props;
    this.props.fetchBranchStatus(branchLike, key);
  };

  fetchBranchData = () => {
    const {
      branchLike,
      component: { key },
      conditions,
    } = this.props;

    this.setState({ loading: true });

    const metricKeys =
      conditions !== undefined
        ? // Also load metrics that apply to failing QG conditions.
          uniq([...PR_METRICS, ...conditions.filter((c) => c.level !== 'OK').map((c) => c.metric)])
        : PR_METRICS;

    getMeasuresWithMetrics(key, metricKeys, getBranchLikeQuery(branchLike)).then(
      ({ component, metrics }) => {
        if (this.mounted && component.measures) {
          this.setState({
            loading: false,
            measures: enhanceMeasuresWithMetrics(component.measures || [], metrics),
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  render() {
    const { branchLike, component, conditions, ignoredConditions, status } = this.props;
    const { loading, measures } = this.state;

    if (loading) {
      return (
        <LargeCenteredLayout>
          <div className="sw-p-6">
            <DeferredSpinner loading={true} />
          </div>
        </LargeCenteredLayout>
      );
    }

    if (conditions === undefined) {
      return null;
    }

    const path =
      component.qualityGate === undefined
        ? getQualityGatesUrl()
        : getQualityGateUrl(component.qualityGate.name);

    const failedConditions = conditions
      .filter((condition) => condition.level === 'ERROR')
      .map((c) => enhanceConditionWithMeasure(c, measures))
      .filter(isDefined);

    return (
      <LargeCenteredLayout>
        <div className="it__pr-overview sw-mt-12">
          <div className="sw-flex">
            <div className="sw-flex sw-flex-col sw-mr-12 width-30">
              <QualityGateStatusTitle />
              <Card>
                {status && (
                  <QualityGateStatusHeader
                    status={status}
                    failedConditionCount={failedConditions.length}
                  />
                )}

                <div className="sw-flex sw-items-center sw-mb-4">
                  <TextMuted text={translate('overview.on_new_code_long')} />
                  <HelpTooltip
                    className="sw-ml-2"
                    overlay={
                      <FormattedMessage
                        defaultMessage={translate('overview.quality_gate.conditions_on_new_code')}
                        id="overview.quality_gate.conditions_on_new_code"
                        values={{
                          link: <Link to={path}>{translate('overview.quality_gate.status')}</Link>,
                        }}
                      />
                    }
                  >
                    <HelperHintIcon aria-label="help-tooltip" />
                  </HelpTooltip>
                </div>

                {ignoredConditions && <IgnoredConditionWarning />}

                {status === 'OK' && failedConditions.length === 0 && (
                  <QualityGateStatusPassedView />
                )}

                {status !== 'OK' && <BasicSeparator />}

                {failedConditions.length > 0 && (
                  <div>
                    <QualityGateConditions
                      branchLike={branchLike}
                      collapsible={true}
                      component={component}
                      failedConditions={failedConditions}
                    />
                  </div>
                )}
              </Card>
              <SonarLintPromotion qgConditions={conditions} />
            </div>

            <div className="flex-1">
              <h2 className="overview-panel-title spacer-bottom small">
                {translate('overview.measures')}
              </h2>

              <div className="overview-panel-content">
                {[
                  IssueType.Bug,
                  IssueType.Vulnerability,
                  IssueType.SecurityHotspot,
                  IssueType.CodeSmell,
                ].map((type: IssueType) => (
                  <div className="overview-measures-row display-flex-row" key={type}>
                    <div className="overview-panel-big-padded flex-1 small display-flex-center">
                      <IssueLabel
                        branchLike={branchLike}
                        component={component}
                        measures={measures}
                        type={type}
                        useDiffMetric={true}
                      />
                    </div>
                    <div className="overview-panel-big-padded overview-measures-aside display-flex-center">
                      <IssueRating
                        branchLike={branchLike}
                        component={component}
                        measures={measures}
                        type={type}
                        useDiffMetric={true}
                      />
                    </div>
                  </div>
                ))}

                {[MeasurementType.Coverage, MeasurementType.Duplication].map(
                  (type: MeasurementType) => (
                    <div className="overview-measures-row display-flex-row" key={type}>
                      <div className="overview-panel-big-padded flex-1 small display-flex-center">
                        <MeasurementLabel
                          branchLike={branchLike}
                          component={component}
                          measures={measures}
                          type={type}
                          useDiffMetric={true}
                        />
                      </div>

                      <AfterMergeEstimate
                        className="overview-panel-big-padded overview-measures-aside text-right overview-measures-emphasis"
                        measures={measures}
                        type={type}
                      />
                    </div>
                  )
                )}
              </div>
            </div>
          </div>
        </div>
      </LargeCenteredLayout>
    );
  }
}

export default withBranchStatus(withBranchStatusActions(PullRequestOverview));
