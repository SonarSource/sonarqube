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
import { sortBy, uniqBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { FormattedMessage } from 'react-intl';
import { getAvailablePlugins, getInstalledPlugins } from '../../api/plugins';
import { getValue, setSimpleSettingValue } from '../../api/settings';
import DocLink from '../../components/common/DocLink';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { Location, Router, withRouter } from '../../components/hoc/withRouter';
import { Alert } from '../../components/ui/Alert';
import Spinner from '../../components/ui/Spinner';
import { translate } from '../../helpers/l10n';
import { EditionKey } from '../../types/editions';
import { PendingPluginResult, Plugin, RiskConsent } from '../../types/plugins';
import { SettingsKey } from '../../types/settings';
import EditionBoxes from './EditionBoxes';
import Footer from './Footer';
import Header from './Header';
import PluginsList from './PluginsList';
import Search from './Search';
import PluginRiskConsentBox from './components/PluginRiskConsentBox';
import './style.css';
import {
  Query,
  filterPlugins,
  getInstalledPluginsWithUpdates,
  getPluginUpdates,
  parseQuery,
  serializeQuery,
} from './utils';

interface Props {
  currentEdition?: EditionKey;
  fetchPendingPlugins: () => void;
  pendingPlugins: PendingPluginResult;
  location: Location;
  router: Router;
  standaloneMode?: boolean;
  updateCenterActive: boolean;
}

interface State {
  loadingPlugins: boolean;
  plugins: Plugin[];
  riskConsent?: RiskConsent;
}

class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loadingPlugins: true, plugins: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchQueryPlugins();
    this.fetchRiskConsent();
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
          plugins: sortBy(plugins, 'name'),
        });
      }
    }, this.stopLoadingPlugins);
  };

  fetchAllPlugins = (): Promise<Plugin[] | void> => {
    return Promise.all([getInstalledPluginsWithUpdates(), getAvailablePlugins()]).then(
      ([installed, available]) => uniqBy([...installed, ...available.plugins], 'key'),
      this.stopLoadingPlugins,
    );
  };

  fetchRiskConsent = async () => {
    const consent = await getValue({ key: SettingsKey.PluginRiskConsent });

    if (consent === undefined) {
      return;
    }

    this.setState({ riskConsent: consent.value as RiskConsent | undefined });
  };

  acknowledgeRisk = async () => {
    await setSimpleSettingValue({
      key: SettingsKey.PluginRiskConsent,
      value: RiskConsent.Accepted,
    });

    await this.fetchRiskConsent();
  };

  updateQuery = (newQuery: Partial<Query>) => {
    const query = serializeQuery({ ...parseQuery(this.props.location.query), ...newQuery });
    this.props.router.push({ pathname: this.props.location.pathname, query });
  };

  stopLoadingPlugins = () => {
    if (this.mounted) {
      this.setState({ loadingPlugins: false });
    }
  };

  render() {
    const { currentEdition, standaloneMode, pendingPlugins } = this.props;
    const { loadingPlugins, plugins, riskConsent } = this.state;
    const query = parseQuery(this.props.location.query);
    const filteredPlugins = filterPlugins(plugins, query.search);

    /*
     * standalone mode is true when cluster mode is not active. We preserve this
     * condition if it ever becomes possible to have a community edition NOT in standalone mode.
     */
    const allowActions =
      currentEdition === EditionKey.community &&
      Boolean(standaloneMode) &&
      riskConsent === RiskConsent.Accepted;

    return (
      <main className="page page-limited" id="marketplace-page">
        <Suggestions suggestions="marketplace" />
        <Helmet title={translate('marketplace.page')} />
        <Header currentEdition={currentEdition} />
        <EditionBoxes currentEdition={currentEdition} />
        <header className="page-header">
          <h2 className="page-title">{translate('marketplace.page.plugins')}</h2>
          <div className="page-description">
            <p>{translate('marketplace.page.plugins.description')}</p>
            {currentEdition !== EditionKey.community && (
              <Alert className="spacer-top" variant="info">
                <FormattedMessage
                  id="marketplace.page.plugins.description2"
                  defaultMessage={translate('marketplace.page.plugins.description2')}
                  values={{
                    link: (
                      <DocLink to="/instance-administration/marketplace/">
                        {translate('marketplace.page.plugins.description2.link')}
                      </DocLink>
                    ),
                  }}
                />
              </Alert>
            )}
          </div>
        </header>

        <PluginRiskConsentBox
          acknowledgeRisk={this.acknowledgeRisk}
          currentEdition={currentEdition}
          riskConsent={riskConsent}
        />

        <Search
          query={query}
          updateCenterActive={this.props.updateCenterActive}
          updateQuery={this.updateQuery}
        />
        <Spinner loading={loadingPlugins}>
          {filteredPlugins.length === 0 &&
            translate('marketplace.plugin_list.no_plugins', query.filter)}
          {filteredPlugins.length > 0 && (
            <>
              <PluginsList
                pending={pendingPlugins}
                plugins={filteredPlugins}
                readOnly={!allowActions}
                refreshPending={this.props.fetchPendingPlugins}
              />
              <Footer total={filteredPlugins.length} />
            </>
          )}
        </Spinner>
      </main>
    );
  }
}

export default withRouter(App);
