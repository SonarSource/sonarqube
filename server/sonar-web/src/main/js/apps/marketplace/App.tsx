/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import * as PropTypes from 'prop-types';
import { sortBy, uniqBy } from 'lodash';
import Helmet from 'react-helmet';
import Header from './Header';
import EditionsStatusNotif from './components/EditionsStatusNotif';
import EditionBoxes from './EditionBoxes';
import Footer from './Footer';
import PendingActions from './PendingActions';
import PluginsList from './PluginsList';
import Search from './Search';
import {
  getAvailablePlugins,
  getInstalledPluginsWithUpdates,
  getPendingPlugins,
  getPluginUpdates,
  Plugin,
  PluginPending
} from '../../api/plugins';
import { EditionStatus, getEditionStatus } from '../../api/marketplace';
import { RawQuery } from '../../helpers/query';
import { translate } from '../../helpers/l10n';
import { filterPlugins, parseQuery, Query, serializeQuery } from './utils';

export interface Props {
  editionsUrl: string;
  location: { pathname: string; query: RawQuery };
  sonarqubeVersion: string;
  updateCenterActive: boolean;
}

interface State {
  editionStatus?: EditionStatus;
  loading: boolean;
  pending: {
    installing: PluginPending[];
    updating: PluginPending[];
    removing: PluginPending[];
  };
  plugins: Plugin[];
}

export default class App extends React.PureComponent<Props, State> {
  mounted: boolean;

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      loading: true,
      pending: {
        installing: [],
        updating: [],
        removing: []
      },
      plugins: []
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchPendingPlugins();
    this.fetchEditionStatus();
    this.fetchQueryPlugins();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.location.query.filter !== this.props.location.query.filter) {
      this.fetchQueryPlugins();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchQueryPlugins = () => {
    const query = parseQuery(this.props.location.query);
    if (query.filter === 'updates') {
      this.fetchUpdatesOnly();
    } else {
      this.fetchAllPlugins();
    }
  };

  fetchAllPlugins = () => {
    this.setState({ loading: true });
    Promise.all([getInstalledPluginsWithUpdates(), getAvailablePlugins()]).then(
      ([installed, available]) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            plugins: sortBy(uniqBy([...installed, ...available.plugins], 'key'), 'name')
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  fetchUpdatesOnly = () => {
    this.setState({ loading: true });
    getPluginUpdates().then(
      plugins => {
        if (this.mounted) {
          this.setState({ loading: false, plugins });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  fetchPendingPlugins = () =>
    getPendingPlugins().then(
      pending => {
        if (this.mounted) {
          this.setState({ pending });
        }
      },
      () => {}
    );

  fetchEditionStatus = () =>
    getEditionStatus().then(
      editionStatus => {
        if (this.mounted) {
          this.updateEditionStatus(editionStatus);
        }
      },
      () => {}
    );

  updateEditionStatus = (editionStatus: EditionStatus) =>
    this.setState({ editionStatus: editionStatus });

  updateQuery = (newQuery: Partial<Query>) => {
    const query = serializeQuery({
      ...parseQuery(this.props.location.query),
      ...newQuery
    });
    this.context.router.push({
      pathname: this.props.location.pathname,
      query
    });
  };

  render() {
    const { editionStatus, plugins, pending } = this.state;
    const query = parseQuery(this.props.location.query);
    const filteredPlugins = query.search ? filterPlugins(plugins, query.search) : plugins;
    return (
      <div className="page page-limited" id="marketplace-page">
        <Helmet title={translate('marketplace.page')} />
        {editionStatus && <EditionsStatusNotif editionStatus={editionStatus} />}
        <Header />
        <EditionBoxes
          editionStatus={editionStatus}
          editionsUrl={this.props.editionsUrl}
          sonarqubeVersion={this.props.sonarqubeVersion}
          updateCenterActive={this.props.updateCenterActive}
          updateEditionStatus={this.updateEditionStatus}
        />
        <PendingActions refreshPending={this.fetchPendingPlugins} pending={pending} />
        <Search
          query={query}
          updateCenterActive={this.props.updateCenterActive}
          updateQuery={this.updateQuery}
        />
        <PluginsList
          plugins={filteredPlugins}
          pending={pending}
          refreshPending={this.fetchPendingPlugins}
          updateQuery={this.updateQuery}
        />
        <Footer total={filteredPlugins.length} />
      </div>
    );
  }
}
