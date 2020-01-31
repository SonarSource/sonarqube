/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { isDefined } from 'sonar-ui-common/helpers/types';
import { getMeasuresAndMeta } from '../../../api/measures';
import DocTooltip from '../../../components/docs/DocTooltip';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { fetchBranchStatus } from '../../../store/rootActions';
import { getBranchStatusByBranchLike, Store } from '../../../store/rootReducer';
import { BranchLike, PullRequest } from '../../../types/branch-like';
import { QualityGateStatusCondition } from '../../../types/quality-gates';
import IssueLabel from '../components/IssueLabel';
import IssueRating from '../components/IssueRating';
import MeasurementLabel from '../components/MeasurementLabel';
import QualityGateConditions from '../components/QualityGateConditions';
import '../styles.css';
import { IssueType, MeasurementType, PR_METRICS } from '../utils';
import AfterMergeEstimate from './AfterMergeEstimate';
import LargeQualityGateBadge from './LargeQualityGateBadge';

interface Props {
  branchLike: PullRequest;
  component: T.Component;
  conditions?: QualityGateStatusCondition[];
  fetchBranchStatus: (branchLike: BranchLike, projectKey: string) => Promise<void>;
  ignoredConditions?: boolean;
  status?: T.Status;
}

interface State {
  loading: boolean;
  measures: T.MeasureEnhanced[];
}

export class PullRequestOverview extends React.PureComponent<Props, State> {
  mounted = false;

  state: State = {
    loading: false,
    measures: []
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchBranchData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchBranchData = () => {
    const {
      branchLike,
      component: { key }
    } = this.props;

    this.setState({ loading: true });

    Promise.all([
      getMeasuresAndMeta(key, PR_METRICS, {
        additionalFields: 'metrics',
        ...getBranchLikeQuery(branchLike)
      }),
      this.props.fetchBranchStatus(branchLike, key)
    ]).then(
      ([{ component, metrics }]) => {
        if (this.mounted && component.measures) {
          this.setState({
            loading: false,
            measures: enhanceMeasuresWithMetrics(component.measures || [], metrics || [])
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
      .filter(condition => condition.level === 'ERROR')
      .map(c => enhanceConditionWithMeasure(c, measures))
      .filter(isDefined);

    return (
      <div className="page page-limited">
        <div
          className={classNames('pr-overview', {
            'has-conditions': failedConditions.length > 0
          })}>
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
              <h2 className="overview-panel-title spacer-bottom small">
                {translate('overview.quality_gate')}
                <DocTooltip
                  className="spacer-left"
                  doc={import(
                    /* webpackMode: "eager" */ 'Docs/tooltips/quality-gates/project-homepage-quality-gate.md'
                  )}
                />
              </h2>
              <LargeQualityGateBadge component={component} level={status} />
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
                  IssueType.CodeSmell
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

const mapStateToProps = (state: Store, { branchLike, component }: Props) => {
  const { conditions, ignoredConditions, status } = getBranchStatusByBranchLike(
    state,
    component.key,
    branchLike
  );
  return { conditions, ignoredConditions, status };
};

const mapDispatchToProps = { fetchBranchStatus: fetchBranchStatus as any };

export default connect(mapStateToProps, mapDispatchToProps)(PullRequestOverview);
