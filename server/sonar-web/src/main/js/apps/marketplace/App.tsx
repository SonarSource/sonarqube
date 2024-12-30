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
import {
  BasicSeparator,
  FlagMessage,
  LargeCenteredLayout,
  PageContentFontWrapper,
  Spinner,
  SubTitle,
} from '~design-system';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location, Router } from '~sonar-aligned/types/router';
import { getAvailablePlugins, getInstalledPlugins } from '../../api/plugins';
import { getValue, setSimpleSettingValue } from '../../api/settings';
import DocumentationLink from '../../components/common/DocumentationLink';
import ListFooter from '../../components/controls/ListFooter';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { EditionKey } from '../../types/editions';
import { PendingPluginResult, Plugin, RiskConsent } from '../../types/plugins';
import { SettingsKey } from '../../types/settings';
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
  location: Location;
  pendingPlugins: PendingPluginResult;
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
     * condition if it ever becomes possible to have a community build NOT in standalone mode.
     */
    const allowActions =
      currentEdition === EditionKey.community &&
      Boolean(standaloneMode) &&
      riskConsent === RiskConsent.Accepted;

    return (
      <LargeCenteredLayout as="main" id="marketplace-page">
        <PageContentFontWrapper className="sw-typo-default sw-py-8">
          <Helmet title={translate('marketplace.page')} />

          <BasicSeparator className="sw-my-6" />

          <div>
            <SubTitle>{translate('marketplace.page.plugins')}</SubTitle>
            <div className="sw-mt-2 sw-max-w-abs-600 ">
              <p>{translate('marketplace.page.plugins.description')}</p>
              {currentEdition !== EditionKey.community && (
                <FlagMessage className="sw-mt-2" variant="info">
                  <p>
                    <FormattedMessage
                      id="marketplace.page.plugins.description2"
                      defaultMessage={translate('marketplace.page.plugins.description2')}
                      values={{
                        link: (
                          <DocumentationLink to={DocLink.InstanceAdminMarketplace}>
                            {translate('marketplace.page.plugins.description2.link')}
                          </DocumentationLink>
                        ),
                      }}
                    />
                  </p>
                </FlagMessage>
              )}
            </div>
          </div>

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
          <div className="sw-mt-4">
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
                  <ListFooter count={filteredPlugins.length} total={plugins.length} />
                </>
              )}
            </Spinner>
          </div>
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

export default withRouter(App);
