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
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import PageActions from 'sonar-ui-common/components/ui/PageActions';
import { getComponentLeaves } from '../../../api/components';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import { getBranchLikeQuery, isSameBranchLike } from '../../../helpers/branches';
import BubbleChart from '../drilldown/BubbleChart';
import { enhanceComponent, getBubbleMetrics, hasFullMeasures, isFileType } from '../utils';
import Breadcrumbs from './Breadcrumbs';
import LeakPeriodLegend from './LeakPeriodLegend';
import MeasureContentHeader from './MeasureContentHeader';

interface Props {
  branchLike?: T.BranchLike;
  className?: string;
  component: T.ComponentMeasure;
  domain: string;
  leakPeriod?: T.Period;
  loading: boolean;
  metrics: T.Dict<T.Metric>;
  onIssueChange?: (issue: T.Issue) => void;
  rootComponent: T.ComponentMeasure;
  updateLoading: (param: T.Dict<boolean>) => void;
  updateSelected: (component: string) => void;
}

interface State {
  components: T.ComponentMeasureEnhanced[];
  paging?: T.Paging;
}

const BUBBLES_LIMIT = 500;

export default class MeasureOverview extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { components: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponents();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.component !== this.props.component ||
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike) ||
      prevProps.metrics !== this.props.metrics ||
      prevProps.domain !== this.props.domain
    ) {
      this.fetchComponents();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchComponents = () => {
    const { branchLike, component, domain, metrics } = this.props;
    if (isFileType(component)) {
      this.setState({ components: [], paging: undefined });
      return;
    }
    const { x, y, size, colors } = getBubbleMetrics(domain, metrics);
    const metricsKey = [x.key, y.key, size.key];
    if (colors) {
      metricsKey.push(...colors.map(metric => metric.key));
    }
    const options = {
      ...getBranchLikeQuery(branchLike),
      s: 'metric',
      metricSort: size.key,
      asc: false,
      ps: BUBBLES_LIMIT
    };

    this.props.updateLoading({ bubbles: true });
    getComponentLeaves(component.key, metricsKey, options).then(
      r => {
        if (domain === this.props.domain) {
          if (this.mounted) {
            this.setState({
              components: r.components.map(component =>
                enhanceComponent(component, undefined, metrics)
              ),
              paging: r.paging
            });
          }
          this.props.updateLoading({ bubbles: false });
        }
      },
      () => this.props.updateLoading({ bubbles: false })
    );
  };

  renderContent() {
    const { branchLike, component } = this.props;
    if (isFileType(component)) {
      return (
        <div className="measure-details-viewer">
          <SourceViewer
            branchLike={branchLike}
            component={component.key}
            onIssueChange={this.props.onIssueChange}
          />
        </div>
      );
    }

    return (
      <BubbleChart
        component={this.props.component}
        components={this.state.components}
        domain={this.props.domain}
        metrics={this.props.metrics}
        updateSelected={this.props.updateSelected}
      />
    );
  }

  render() {
    const { branchLike, component, leakPeriod, rootComponent } = this.props;
    const { paging } = this.state;
    const displayLeak = hasFullMeasures(branchLike);
    return (
      <div className={this.props.className}>
        <div className="layout-page-header-panel layout-page-main-header">
          <A11ySkipTarget anchor="measures_main" />

          <div className="layout-page-header-panel-inner layout-page-main-header-inner">
            <div className="layout-page-main-inner">
              <MeasureContentHeader
                left={
                  <Breadcrumbs
                    backToFirst={true}
                    branchLike={branchLike}
                    className="text-ellipsis"
                    component={component}
                    handleSelect={this.props.updateSelected}
                    rootComponent={rootComponent}
                  />
                }
                right={
                  <PageActions
                    current={this.state.components.length}
                    total={paging && paging.total}
                  />
                }
              />
            </div>
          </div>
        </div>
        <div className="layout-page-main-inner measure-details-content">
          <div className="clearfix big-spacer-bottom">
            {leakPeriod && displayLeak && (
              <LeakPeriodLegend className="pull-right" component={component} period={leakPeriod} />
            )}
          </div>
          <DeferredSpinner loading={this.props.loading} />
          {!this.props.loading && this.renderContent()}
        </div>
      </div>
    );
  }
}
