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
import { Edition, EditionStatus, getEditionsList, getEditionStatus } from '../../api/marketplace';
import { RawQuery } from '../../helpers/query';
import { translate } from '../../helpers/l10n';
import {
  getEditionsForLastVersion,
  getEditionsForVersion,
  filterPlugins,
  parseQuery,
  Query,
  serializeQuery
} from './utils';

export interface Props {
  editionsUrl: string;
  location: { pathname: string; query: RawQuery };
  sonarqubeVersion: string;
  standaloneMode: boolean;
  updateCenterActive: boolean;
}

interface State {
  editions?: Edition[];
  editionsReadOnly: boolean;
  editionStatus?: EditionStatus;
  loadingEditions: boolean;
  loadingPlugins: boolean;
  pending: {
    installing: PluginPending[];
    updating: PluginPending[];
    removing: PluginPending[];
  };
  plugins: Plugin[];
}

export default class App extends React.PureComponent<Props, State> {
  mounted: boolean;
  timer?: NodeJS.Timer;

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      editionsReadOnly: false,
      loadingEditions: true,
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
    this.fetchEditions();
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
    this.setState({ loadingPlugins: true });
    Promise.all([getInstalledPluginsWithUpdates(), getAvailablePlugins()]).then(
      ([installed, available]) => {
        if (this.mounted) {
          this.setState({
            loadingPlugins: false,
            plugins: sortBy(uniqBy([...installed, ...available.plugins], 'key'), 'name')
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loadingPlugins: false });
        }
      }
    );
  };

  fetchUpdatesOnly = () => {
    this.setState({ loadingPlugins: true });
    getPluginUpdates().then(
      plugins => {
        if (this.mounted) {
          this.setState({ loadingPlugins: false, plugins });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loadingPlugins: false });
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

  fetchEditions = () => {
    this.setState({ loadingEditions: true });
    getEditionsList(this.props.editionsUrl).then(
      editionsPerVersion => {
        if (this.mounted) {
          const newState = {
            editions: getEditionsForVersion(editionsPerVersion, this.props.sonarqubeVersion),
            editionsReadOnly: false,
            loadingEditions: false
          };
          if (!newState.editions) {
            newState.editions = getEditionsForLastVersion(editionsPerVersion);
            newState.editionsReadOnly = true;
          }
          this.setState(newState);
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loadingEditions: false });
        }
      }
    );
  };

  updateEditionStatus = (editionStatus: EditionStatus) => {
    this.setState({ editionStatus });
    if (this.timer) {
      global.clearTimeout(this.timer);
      this.timer = undefined;
    }
    if (editionStatus.installationStatus === 'AUTOMATIC_IN_PROGRESS') {
      this.timer = global.setTimeout(() => {
        this.fetchEditionStatus();
        this.timer = undefined;
      }, 2000);
    }
  };

  updateQuery = (newQuery: Partial<Query>) => {
    const query = serializeQuery({ ...parseQuery(this.props.location.query), ...newQuery });
    this.context.router.push({ pathname: this.props.location.pathname, query });
  };

  render() {
    const { standaloneMode } = this.props;
    const { editions, editionStatus, loadingPlugins, plugins, pending } = this.state;
    const query = parseQuery(this.props.location.query);
    const filteredPlugins = query.search ? filterPlugins(plugins, query.search) : plugins;

    return (
      <div className="page page-limited" id="marketplace-page">
        <Helmet title={translate('marketplace.page')} />
        <div className="page-notifs">
          {editionStatus && (
            <EditionsStatusNotif
              editions={editions}
              editionStatus={editionStatus}
              readOnly={!standaloneMode}
              updateEditionStatus={this.updateEditionStatus}
            />
          )}
          {!standaloneMode && (
            <PendingActions refreshPending={this.fetchPendingPlugins} pending={pending} />
          )}
        </div>
        <Header />
        <EditionBoxes
          editions={editions}
          loading={this.state.loadingEditions}
          editionStatus={editionStatus}
          editionsUrl={this.props.editionsUrl}
          readOnly={!standaloneMode || this.state.editionsReadOnly}
          sonarqubeVersion={this.props.sonarqubeVersion}
          updateCenterActive={this.props.updateCenterActive}
          updateEditionStatus={this.updateEditionStatus}
        />
        <Search
          query={query}
          updateCenterActive={this.props.updateCenterActive}
          updateQuery={this.updateQuery}
        />
        {loadingPlugins && <i className="spinner" />}
        {!loadingPlugins && (
          <PluginsList
            plugins={filteredPlugins}
            pending={pending}
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
