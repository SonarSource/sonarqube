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
import classNames from 'classnames';
import Breadcrumbs from './Breadcrumbs';
import MeasureFavoriteContainer from './MeasureFavoriteContainer';
import MeasureHeader from './MeasureHeader';
import MeasureViewSelect from './MeasureViewSelect';
import MetricNotFound from './MetricNotFound';
import PageActions from './PageActions';
import FilesView from '../drilldown/FilesView';
import CodeView from '../drilldown/CodeView';
import TreeMapView from '../drilldown/TreeMapView';
import { getComponentTree } from '../../../api/components';
import { complementary } from '../config/complementary';
import { enhanceComponent, isFileType, isViewType } from '../utils';
import { getProjectUrl } from '../../../helpers/urls';
import { isDiffMetric } from '../../../helpers/measures';
/*:: import type { Component, ComponentEnhanced, Paging, Period } from '../types'; */
/*:: import type { MeasureEnhanced } from '../../../components/measure/types'; */
/*:: import type { Metric } from '../../../store/metrics/actions'; */

// Switching to the following type will make flow crash with :
// https://github.com/facebook/flow/issues/3147
// router: { push: ({ pathname: string, query?: RawQuery }) => void }
/*:: type Props = {|
  branch?: string,
  className?: string,
  component: Component,
  currentUser: { isLoggedIn: boolean },
  loading: boolean,
  leakPeriod?: Period,
  measure: ?MeasureEnhanced,
  metric: Metric,
  metrics: { [string]: Metric },
  rootComponent: Component,
  router: Object,
  secondaryMeasure: ?MeasureEnhanced,
  updateLoading: ({ [string]: boolean }) => void,
  updateSelected: string => void,
  updateView: string => void,
  view: string
|}; */

/*:: type State = {
  components: Array<ComponentEnhanced>,
  metric: ?Metric,
  paging?: Paging,
  selected: ?string,
  view: ?string
}; */

export default class MeasureContent extends React.PureComponent {
  /*:: container: HTMLElement; */
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    components: [],
    metric: null,
    paging: null,
    selected: null,
    view: null
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponents(this.props);
  }

  componentWillReceiveProps(nextProps /*: Props */) {
    if (
      nextProps.branch !== this.props.branch ||
      nextProps.component !== this.props.component ||
      nextProps.metric !== this.props.metric
    ) {
      this.fetchComponents(nextProps);
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getSelectedIndex = () => {
    const componentKey = isFileType(this.props.component)
      ? this.props.component.key
      : this.state.selected;
    const index = this.state.components.findIndex(component => component.key === componentKey);
    return index !== -1 ? index : null;
  };

  getComponentRequestParams = (
    view /*: string */,
    metric /*: Metric */,
    options /*: Object */ = {}
  ) => {
    const strategy = view === 'list' ? 'leaves' : 'children';
    const metricKeys = [metric.key];
    const opts /*: Object */ = {
      branch: this.props.branch,
      metricSortFilter: 'withMeasuresOnly'
    };
    const isDiff = isDiffMetric(metric.key);
    if (isDiff) {
      opts.metricPeriodSort = 1;
    }
    if (view === 'treemap') {
      const sizeMetric = isDiff ? 'new_lines' : 'ncloc';
      metricKeys.push(sizeMetric);
      opts.metricSort = sizeMetric;
      opts.s = isDiff ? 'metricPeriod' : 'metric';
      opts.asc = false;
    } else {
      metricKeys.push(...(complementary[metric.key] || []));
      opts.asc = metric.direction === 1;
      opts.ps = 100;
      opts.metricSort = metric.key;
      opts.s = isDiff ? 'metricPeriod' : 'metric';
    }
    return { metricKeys, opts: { ...opts, ...options }, strategy };
  };

  fetchComponents = ({ component, metric, metrics, view } /*: Props */) => {
    if (isFileType(component)) {
      return this.setState({ metric: null, view: null });
    }

    const { metricKeys, opts, strategy } = this.getComponentRequestParams(view, metric);
    this.props.updateLoading({ components: true });
    getComponentTree(strategy, component.key, metricKeys, opts).then(
      r => {
        if (metric === this.props.metric) {
          if (this.mounted) {
            this.setState(({ selected } /*: State */) => ({
              components: r.components.map(component =>
                enhanceComponent(component, metric, metrics)
              ),
              metric,
              paging: r.paging,
              selected:
                r.components.length > 0 && !r.components.find(c => c.key === selected)
                  ? r.components[0].key
                  : selected,
              view
            }));
          }
          this.props.updateLoading({ components: false });
        }
      },
      () => this.props.updateLoading({ components: false })
    );
  };

  fetchMoreComponents = () => {
    const { component, metric, metrics, view } = this.props;
    const { paging } = this.state;
    if (!paging) {
      return;
    }
    const { metricKeys, opts, strategy } = this.getComponentRequestParams(view, metric, {
      p: paging.pageIndex + 1
    });
    this.props.updateLoading({ components: true });
    getComponentTree(strategy, component.key, metricKeys, opts).then(
      r => {
        if (metric === this.props.metric) {
          if (this.mounted) {
            this.setState(state => ({
              components: [
                ...state.components,
                ...r.components.map(component => enhanceComponent(component, metric, metrics))
              ],
              metric,
              paging: r.paging,
              view
            }));
          }
          this.props.updateLoading({ components: false });
        }
      },
      () => this.props.updateLoading({ components: false })
    );
  };

  onOpenComponent = (componentKey /*: string */) => {
    if (isViewType(this.props.rootComponent)) {
      const component = this.state.components.find(
        component => component.refKey === componentKey || component.key === componentKey
      );
      if (component && component.refKey != null) {
        if (this.props.view === 'treemap') {
          this.props.router.push(getProjectUrl(componentKey));
        }
        return;
      }
    }
    this.setState({ selected: this.props.component.key });
    this.props.updateSelected(componentKey);
    if (this.container) {
      this.container.focus();
    }
  };

  onSelectComponent = (componentKey /*: string */) => this.setState({ selected: componentKey });

  renderCode() {
    return (
      <div className="measure-details-viewer">
        <CodeView
          branch={this.props.branch}
          component={this.props.component}
          components={this.state.components}
          leakPeriod={this.props.leakPeriod}
          metric={this.props.metric}
          selectedIdx={this.getSelectedIndex()}
          updateSelected={this.props.updateSelected}
        />
      </div>
    );
  }

  renderMeasure() {
    const { metric, view } = this.state;
    if (metric != null) {
      if (['list', 'tree'].includes(view)) {
        const selectedIdx = this.getSelectedIndex();
        return (
          <FilesView
            branch={this.props.branch}
            components={this.state.components}
            fetchMore={this.fetchMoreComponents}
            handleOpen={this.onOpenComponent}
            handleSelect={this.onSelectComponent}
            metric={metric}
            metrics={this.props.metrics}
            paging={this.state.paging}
            selectedKey={selectedIdx != null ? this.state.selected : null}
            selectedIdx={selectedIdx}
          />
        );
      }

      if (view === 'treemap') {
        return (
          <TreeMapView
            branch={this.props.branch}
            components={this.state.components}
            handleSelect={this.onOpenComponent}
            metric={metric}
          />
        );
      }
    }

    return null;
  }

  render() {
    const { branch, component, currentUser, measure, metric, rootComponent, view } = this.props;
    const isLoggedIn = currentUser && currentUser.isLoggedIn;
    const isFile = isFileType(component);
    const selectedIdx = this.getSelectedIndex();
    return (
      <div
        className={classNames('no-outline', this.props.className)}
        ref={container => (this.container = container)}
        tabIndex={0}>
        <div className="layout-page-header-panel layout-page-main-header">
          <div className="layout-page-header-panel-inner layout-page-main-header-inner">
            <div className="layout-page-main-inner">
              <Breadcrumbs
                backToFirst={view === 'list'}
                branch={branch}
                className="measure-breadcrumbs spacer-right text-ellipsis"
                component={component}
                handleSelect={this.onOpenComponent}
                rootComponent={rootComponent}
              />
              {component.key !== rootComponent.key &&
                isLoggedIn && (
                  <MeasureFavoriteContainer
                    component={component.key}
                    className="measure-favorite spacer-right"
                  />
                )}
              {!isFile && (
                <MeasureViewSelect
                  className="measure-view-select"
                  metric={metric}
                  handleViewChange={this.props.updateView}
                  view={view}
                />
              )}
              <PageActions
                current={selectedIdx != null && view !== 'treemap' ? selectedIdx + 1 : null}
                loading={this.props.loading}
                isFile={isFile}
                paging={this.state.paging}
                totalLoadedComponents={this.state.components.length}
                view={view}
              />
            </div>
          </div>
        </div>
        {metric == null && (
          <MetricNotFound className="layout-page-main-inner measure-details-content" />
        )}
        {metric != null &&
          measure != null && (
            <div className="layout-page-main-inner measure-details-content">
              <MeasureHeader
                branch={branch}
                component={component}
                components={this.state.components}
                leakPeriod={this.props.leakPeriod}
                measure={measure}
                secondaryMeasure={this.props.secondaryMeasure}
              />
              {isFileType(component) ? this.renderCode() : this.renderMeasure()}
            </div>
          )}
      </div>
    );
  }
}
