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
import Helmet from 'react-helmet';
import Header from './Header';
import List from './List';
import {
  getCustomMeasures,
  createCustomMeasure,
  updateCustomMeasure,
  deleteCustomMeasure
} from '../../../api/measures';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import ListFooter from '../../../components/controls/ListFooter';
import { translate } from '../../../helpers/l10n';

interface Props {
  component: { key: string };
}

interface State {
  loading: boolean;
  measures?: T.CustomMeasure[];
  paging?: T.Paging;
}

const PAGE_SIZE = 50;

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchMeasures();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchMeasures = () => {
    this.setState({ loading: true });
    getCustomMeasures({ projectKey: this.props.component.key, ps: PAGE_SIZE }).then(
      ({ customMeasures, paging }) => {
        if (this.mounted) {
          this.setState({ loading: false, measures: customMeasures, paging });
        }
      },
      this.stopLoading
    );
  };

  fetchMore = () => {
    const { paging } = this.state;
    if (paging) {
      this.setState({ loading: true });
      getCustomMeasures({
        projectKey: this.props.component.key,
        p: paging.pageIndex + 1,
        ps: PAGE_SIZE
      }).then(({ customMeasures, paging }) => {
        if (this.mounted) {
          this.setState(({ measures = [] }: State) => ({
            loading: false,
            measures: [...measures, ...customMeasures],
            paging
          }));
        }
      }, this.stopLoading);
    }
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleCreate = (data: { description: string; metricKey: string; value: string }) => {
    return createCustomMeasure({ ...data, projectKey: this.props.component.key }).then(measure => {
      if (this.mounted) {
        this.setState(({ measures = [], paging }: State) => ({
          measures: [...measures, measure],
          paging: paging && { ...paging, total: paging.total + 1 }
        }));
      }
    });
  };

  handleEdit = (data: { description: string; id: string; value: string }) => {
    return updateCustomMeasure(data).then(() => {
      if (this.mounted) {
        this.setState(({ measures = [] }: State) => ({
          measures: measures.map(measure =>
            measure.id === data.id ? { ...measure, ...data } : measure
          )
        }));
      }
    });
  };

  handleDelete = (measureId: string) => {
    return deleteCustomMeasure({ id: measureId }).then(() => {
      if (this.mounted) {
        this.setState(({ measures = [], paging }: State) => ({
          measures: measures.filter(measure => measure.id !== measureId),
          paging: paging && { ...paging, total: paging.total - 1 }
        }));
      }
    });
  };

  render() {
    const { loading, measures, paging } = this.state;

    return (
      <>
        <Suggestions suggestions="custom_measures" />
        <Helmet title={translate('custom_measures.page')} />
        <div className="page page-limited">
          <Header
            loading={loading}
            onCreate={this.handleCreate}
            skipMetrics={measures && measures.map(measure => measure.metric.key)}
          />
          {measures && (
            <List measures={measures} onDelete={this.handleDelete} onEdit={this.handleEdit} />
          )}
          {measures && paging && (
            <ListFooter
              count={measures.length}
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
