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
import { Helmet } from 'react-helmet';
import ListFooter from 'sonar-ui-common/components/controls/ListFooter';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  createMetric,
  deleteMetric,
  getMetricDomains,
  getMetrics,
  getMetricTypes,
  MetricsResponse,
  updateMetric
} from '../../../api/metrics';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { MetricProps } from './Form';
import Header from './Header';
import List from './List';

interface Props {}

interface State {
  domains?: string[];
  loading: boolean;
  metrics?: T.Metric[];
  paging?: T.Paging;
  types?: string[];
}

const PAGE_SIZE = 50;

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchData = () => {
    Promise.all([
      getMetricDomains(),
      getMetricTypes(),
      getMetrics({ isCustom: true, ps: PAGE_SIZE })
    ]).then(([domains, types, metricsResponse]) => {
      if (this.mounted) {
        this.setState({
          domains,
          loading: false,
          metrics: metricsResponse.metrics,
          paging: this.getPaging(metricsResponse),
          types
        });
      }
    }, this.stopLoading);
  };

  fetchMore = () => {
    const { paging } = this.state;
    if (paging) {
      this.setState({ loading: true });
      getMetrics({ isCustom: true, p: paging.pageIndex + 1, ps: PAGE_SIZE }).then(
        metricsResponse => {
          if (this.mounted) {
            this.setState(({ metrics = [] }: State) => ({
              loading: false,
              metrics: [...metrics, ...metricsResponse.metrics],
              paging: this.getPaging(metricsResponse)
            }));
          }
        },
        this.stopLoading
      );
    }
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  getPaging = (response: MetricsResponse): T.Paging => ({
    pageIndex: response.p,
    pageSize: response.ps,
    total: response.total
  });

  handleCreate = (data: MetricProps) => {
    return createMetric(data).then(metric => {
      if (this.mounted) {
        this.setState(({ metrics = [], paging }: State) => ({
          metrics: [...metrics, metric],
          paging: paging && { ...paging, total: paging.total + 1 }
        }));
      }
    });
  };

  handleEdit = (data: { id: string } & MetricProps) => {
    return updateMetric(data).then(() => {
      if (this.mounted) {
        this.setState(({ metrics = [] }: State) => ({
          metrics: metrics.map(metric => (metric.id === data.id ? { ...metric, ...data } : metric))
        }));
      }
    });
  };

  handleDelete = (metricKey: string) => {
    return deleteMetric({ keys: metricKey }).then(() => {
      if (this.mounted) {
        this.setState(({ metrics = [], paging }: State) => ({
          metrics: metrics.filter(metric => metric.key !== metricKey),
          paging: paging && { ...paging, total: paging.total - 1 }
        }));
      }
    });
  };

  render() {
    const { domains, loading, metrics, paging, types } = this.state;

    return (
      <>
        <Suggestions suggestions="custom_metrics" />
        <Helmet title={translate('custom_metrics.page')} />
        <div className="page page-limited" id="custom-metrics-page">
          <Header domains={domains} loading={loading} onCreate={this.handleCreate} types={types} />
          {metrics && (
            <List
              domains={domains}
              metrics={metrics}
              onDelete={this.handleDelete}
              onEdit={this.handleEdit}
              types={types}
            />
          )}
          {metrics && paging && (
            <ListFooter
              count={metrics.length}
              loadMore={this.fetchMore}
              ready={!loading}
              total={paging.total}
            />
          )}
        </div>
      </>
    );
  }
}
