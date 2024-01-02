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
import { getComponentTree } from '../../../api/components';
import { getMeasures } from '../../../api/measures';
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import { Router } from '../../../components/hoc/withRouter';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import PageActions from '../../../components/ui/PageActions';
import { getBranchLikeQuery, isSameBranchLike } from '../../../helpers/branch-like';
import { getComponentMeasureUniqueKey } from '../../../helpers/component';
import { translate } from '../../../helpers/l10n';
import { isDiffMetric } from '../../../helpers/measures';
import { RequestData } from '../../../helpers/request';
import { getProjectUrl } from '../../../helpers/urls';
import { BranchLike } from '../../../types/branch-like';
import { isFile, isView } from '../../../types/component';
import { MeasurePageView } from '../../../types/measures';
import { MetricKey } from '../../../types/metrics';
import {
  ComponentMeasure,
  ComponentMeasureEnhanced,
  ComponentMeasureIntern,
  Dict,
  Issue,
  Measure,
  Metric,
  Paging,
  Period,
} from '../../../types/types';
import { complementary } from '../config/complementary';
import FilesView from '../drilldown/FilesView';
import TreeMapView from '../drilldown/TreeMapView';
import { enhanceComponent, Query } from '../utils';
import Breadcrumbs from './Breadcrumbs';
import MeasureContentHeader from './MeasureContentHeader';
import MeasureHeader from './MeasureHeader';
import MeasureViewSelect from './MeasureViewSelect';

interface Props {
  branchLike?: BranchLike;
  leakPeriod?: Period;
  requestedMetric: Pick<Metric, 'key' | 'direction'>;
  metrics: Dict<Metric>;
  onIssueChange?: (issue: Issue) => void;
  rootComponent: ComponentMeasure;
  router: Router;
  selected?: string;
  asc?: boolean;
  updateQuery: (query: Partial<Query>) => void;
  view: MeasurePageView;
}

interface State {
  baseComponent?: ComponentMeasure;
  components: ComponentMeasureEnhanced[];
  loadingMoreComponents: boolean;
  measure?: Measure;
  metric?: Metric;
  paging?: Paging;
  secondaryMeasure?: Measure;
  selectedComponent?: ComponentMeasureIntern;
}

export default class MeasureContent extends React.PureComponent<Props, State> {
  container?: HTMLElement | null;
  mounted = false;
  state: State = {
    components: [],
    loadingMoreComponents: false,
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
    const { asc, branchLike, metrics, requestedMetric, rootComponent, selected, view } = this.props;
    // if asc is undefined we dont want to pass it inside options
    const { metricKeys, opts, strategy } = this.getComponentRequestParams(view, requestedMetric, {
      ...(asc !== undefined && { asc }),
    });
    const componentKey = selected || rootComponent.key;
    const baseComponentMetrics = [requestedMetric.key];
    if (requestedMetric.key === MetricKey.ncloc) {
      baseComponentMetrics.push('ncloc_language_distribution');
    }
    Promise.all([
      getComponentTree(strategy, componentKey, metricKeys, opts),
      getMeasures({
        component: componentKey,
        metricKeys: baseComponentMetrics.join(),
        ...getBranchLikeQuery(branchLike),
      }),
    ]).then(
      ([tree, measures]) => {
        if (this.mounted) {
          const metric = tree.metrics.find((m) => m.key === requestedMetric.key);
          if (metric !== undefined) {
            metric.direction = requestedMetric.direction;
          }

          const components = tree.components.map((component) =>
            enhanceComponent(component, metric, metrics)
          );

          const measure = measures.find((m) => m.metric === requestedMetric.key);
          const secondaryMeasure = measures.find((m) => m.metric !== requestedMetric.key);

          this.setState(({ selectedComponent }) => ({
            baseComponent: tree.baseComponent,
            components,
            measure,
            metric,
            paging: tree.paging,
            secondaryMeasure,
            selectedComponent:
              components.length > 0 &&
              components.find(
                (c) =>
                  getComponentMeasureUniqueKey(c) ===
                  getComponentMeasureUniqueKey(selectedComponent)
              )
                ? selectedComponent
                : undefined,
          }));
        }
      },
      () => {
        /* noop */
      }
    );
  };

  fetchMoreComponents = () => {
    const { metrics, view, asc } = this.props;
    const { baseComponent, metric, paging } = this.state;
    if (!baseComponent || !paging || !metric) {
      return;
    }
    // if asc is undefined we dont want to pass it inside options
    const { metricKeys, opts, strategy } = this.getComponentRequestParams(view, metric, {
      p: paging.pageIndex + 1,
      ...(asc !== undefined && { asc }),
    });
    this.setState({ loadingMoreComponents: true });
    getComponentTree(strategy, baseComponent.key, metricKeys, opts).then(
      (r) => {
        if (this.mounted && metric.key === this.props.requestedMetric.key) {
          this.setState((state) => ({
            components: [
              ...state.components,
              ...r.components.map((component) => enhanceComponent(component, metric, metrics)),
            ],
            loadingMoreComponents: false,
            paging: r.paging,
          }));
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
    view: MeasurePageView,
    metric: Pick<Metric, 'key' | 'direction'>,
    options: Object = {}
  ) {
    const strategy = view === 'list' ? 'leaves' : 'children';
    const metricKeys = [metric.key];
    const opts: RequestData = {
      ...getBranchLikeQuery(this.props.branchLike),
      additionalFields: 'metrics',
      ps: 500,
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
      selected: component !== this.props.rootComponent.key ? component : undefined,
    });
  };

  updateView = (view: MeasurePageView) => {
    this.props.updateQuery({ view });
  };

  onOpenComponent = (component: ComponentMeasureIntern) => {
    if (isView(this.props.rootComponent.qualifier)) {
      const comp = this.state.components.find(
        (c) =>
          c.refKey === component.key ||
          getComponentMeasureUniqueKey(c) === getComponentMeasureUniqueKey(component)
      );

      if (comp) {
        this.props.router.push(getProjectUrl(comp.refKey || comp.key, component.branch));
      }

      return;
    }

    this.setState((state) => ({ selectedComponent: state.baseComponent }));
    this.updateSelected(component.key);
    if (this.container) {
      this.container.focus();
    }
  };

  onSelectComponent = (component: ComponentMeasureIntern) => {
    this.setState({ selectedComponent: component });
  };

  getSelectedIndex = () => {
    const componentKey = isFile(this.state.baseComponent?.qualifier)
      ? getComponentMeasureUniqueKey(this.state.baseComponent)
      : getComponentMeasureUniqueKey(this.state.selectedComponent);
    const index = this.state.components.findIndex(
      (component) => getComponentMeasureUniqueKey(component) === componentKey
    );
    return index !== -1 ? index : undefined;
  };

  getDefaultShowBestMeasures() {
    const { asc, view } = this.props;
    if ((asc !== undefined && view === 'list') || view === 'tree') {
      return true;
    }
    return false;
  }

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
          defaultShowBestMeasures={this.getDefaultShowBestMeasures()}
          fetchMore={this.fetchMoreComponents}
          handleOpen={this.onOpenComponent}
          handleSelect={this.onSelectComponent}
          loadingMore={this.state.loadingMoreComponents}
          metric={metric}
          metrics={this.props.metrics}
          paging={this.state.paging}
          rootComponent={this.props.rootComponent}
          selectedIdx={selectedIdx}
          selectedComponent={
            selectedIdx !== undefined
              ? (this.state.selectedComponent as ComponentMeasureEnhanced)
              : undefined
          }
          view={view}
        />
      );
    }

    return (
      <TreeMapView
        components={this.state.components}
        handleSelect={this.onOpenComponent}
        metric={metric}
      />
    );
  }

  render() {
    const { branchLike, rootComponent, view } = this.props;
    const { baseComponent, measure, metric, paging, secondaryMeasure } = this.state;

    if (!baseComponent || !metric) {
      return null;
    }

    const measureValue =
      measure && (isDiffMetric(measure.metric) ? measure.period?.value : measure.value);
    const isFileComponent = isFile(baseComponent.qualifier);
    const selectedIdx = this.getSelectedIndex();

    return (
      <div
        className="layout-page-main no-outline"
        ref={(container) => (this.container = container)}
      >
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
                    {!isFileComponent && metric && (
                      <>
                        <div id="measures-view-selection-label">
                          {translate('component_measures.view_as')}
                        </div>
                        <MeasureViewSelect
                          className="measure-view-select spacer-left big-spacer-right"
                          handleViewChange={this.updateView}
                          metric={metric}
                          view={view}
                        />

                        <PageActions
                          componentQualifier={rootComponent.qualifier}
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
          {isFileComponent ? (
            <div className="measure-details-viewer">
              <SourceViewer
                branchLike={branchLike}
                component={baseComponent.key}
                metricKey={this.state.metric?.key}
                onIssueChange={this.props.onIssueChange}
              />
            </div>
          ) : (
            this.renderMeasure()
          )}
        </div>
      </div>
    );
  }
}
