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
import * as React from 'react';
import { getComponentLeaves } from '../../../api/components';
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import PageActions from '../../../components/ui/PageActions';
import { getBranchLikeQuery, isSameBranchLike } from '../../../helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import { isFile } from '../../../types/component';
import {
  ComponentMeasure,
  ComponentMeasureEnhanced,
  ComponentMeasureIntern,
  Dict,
  Issue,
  Metric,
  Paging,
  Period,
} from '../../../types/types';
import BubbleChart from '../drilldown/BubbleChart';
import { BUBBLES_FETCH_LIMIT, enhanceComponent, getBubbleMetrics, hasFullMeasures } from '../utils';
import Breadcrumbs from './Breadcrumbs';
import LeakPeriodLegend from './LeakPeriodLegend';
import MeasureContentHeader from './MeasureContentHeader';

interface Props {
  branchLike?: BranchLike;
  className?: string;
  component: ComponentMeasure;
  domain: string;
  leakPeriod?: Period;
  loading: boolean;
  metrics: Dict<Metric>;
  onIssueChange?: (issue: Issue) => void;
  rootComponent: ComponentMeasure;
  updateLoading: (param: Dict<boolean>) => void;
  updateSelected: (component: ComponentMeasureIntern) => void;
}

interface State {
  components: ComponentMeasureEnhanced[];
  paging?: Paging;
}

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
    if (isFile(component.qualifier)) {
      this.setState({ components: [], paging: undefined });
      return;
    }
    const { x, y, size, colors } = getBubbleMetrics(domain, metrics);
    const metricsKey = [x.key, y.key, size.key];
    if (colors) {
      metricsKey.push(...colors.map((metric) => metric.key));
    }
    const options = {
      ...getBranchLikeQuery(branchLike),
      s: 'metric',
      metricSort: size.key,
      asc: false,
      ps: BUBBLES_FETCH_LIMIT,
    };

    this.props.updateLoading({ bubbles: true });
    getComponentLeaves(component.key, metricsKey, options).then(
      (r) => {
        if (domain === this.props.domain) {
          if (this.mounted) {
            this.setState({
              components: r.components.map((c) => enhanceComponent(c, undefined, metrics)),
              paging: r.paging,
            });
          }
          this.props.updateLoading({ bubbles: false });
        }
      },
      () => this.props.updateLoading({ bubbles: false })
    );
  };

  renderContent() {
    const { branchLike, component, domain, metrics } = this.props;
    const { paging } = this.state;

    if (isFile(component.qualifier)) {
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
        componentKey={component.key}
        branchLike={branchLike}
        components={this.state.components}
        domain={domain}
        metrics={metrics}
        paging={paging}
        updateSelected={this.props.updateSelected}
      />
    );
  }

  render() {
    const { branchLike, className, component, leakPeriod, loading, rootComponent } = this.props;
    const displayLeak = hasFullMeasures(branchLike);
    return (
      <div className={className}>
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
                    componentQualifier={rootComponent.qualifier}
                    current={this.state.components.length}
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
          <DeferredSpinner loading={loading} />
          {!loading && this.renderContent()}
        </div>
      </div>
    );
  }
}
