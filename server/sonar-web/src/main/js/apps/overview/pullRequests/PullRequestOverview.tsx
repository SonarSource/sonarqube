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
  CoverageIndicator,
  DeferredSpinner,
  DuplicationsIndicator,
  HelperHintIcon,
  LargeCenteredLayout,
  Link,
  PageTitle,
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
import { duplicationRatingConverter } from '../../../components/measure/utils';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { enhanceConditionWithMeasure, enhanceMeasuresWithMetrics } from '../../../helpers/measures';
import { isDefined } from '../../../helpers/types';
import { getQualityGatesUrl, getQualityGateUrl } from '../../../helpers/urls';
import { BranchStatusData, PullRequest } from '../../../types/branch-like';
import { IssueType } from '../../../types/issues';
import { Component, MeasureEnhanced } from '../../../types/types';
import MeasuresPanelIssueMeasure from '../branches/MeasuresPanelIssueMeasure';
import MeasuresPanelPercentMeasure from '../branches/MeasuresPanelPercentMeasure';
import IgnoredConditionWarning from '../components/IgnoredConditionWarning';
import QualityGateConditions from '../components/QualityGateConditions';
import QualityGateStatusHeader from '../components/QualityGateStatusHeader';
import QualityGateStatusPassedView from '../components/QualityGateStatusPassedView';
import { QualityGateStatusTitle } from '../components/QualityGateStatusTitle';
import SonarLintPromotion from '../components/SonarLintPromotion';
import '../styles.css';
import { MeasurementType, PR_METRICS } from '../utils';

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
            <DeferredSpinner loading />
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
                          link: <Link to={path}>{translate('overview.quality_gate')}</Link>,
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
                      collapsible
                      component={component}
                      failedConditions={failedConditions}
                    />
                  </div>
                )}
              </Card>
              <SonarLintPromotion qgConditions={conditions} />
            </div>

            <div className="sw-flex-1">
              <h2 className="sw-body-md-highlight">
                <PageTitle text={translate('overview.measures')} />
              </h2>

              <div className="sw-grid sw-grid-cols-2 sw-gap-4 sw-mt-4">
                {[
                  IssueType.Bug,
                  IssueType.Vulnerability,
                  IssueType.SecurityHotspot,
                  IssueType.CodeSmell,
                ].map((type: IssueType) => (
                  <Card key={type} className="sw-p-8">
                    <MeasuresPanelIssueMeasure
                      branchLike={branchLike}
                      component={component}
                      isNewCodeTab
                      measures={measures}
                      type={type}
                    />
                  </Card>
                ))}

                {[MeasurementType.Coverage, MeasurementType.Duplication].map(
                  (type: MeasurementType) => (
                    <Card key={type} className="sw-p-8">
                      <MeasuresPanelPercentMeasure
                        branchLike={branchLike}
                        component={component}
                        measures={measures}
                        ratingIcon={renderMeasureIcon(type)}
                        type={type}
                        useDiffMetric
                      />
                    </Card>
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

function renderMeasureIcon(type: MeasurementType) {
  if (type === MeasurementType.Coverage) {
    return function CoverageIndicatorRenderer(value?: string) {
      return <CoverageIndicator value={value} size="md" />;
    };
  }

  return function renderDuplicationIcon(value?: string) {
    const rating = duplicationRatingConverter(Number(value));

    return <DuplicationsIndicator rating={rating} size="md" />;
  };
}
