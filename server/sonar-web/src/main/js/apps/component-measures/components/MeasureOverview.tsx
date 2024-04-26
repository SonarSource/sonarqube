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
import { Spinner } from 'design-system';
import * as React from 'react';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { getComponentLeaves } from '../../../api/components';
import SourceViewer from '../../../components/SourceViewer/SourceViewer';
import { isSameBranchLike } from '../../../helpers/branch-like';
import { BranchLike } from '../../../types/branch-like';
import { isFile } from '../../../types/component';
import {
  ComponentMeasure,
  ComponentMeasureEnhanced,
  ComponentMeasureIntern,
  Dict,
  Metric,
  Paging,
  Period,
} from '../../../types/types';
import BubbleChartView from '../drilldown/BubbleChartView';
import { BUBBLES_FETCH_LIMIT, enhanceComponent, getBubbleMetrics, hasFullMeasures } from '../utils';
import LeakPeriodLegend from './LeakPeriodLegend';
import MeasureContentHeader from './MeasureContentHeader';
import MeasuresBreadcrumbs from './MeasuresBreadcrumbs';

interface Props {
  branchLike?: BranchLike;
  className?: string;
  component: ComponentMeasure;
  domain: string;
  leakPeriod?: Period;
  loading: boolean;
  metrics: Dict<Metric>;
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
      () => this.props.updateLoading({ bubbles: false }),
    );
  };

  renderContent(isFile: boolean) {
    const { branchLike, component, domain, metrics } = this.props;
    const { paging } = this.state;

    if (isFile) {
      return (
        <div className="measure-details-viewer">
          <SourceViewer hideHeader branchLike={branchLike} component={component.key} />
        </div>
      );
    }

    return (
      <BubbleChartView
        component={component}
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
    const isFileComponent = isFile(component.qualifier);

    return (
      <div className={className}>
        <A11ySkipTarget anchor="measures_main" />

        <MeasureContentHeader
          left={
            <MeasuresBreadcrumbs
              backToFirst
              branchLike={branchLike}
              component={component}
              handleSelect={this.props.updateSelected}
              rootComponent={rootComponent}
            />
          }
          right={
            leakPeriod &&
            displayLeak && <LeakPeriodLegend component={component} period={leakPeriod} />
          }
        />

        <div className="sw-p-6">
          <Spinner loading={loading} />
          {!loading && this.renderContent(isFileComponent)}
        </div>
      </div>
    );
  }
}
