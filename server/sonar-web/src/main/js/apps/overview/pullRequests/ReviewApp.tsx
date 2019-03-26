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
import * as classNames from 'classnames';
import { connect } from 'react-redux';
import AfterMergeEstimate from './AfterMergeEstimate';
import LargeQualityGateBadge from './LargeQualityGateBadge';
import IssueLabel from './IssueLabel';
import IssueRating from './IssueRating';
import MeasurementLabel from './MeasurementLabel';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import DocTooltip from '../../../components/docs/DocTooltip';
import QualityGateConditions from '../qualityGate/QualityGateConditions';
import { getMeasures } from '../../../api/measures';
import { getQualityGateProjectStatus } from '../../../api/quality-gates';
import { PR_METRICS, IssueType, MeasurementType } from '../utils';
import { getBranchLikeQuery, isSameBranchLike } from '../../../helpers/branches';
import { extractStatusConditionsFromProjectStatus } from '../../../helpers/qualityGates';
import { registerBranchStatus } from '../../../store/rootActions';
import { translate } from '../../../helpers/l10n';
import '../styles.css';

interface Props {
  branchLike: T.PullRequest | T.ShortLivingBranch;
  component: T.Component;
  registerBranchStatus: (branchLike: T.BranchLike, component: string, status: T.Status) => void;
}

interface State {
  conditions: T.QualityGateStatusCondition[];
  loading: boolean;
  measures: T.Measure[];
  status?: T.Status;
}

export class ReviewApp extends React.Component<Props, State> {
  mounted = false;

  state: State = {
    conditions: [],
    loading: false,
    measures: []
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchBranchData();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      this.props.component.key !== prevProps.component.key ||
      !isSameBranchLike(this.props.branchLike, prevProps.branchLike)
    ) {
      this.fetchBranchData();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchBranchData = () => {
    const { branchLike, component } = this.props;

    this.setState({ loading: true });

    const data = { projectKey: component.key, ...getBranchLikeQuery(branchLike) };

    Promise.all([
      getMeasures({
        component: component.key,
        metricKeys: PR_METRICS.join(),
        ...getBranchLikeQuery(branchLike)
      }),
      getQualityGateProjectStatus(data)
    ]).then(
      ([measures, projectStatus]) => {
        if (this.mounted && measures && projectStatus) {
          const { status } = projectStatus;
          this.setState({
            conditions: extractStatusConditionsFromProjectStatus(projectStatus),
            loading: false,
            measures,
            status
          });

          this.props.registerBranchStatus(branchLike, component.key, status);
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
    const { branchLike, component } = this.props;
    const { conditions = [], loading, measures, status } = this.state;
    const erroredConditions = conditions.filter(condition => condition.level === 'ERROR');

    return (
      <div className="page page-limited">
        {loading ? (
          <DeferredSpinner />
        ) : (
          <div
            className={classNames('pr-overview', {
              'has-conditions': erroredConditions.length > 0
            })}>
            <div className="pr-overview-quality-gate big-spacer-right">
              <h3 className="spacer-bottom small">
                {translate('overview.quality_gate')}
                <DocTooltip
                  className="spacer-left"
                  doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/quality-gates/project-homepage-quality-gate.md')}
                />
              </h3>
              <LargeQualityGateBadge component={component} level={status} />
            </div>

            {erroredConditions.length > 0 && (
              <div className="pr-overview-failed-conditions big-spacer-right">
                <h3 className="spacer-bottom small">{translate('overview.failed_conditions')}</h3>
                <QualityGateConditions
                  branchLike={branchLike}
                  collapsible={true}
                  component={component}
                  conditions={erroredConditions}
                />
              </div>
            )}

            <div className="pr-overview-measurements flex-1">
              <h3 className="spacer-bottom small">{translate('overview.metrics')}</h3>

              {['BUG', 'VULNERABILITY', 'CODE_SMELL'].map((type: IssueType) => (
                <div className="pr-overview-measurements-row display-flex-row" key={type}>
                  <div className="pr-overview-measurements-value flex-1 small display-flex-center">
                    <IssueLabel
                      branchLike={branchLike}
                      className="overview-domain-measure-value"
                      component={component}
                      measures={measures}
                      type={type}
                    />
                  </div>
                  <div className="pr-overview-measurements-rating display-flex-center">
                    <IssueRating
                      branchLike={branchLike}
                      component={component}
                      measures={measures}
                      type={type}
                    />
                  </div>
                </div>
              ))}

              {['COVERAGE', 'DUPLICATION'].map((type: MeasurementType) => (
                <div className="pr-overview-measurements-row display-flex-row" key={type}>
                  <div className="pr-overview-measurements-value flex-1 small display-flex-center">
                    <MeasurementLabel
                      branchLike={branchLike}
                      className="overview-domain-measure-value"
                      component={component}
                      measures={measures}
                      type={type}
                    />
                  </div>
                  <div className="pr-overview-measurements-estimate display-flex-center">
                    <AfterMergeEstimate measures={measures} type={type} />
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    );
  }
}

const mapDispatchToProps = { registerBranchStatus };

export default connect(
  null,
  mapDispatchToProps
)(ReviewApp);
