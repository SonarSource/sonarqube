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
import * as classNames from 'classnames';
import * as React from 'react';
import { connect } from 'react-redux';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getMeasures } from '../../../api/measures';
import DocTooltip from '../../../components/docs/DocTooltip';
import { getBranchLikeQuery } from '../../../helpers/branches';
import { fetchBranchStatus } from '../../../store/rootActions';
import { getBranchStatusByBranchLike, Store } from '../../../store/rootReducer';
import QualityGateConditions from '../qualityGate/QualityGateConditions';
import '../styles.css';
import { IssueType, MeasurementType, PR_METRICS } from '../utils';
import AfterMergeEstimate from './AfterMergeEstimate';
import IssueLabel from './IssueLabel';
import IssueRating from './IssueRating';
import LargeQualityGateBadge from './LargeQualityGateBadge';
import MeasurementLabel from './MeasurementLabel';

interface OwnProps {
  branchLike: T.PullRequest | T.ShortLivingBranch;
  component: T.Component;
}

interface StateProps {
  conditions?: T.QualityGateStatusCondition[];
  ignoredConditions?: boolean;
  status?: T.Status;
}

interface DispatchProps {
  fetchBranchStatus: (branchLike: T.BranchLike, projectKey: string) => Promise<void>;
}

type Props = OwnProps & StateProps & DispatchProps;

interface State {
  loading: boolean;
  measures: T.Measure[];
}

export class ReviewApp extends React.PureComponent<Props, State> {
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
    const { branchLike, component } = this.props;

    this.setState({ loading: true });

    Promise.all([
      getMeasures({
        component: component.key,
        metricKeys: PR_METRICS.join(),
        ...getBranchLikeQuery(branchLike)
      }),
      this.props.fetchBranchStatus(branchLike, component.key)
    ]).then(
      ([measures]) => {
        if (this.mounted && measures) {
          this.setState({
            loading: false,
            measures
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

    if (loading || !conditions) {
      return (
        <div className="page page-limited">
          <i className="spinner" />
        </div>
      );
    }

    const erroredConditions = conditions.filter(condition => condition.level === 'ERROR');

    return (
      <div className="page page-limited">
        <div
          className={classNames('pr-overview', {
            'has-conditions': erroredConditions.length > 0
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
                  {type === 'VULNERABILITY' && (
                    <div className="pr-overview-measurements-value flex-1 small display-flex-center">
                      <IssueLabel
                        branchLike={branchLike}
                        className="overview-domain-measure-value"
                        component={component}
                        docTooltip={import(/* webpackMode: "eager" */ 'Docs/tooltips/metrics/security-hotspots.md')}
                        measures={measures}
                        type="SECURITY_HOTSPOT"
                      />
                    </div>
                  )}
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

                  <AfterMergeEstimate
                    className="pr-overview-measurements-estimate"
                    measures={measures}
                    type={type}
                  />
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state: Store, { branchLike, component }: OwnProps) => {
  const { conditions, ignoredConditions, status } = getBranchStatusByBranchLike(
    state,
    component.key,
    branchLike
  );
  return { conditions, ignoredConditions, status };
};

const mapDispatchToProps = { fetchBranchStatus: fetchBranchStatus as any };

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ReviewApp);
