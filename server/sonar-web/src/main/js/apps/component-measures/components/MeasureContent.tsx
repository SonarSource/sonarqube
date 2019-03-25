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
import { InjectedRouter } from 'react-router';
import Breadcrumbs from './Breadcrumbs';
import MeasureContentHeader from './MeasureContentHeader';
import MeasureHeader from './MeasureHeader';
import MeasureViewSelect from './MeasureViewSelect';
import PageActions from '../../../components/ui/PageActions';
import { complementary } from '../config/complementary';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import FilesView from '../drilldown/FilesView';
import TreeMapView from '../drilldown/TreeMapView';
import { Query, View, isFileType, enhanceComponent, isViewType } from '../utils';
import { getComponentTree } from '../../../api/components';
import { isSameBranchLike, getBranchLikeQuery } from '../../../helpers/branches';
import { isDiffMetric, getPeriodValue } from '../../../helpers/measures';
import { RequestData } from '../../../helpers/request';
import { getProjectUrl } from '../../../helpers/urls';
import { getMeasures } from '../../../api/measures';
import { translate } from '../../../helpers/l10n';

interface Props {
  branchLike?: T.BranchLike;
  leakPeriod?: T.Period;
  requestedMetric: Pick<T.Metric, 'key' | 'direction'>;
  metrics: T.Dict<T.Metric>;
  rootComponent: T.ComponentMeasure;
  router: InjectedRouter;
  selected?: string;
  updateQuery: (query: Partial<Query>) => void;
  view: View;
}

interface State {
  baseComponent?: T.ComponentMeasure;
  components: T.ComponentMeasureEnhanced[];
  loadingMoreComponents: boolean;
  measure?: T.Measure;
  metric?: T.Metric;
  paging?: T.Paging;
  secondaryMeasure?: T.Measure;
  selected?: string;
}

export default class MeasureContent extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
  mounted = false;
  state: State = {
    components: [],
    loadingMoreComponents: false
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchComponentTree();
  }

  componentDidUpdate(prevProps: Props) {
    const prevComponentKey = prevProps.selected || prevProps.rootComponent.key;
    const componentKey = this.props.selected || this.props.rootComponent.key;
    if (
      prevComponentKey !== componentKey ||
      !isSameBranchLike(prevProps.branchLike, this.props.branchLike) ||
      prevProps.requestedMetric !== this.props.requestedMetric ||
      prevProps.view !== this.props.view
    ) {
      this.fetchComponentTree();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchComponentTree = () => {
    const { metricKeys, opts, strategy } = this.getComponentRequestParams(
      this.props.view,
      this.props.requestedMetric
    );
    const componentKey = this.props.selected || this.props.rootComponent.key;
    const baseComponentMetrics = [this.props.requestedMetric.key];
    if (this.props.requestedMetric.key === 'ncloc') {
      baseComponentMetrics.push('ncloc_language_distribution');
    }
    Promise.all([
      getComponentTree(strategy, componentKey, metricKeys, opts),
      getMeasures({
        component: componentKey,
        metricKeys: baseComponentMetrics.join(),
        ...getBranchLikeQuery(this.props.branchLike)
      })
    ]).then(([tree, measures]) => {
      if (this.mounted) {
        const metric = tree.metrics.find(m => m.key === this.props.requestedMetric.key);
        const components = tree.components.map(component =>
          enhanceComponent(component, metric, this.props.metrics)
        );

        const measure = measures.find(measure => measure.metric === this.props.requestedMetric.key);
        const secondaryMeasure = measures.find(
          measure => measure.metric !== this.props.requestedMetric.key
        );

        this.setState(({ selected }) => ({
          baseComponent: tree.baseComponent,
          components,
          measure,
          metric,
          paging: tree.paging,
          secondaryMeasure,
          selected:
            components.length > 0 && components.find(c => c.key === selected) ? selected : undefined
        }));
      }
    });
  };

  fetchMoreComponents = () => {
    const { metrics, view } = this.props;
    const { baseComponent, metric, paging } = this.state;
    if (!baseComponent || !paging || !metric) {
      return;
    }
    const { metricKeys, opts, strategy } = this.getComponentRequestParams(view, metric, {
      p: paging.pageIndex + 1
    });
    this.setState({ loadingMoreComponents: true });
    getComponentTree(strategy, baseComponent.key, metricKeys, opts).then(
      r => {
        if (metric.key === this.props.requestedMetric.key) {
          if (this.mounted) {
            this.setState(state => ({
              components: [
                ...state.components,
                ...r.components.map(component => enhanceComponent(component, metric, metrics))
              ],
              loadingMoreComponents: false,
              paging: r.paging
            }));
          }
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loadingMoreComponents: false });
        }
      }
    );
  };

  getComponentRequestParams(
    view: View,
    metric: Pick<T.Metric, 'key' | 'direction'>,
    options: Object = {}
  ) {
    const strategy = view === 'list' ? 'leaves' : 'children';
    const metricKeys = [metric.key];
    const opts: RequestData = {
      ...getBranchLikeQuery(this.props.branchLike),
      additionalFields: 'metrics',
      ps: 500
    };

    const setMetricSort = () => {
      const isDiff = isDiffMetric(metric.key);
      opts.s = isDiff ? 'metricPeriod' : 'metric';
      opts.metricSortFilter = 'withMeasuresOnly';
      if (isDiff) {
        opts.metricPeriodSort = 1;
      }
    };

    const isDiff = isDiffMetric(metric.key);
    if (view === 'tree') {
      metricKeys.push(...(complementary[metric.key] || []));
      opts.asc = true;
      opts.s = 'qualifier,name';
    } else if (view === 'list') {
      metricKeys.push(...(complementary[metric.key] || []));
      opts.asc = metric.direction === 1;
      opts.metricSort = metric.key;
      setMetricSort();
    } else if (view === 'treemap') {
      const sizeMetric = isDiff ? 'new_lines' : 'ncloc';
      metricKeys.push(sizeMetric);
      opts.asc = false;
      opts.metricSort = sizeMetric;
      setMetricSort();
    }

    return { metricKeys, opts: { ...opts, ...options }, strategy };
  }

  updateSelected = (component: string) => {
    this.props.updateQuery({
      selected: component !== this.props.rootComponent.key ? component : undefined
    });
  };

  updateView = (view: View) => {
    this.props.updateQuery({ view });
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
    this.setState(state => ({ selected: state.baseComponent!.key }));
    this.updateSelected(componentKey);
    if (this.container) {
      this.container.focus();
    }
  };

  onSelectComponent = (componentKey: string) => {
    this.setState({ selected: componentKey });
  };

  getSelectedIndex = () => {
    const componentKey = isFileType(this.state.baseComponent!)
      ? this.state.baseComponent!.key
      : this.state.selected;
    const index = this.state.components.findIndex(component => component.key === componentKey);
    return index !== -1 ? index : undefined;
  };

  renderMeasure() {
    const { view } = this.props;
    const { metric } = this.state;
    if (!metric) {
      return null;
    }
    if (view === 'tree' || view === 'list') {
      const selectedIdx = this.getSelectedIndex();
      return (
        <FilesView
          branchLike={this.props.branchLike}
          components={this.state.components}
          defaultShowBestMeasures={view === 'tree'}
          fetchMore={this.fetchMoreComponents}
          handleOpen={this.onOpenComponent}
          handleSelect={this.onSelectComponent}
          loadingMore={this.state.loadingMoreComponents}
          metric={metric}
          metrics={this.props.metrics}
          paging={this.state.paging}
          rootComponent={this.props.rootComponent}
          selectedIdx={selectedIdx}
          selectedKey={selectedIdx !== undefined ? this.state.selected : undefined}
          view={view}
        />
      );
    } else {
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

  render() {
    const { branchLike, rootComponent, view } = this.props;
    const { baseComponent, measure, metric, paging, secondaryMeasure } = this.state;

    if (!baseComponent || !metric) {
      return null;
    }

    const measureValue =
      measure && (isDiffMetric(measure.metric) ? getPeriodValue(measure, 1) : measure.value);
    const isFile = isFileType(baseComponent);
    const selectedIdx = this.getSelectedIndex();

    return (
      <div className="layout-page-main no-outline" ref={container => (this.container = container)}>
        <A11ySkipTarget anchor="measures_main" />

        <div className="layout-page-header-panel layout-page-main-header">
          <div className="layout-page-header-panel-inner layout-page-main-header-inner">
            <div className="layout-page-main-inner">
              <MeasureContentHeader
                left={
                  <Breadcrumbs
                    backToFirst={view === 'list'}
                    branchLike={branchLike}
                    className="text-ellipsis flex-1"
                    component={baseComponent}
                    handleSelect={this.onOpenComponent}
                    rootComponent={rootComponent}
                  />
                }
                right={
                  <div className="display-flex-center">
                    {!isFile && metric && (
                      <>
                        <div>{translate('component_measures.view_as')}</div>
                        <MeasureViewSelect
                          className="measure-view-select spacer-left big-spacer-right"
                          handleViewChange={this.updateView}
                          metric={metric}
                          view={view}
                        />

                        <PageActions
                          current={
                            selectedIdx !== undefined && view !== 'treemap'
                              ? selectedIdx + 1
                              : undefined
                          }
                          showShortcuts={['list', 'tree'].includes(view)}
                          total={paging && paging.total}
                        />
                      </>
                    )}
                  </div>
                }
              />
            </div>
          </div>
        </div>

        <div className="layout-page-main-inner measure-details-content">
          <MeasureHeader
            branchLike={branchLike}
            component={baseComponent}
            leakPeriod={this.props.leakPeriod}
            measureValue={measureValue}
            metric={metric}
            secondaryMeasure={secondaryMeasure}
          />
          {isFile ? (
            <div className="measure-details-viewer">
              <SourceViewer branchLike={branchLike} component={baseComponent.key} />
            </div>
          ) : (
            this.renderMeasure()
          )}
        </div>
      </div>
    );
  }
}
