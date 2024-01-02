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
import classNames from 'classnames';
import { differenceBy, uniq } from 'lodash';
import * as React from 'react';
import { getMeasuresWithMetrics } from '../../../api/measures';
import { BranchStatusContextInterface } from '../../../app/components/branch-status/BranchStatusContext';
import withBranchStatus from '../../../app/components/branch-status/withBranchStatus';
import withBranchStatusActions from '../../../app/components/branch-status/withBranchStatusActions';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { Alert } from '../../../components/ui/Alert';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { BranchStatusData, PullRequest } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { Component, MeasureEnhanced } from '../../../types/types';
import IssueLabel from '../components/IssueLabel';
import IssueRating from '../components/IssueRating';
import MeasurementLabel from '../components/MeasurementLabel';
import QualityGateConditions from '../components/QualityGateConditions';
import SonarLintPromotion from '../components/SonarLintPromotion';
import '../styles.css';
import { MeasurementType, PR_METRICS } from '../utils';
import AfterMergeEstimate from './AfterMergeEstimate';
import LargeQualityGateBadge from './LargeQualityGateBadge';

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
        <div className="page page-limited">
          <i className="spinner" />
        </div>
      );
    }

    if (conditions === undefined) {
      return null;
    }

    const failedConditions = conditions
      .filter((condition) => condition.level === 'ERROR')
      .map((c) => enhanceConditionWithMeasure(c, measures))
      .filter(isDefined);

    return (
      <div className="page page-limited">
        <div
          className={classNames('pr-overview', {
            'has-conditions': failedConditions.length > 0,
          })}
        >
          {ignoredConditions && (
            <Alert className="big-spacer-bottom" display="inline" variant="info">
              <span className="text-middle">
                {translate('overview.quality_gate.ignored_conditions')}
              </span>
              <HelpTooltip
                className="spacer-left"
                overlay={translate('overview.quality_gate.ignored_conditions.tooltip')}
              />
            </Alert>
          )}
          <div className="display-flex-row">
            <div className="big-spacer-right">
              <h2 className="overview-panel-title spacer-bottom small display-inline-flex-center">
                {translate('overview.quality_gate')}
                <HelpTooltip
                  className="little-spacer-left"
                  overlay={
                    <div className="big-padded-top big-padded-bottom">
                      {translate('overview.quality_gate.help')}
                    </div>
                  }
                />
              </h2>
              <LargeQualityGateBadge component={component} level={status} />

              <SonarLintPromotion qgConditions={conditions} />
            </div>

            {failedConditions.length > 0 && (
              <div className="pr-overview-failed-conditions big-spacer-right">
                <h2 className="overview-panel-title spacer-bottom small">
                  {translate('overview.failed_conditions')}
                </h2>
                <QualityGateConditions
                  branchLike={branchLike}
                  collapsible={true}
                  component={component}
                  failedConditions={failedConditions}
                />
              </div>
            )}

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
      </div>
    );
  }
}

export default withBranchStatus(withBranchStatusActions(PullRequestOverview));
