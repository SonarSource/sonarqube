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
import * as PropTypes from 'prop-types';
import { sortBy, uniqBy } from 'lodash';
import Helmet from 'react-helmet';
import Header from './Header';
import EditionBoxes from './EditionBoxes';
import Footer from './Footer';
import PendingActions from './PendingActions';
import PluginsList from './PluginsList';
import Search from './Search';
import { filterPlugins, parseQuery, Query, serializeQuery } from './utils';
import {
  getAvailablePlugins,
  getInstalledPluginsWithUpdates,
  getPendingPlugins,
  getPluginUpdates,
  Plugin,
  PluginPending,
  getInstalledPlugins
} from '../../api/plugins';
import { Edition, EditionStatus } from '../../api/marketplace';
import { RawQuery } from '../../helpers/query';
import { translate } from '../../helpers/l10n';
import './style.css';

export interface Props {
  editions?: Edition[];
  editionsReadOnly: boolean;
  editionStatus?: EditionStatus;
  loadingEditions: boolean;
  location: { pathname: string; query: RawQuery };
  standaloneMode: boolean;
  updateCenterActive: boolean;
  setEditionStatus: (editionStatus: EditionStatus) => void;
}

interface State {
  loadingPlugins: boolean;
  pending: {
    installing: PluginPending[];
    updating: PluginPending[];
    removing: PluginPending[];
  };
  plugins: Plugin[];
}

export default class App extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      loadingPlugins: true,
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
    let fetchFunction = this.fetchAllPlugins;

    if (query.filter === 'updates') {
      fetchFunction = getPluginUpdates;
    } else if (query.filter === 'installed') {
      fetchFunction = getInstalledPlugins;
    }

    this.setState({ loadingPlugins: true });
    fetchFunction().then((plugins: Plugin[]) => {
      if (this.mounted) {
        this.setState({
          loadingPlugins: false,
          plugins: sortBy(plugins, 'name')
        });
      }
    }, this.stopLoadingPlugins);
  };

  fetchAllPlugins = (): Promise<Plugin[] | void> => {
    return Promise.all([getInstalledPluginsWithUpdates(), getAvailablePlugins()]).then(
      ([installed, available]) => uniqBy([...installed, ...available.plugins], 'key'),
      this.stopLoadingPlugins
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

  updateQuery = (newQuery: Partial<Query>) => {
    const query = serializeQuery({ ...parseQuery(this.props.location.query), ...newQuery });
    this.context.router.push({ pathname: this.props.location.pathname, query });
  };

  stopLoadingPlugins = () => {
    if (this.mounted) {
      this.setState({ loadingPlugins: false });
    }
  };

  render() {
    const { editions, editionStatus, standaloneMode } = this.props;
    const { loadingPlugins, plugins, pending } = this.state;
    const query = parseQuery(this.props.location.query);
    const filteredPlugins = query.search ? filterPlugins(plugins, query.search) : plugins;

    return (
      <div className="page page-limited" id="marketplace-page">
        <Helmet title={translate('marketplace.page')} />
        <div className="page-notifs">
          {standaloneMode && (
            <PendingActions pending={pending} refreshPending={this.fetchPendingPlugins} />
          )}
        </div>
        <Header />
        <EditionBoxes
          canInstall={standaloneMode && !this.props.editionsReadOnly}
          canUninstall={standaloneMode}
          editionStatus={editionStatus}
          editions={editions}
          loading={this.props.loadingEditions}
          updateCenterActive={this.props.updateCenterActive}
          updateEditionStatus={this.props.setEditionStatus}
        />
        <Search
          query={query}
          updateCenterActive={this.props.updateCenterActive}
          updateQuery={this.updateQuery}
        />
        {loadingPlugins && <i className="spinner" />}
        {!loadingPlugins && (
          <PluginsList
            pending={pending}
            plugins={filteredPlugins}
            readOnly={!standaloneMode}
            refreshPending={this.fetchPendingPlugins}
            updateQuery={this.updateQuery}
          />
        )}
        {!loadingPlugins && <Footer total={filteredPlugins.length} />}
      </div>
    );
  }
}
