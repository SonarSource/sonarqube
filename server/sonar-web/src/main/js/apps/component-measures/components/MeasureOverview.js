/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
// @flow
import React from 'react';
import Breadcrumbs from './Breadcrumbs';
import LeakPeriodLegend from './LeakPeriodLegend';
import MeasureFavoriteContainer from './MeasureFavoriteContainer';
import PageActions from './PageActions';
import BubbleChart from '../drilldown/BubbleChart';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import { getComponentLeaves } from '../../../api/components';
import { enhanceComponent, getBubbleMetrics, isFileType } from '../utils';
/*:: import type { Component, ComponentEnhanced, Paging, Period } from '../types'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */

/*:: type Props = {|
  branch?: string,
  className?: string,
  component: Component,
  currentUser: { isLoggedIn: boolean },
  domain: string,
  leakPeriod: Period,
  loading: boolean,
  metrics: { [string]: Metric },
  rootComponent: Component,
  updateLoading: ({ [string]: boolean }) => void,
  updateSelected: string => void
|}; */

/*:: type State = {
  components: Array<ComponentEnhanced>,
  paging?: Paging
}; */

const BUBBLES_LIMIT = 500;

export default class MeasureOverview extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    components: [],
    paging: null
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponents(this.props);
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (
      nextProps.component !== this.props.component ||
      nextProps.metrics !== this.props.metrics ||
      nextProps.domain !== this.props.domain
    ) {
      this.fetchComponents(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchComponents = (props /*: Props */) => {
    const { branch, component, domain, metrics } = props;
    if (isFileType(component)) {
      return this.setState({ components: [], paging: null });
    }
    const { x, y, size, colors } = getBubbleMetrics(domain, metrics);
    const metricsKey = [x.key, y.key, size.key];
    if (colors) {
      metricsKey.push(colors.map(metric => metric.key));
    }
    const options = {
      branch,
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
              components: r.components.map(component => enhanceComponent(component, null, metrics)),
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
    const { branch, component } = this.props;
    if (isFileType(component)) {
      return (
        <div className="measure-details-viewer">
          <SourceViewer branch={branch} component={component.key} />
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
    const { branch, component, currentUser, leakPeriod, rootComponent } = this.props;
    const isLoggedIn = currentUser && currentUser.isLoggedIn;
    const isFile = isFileType(component);
    return (
      <div className={this.props.className}>
        <div className="layout-page-header-panel layout-page-main-header">
          <div className="layout-page-header-panel-inner layout-page-main-header-inner">
            <div className="layout-page-main-inner">
              <Breadcrumbs
                backToFirst={true}
                branch={branch}
                className="measure-breadcrumbs spacer-right text-ellipsis"
                component={component}
                handleSelect={this.props.updateSelected}
                rootComponent={rootComponent}
              />
              {component.key !== rootComponent.key &&
                isLoggedIn && (
                  <MeasureFavoriteContainer
                    component={component.key}
                    className="measure-favorite spacer-right"
                  />
                )}
              <PageActions
                current={this.state.components.length}
                loading={this.props.loading}
                isFile={isFile}
                paging={this.state.paging}
              />
            </div>
          </div>
        </div>
        <div className="layout-page-main-inner measure-details-content">
          <div className="clearfix big-spacer-bottom">
            {leakPeriod != null && (
              <LeakPeriodLegend className="pull-right" component={component} period={leakPeriod} />
            )}
          </div>
          {!this.props.loading && this.renderContent()}
        </div>
      </div>
    );
  }
}
