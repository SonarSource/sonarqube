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
import * as React from 'react';
import * as classNames from 'classnames';
import { InjectedRouter } from 'react-router';
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
import { isDiffMetric, MeasureEnhanced } from '../../../helpers/measures';
import { isSameBranchLike, getBranchLikeQuery } from '../../../helpers/branches';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import {
  ComponentMeasure,
  ComponentMeasureEnhanced,
  BranchLike,
  Metric,
  Paging
} from '../../../app/types';
import { RequestData } from '../../../helpers/request';
import { Period } from '../../../helpers/periods';

interface Props {
  branchLike?: BranchLike;
  className?: string;
  component: ComponentMeasure;
  currentUser: { isLoggedIn: boolean };
  loading: boolean;
  leakPeriod?: Period;
  measure?: MeasureEnhanced;
  metric: Metric;
  metrics: { [metric: string]: Metric };
  rootComponent: ComponentMeasure;
  router: InjectedRouter;
  secondaryMeasure?: MeasureEnhanced;
  updateLoading: (param: { [key: string]: boolean }) => void;
  updateSelected: (component: string) => void;
  updateView: (view: string) => void;
  view: string;
}

interface State {
  bestValue?: string;
  components: ComponentMeasureEnhanced[];
  metric?: Metric;
  paging?: Paging;
  selected?: string;
  view?: string;
}

export default class MeasureContent extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
  mounted = false;
  state: State = { components: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponents(this.props);
  }

  componentWillReceiveProps(nextProps: Props) {
    if (
      !isSameBranchLike(nextProps.branchLike, this.props.branchLike) ||
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
    return index !== -1 ? index : undefined;
  };

  getComponentRequestParams = (view: string, metric: Metric, options: Object = {}) => {
    const strategy = view === 'list' ? 'leaves' : 'children';
    const metricKeys = [metric.key];
    const opts: RequestData = {
      ...getBranchLikeQuery(this.props.branchLike),
      additionalFields: 'metrics',
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

  fetchComponents = ({ component, metric, metrics, view }: Props) => {
    if (isFileType(component)) {
      this.setState({ metric: undefined, view: undefined });
      return;
    }

    const { metricKeys, opts, strategy } = this.getComponentRequestParams(view, metric);
    this.props.updateLoading({ components: true });
    getComponentTree(strategy, component.key, metricKeys, opts).then(
      r => {
        if (metric === this.props.metric) {
          if (this.mounted) {
            this.setState(({ selected }: State) => ({
              bestValue: r.metrics[0].bestValue,
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
              bestValue: r.metrics[0].bestValue,
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

  onOpenComponent = (componentKey: string) => {
    if (isViewType(this.props.rootComponent)) {
      const component = this.state.components.find(
        component => component.refKey === componentKey || component.key === componentKey
      );
      if (component && component.refKey !== undefined) {
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

  onSelectComponent = (componentKey: string) => this.setState({ selected: componentKey });

  renderCode() {
    return (
      <div className="measure-details-viewer">
        <CodeView
          branchLike={this.props.branchLike}
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
    if (metric !== undefined) {
      if (!view || ['list', 'tree'].includes(view)) {
        const selectedIdx = this.getSelectedIndex();
        return (
          <FilesView
            bestValue={this.state.bestValue}
            branchLike={this.props.branchLike}
            components={this.state.components}
            fetchMore={this.fetchMoreComponents}
            handleOpen={this.onOpenComponent}
            handleSelect={this.onSelectComponent}
            metric={metric}
            metrics={this.props.metrics}
            paging={this.state.paging}
            rootComponent={this.props.rootComponent}
            selectedIdx={selectedIdx}
            selectedKey={selectedIdx !== undefined ? this.state.selected : undefined}
          />
        );
      }

      if (view === 'treemap') {
        return (
          <TreeMapView
            branchLike={this.props.branchLike}
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
    const { branchLike, component, currentUser, measure, metric, rootComponent, view } = this.props;
    const isLoggedIn = currentUser && currentUser.isLoggedIn;
    const isFile = isFileType(component);
    const selectedIdx = this.getSelectedIndex();
    return (
      <div
        className={classNames('no-outline', this.props.className)}
        ref={container => (this.container = container)}>
        <div className="layout-page-header-panel layout-page-main-header">
          <div className="layout-page-header-panel-inner layout-page-main-header-inner">
            <div className="layout-page-main-inner">
              <Breadcrumbs
                backToFirst={view === 'list'}
                branchLike={branchLike}
                className="measure-breadcrumbs spacer-right text-ellipsis"
                component={component}
                handleSelect={this.onOpenComponent}
                rootComponent={rootComponent}
              />
              {component.key !== rootComponent.key &&
                isLoggedIn && (
                  <MeasureFavoriteContainer
                    branchLike={branchLike}
                    className="measure-favorite spacer-right"
                    component={component.key}
                  />
                )}
              {!isFile && (
                <MeasureViewSelect
                  className="measure-view-select"
                  handleViewChange={this.props.updateView}
                  metric={metric}
                  view={view}
                />
              )}
              <PageActions
                current={
                  selectedIdx !== undefined && view !== 'treemap' ? selectedIdx + 1 : undefined
                }
                isFile={isFile}
                paging={this.state.paging}
                totalLoadedComponents={this.state.components.length}
                view={view}
              />
            </div>
          </div>
        </div>
        {!metric && <MetricNotFound className="layout-page-main-inner measure-details-content" />}
        {metric && (
          <div className="layout-page-main-inner measure-details-content">
            <MeasureHeader
              branchLike={branchLike}
              component={component}
              components={this.state.components}
              leakPeriod={this.props.leakPeriod}
              // fall back to `undefined` to be compatible with typescript files where we compare with `=== undefined`
              measure={measure || undefined}
              metric={metric}
              secondaryMeasure={this.props.secondaryMeasure}
            />
            <DeferredSpinner loading={this.props.loading}>
              {isFileType(component) ? this.renderCode() : this.renderMeasure()}
            </DeferredSpinner>
          </div>
        )}
      </div>
    );
  }
}
