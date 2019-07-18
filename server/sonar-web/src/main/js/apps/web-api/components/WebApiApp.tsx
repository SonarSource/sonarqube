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
import { maxBy } from 'lodash';
import * as React from 'react';
import Helmet from 'react-helmet';
import { Link, withRouter, WithRouterProps } from 'react-router';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { addSideBarClass, removeSideBarClass } from 'sonar-ui-common/helpers/pages';
import { scrollToElement } from 'sonar-ui-common/helpers/scrolling';
import { fetchWebApi } from '../../../api/web-api';
import A11ySkipTarget from '../../../app/components/a11y/A11ySkipTarget';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import '../styles/web-api.css';
import {
  getActionKey,
  isDomainPathActive,
  parseQuery,
  parseVersion,
  Query,
  serializeQuery
} from '../utils';
import Domain from './Domain';
import Menu from './Menu';
import Search from './Search';

type Props = WithRouterProps;

interface State {
  domains: T.WebApi.Domain[];
}

export class WebApiApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { domains: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchList();
    addSideBarClass();
  }

  componentDidUpdate() {
    this.enforceFlags();
    this.scrollToAction();
  }

  componentWillUnmount() {
    this.mounted = false;
    removeSideBarClass();
  }

  fetchList() {
    fetchWebApi().then(
      domains => {
        if (this.mounted) {
          this.setState({ domains: this.parseDomains(domains) });
        }
      },
      () => {}
    );
  }

  parseDomains(domains: any[]): T.WebApi.Domain[] {
    return domains.map(domain => {
      const deprecated = getLatestDeprecatedAction(domain);
      const internal = !domain.actions.find((action: any) => !action.internal);
      return { ...domain, deprecatedSince: deprecated && deprecated.deprecatedSince, internal };
    });
  }

  scrollToAction = () => {
    const splat = this.props.params.splat || '';
    const action = document.getElementById(splat);
    if (action) {
      scrollToElement(action, { topOffset: 20, bottomOffset: 20 });
    } else {
      window.scrollTo(0, 0);
    }
  };

  updateQuery = (newQuery: Partial<Query>) => {
    const query = serializeQuery({ ...parseQuery(this.props.location.query), ...newQuery });
    this.props.router.push({ pathname: this.props.location.pathname, query });
  };

  enforceFlags() {
    const splat = this.props.params.splat || '';
    const { domains } = this.state;
    const query = parseQuery(this.props.location.query);

    const domain = domains.find(domain => splat.startsWith(domain.path));
    if (domain) {
      const action = domain.actions.find(action => getActionKey(domain.path, action.key) === splat);
      const internal = Boolean(!query.internal && (domain.internal || (action && action.internal)));
      const deprecated = Boolean(
        !query.deprecated && (domain.deprecatedSince || (action && action.deprecatedSince))
      );
      if (internal || deprecated) {
        this.updateQuery({ internal, deprecated });
      }
    }
  }

  handleSearch = (search: string) => {
    this.updateQuery({ search });
  };

  toggleFlag(flag: 'deprecated' | 'internal', domainFlag: 'deprecatedSince' | 'internal') {
    const splat = this.props.params.splat || '';
    const { domains } = this.state;
    const domain = domains.find(domain => isDomainPathActive(domain.path, splat));
    const query = parseQuery(this.props.location.query);
    const value = !query[flag];

    if (domain && domain[domainFlag] && !value) {
      this.props.router.push({
        pathname: '/web_api',
        query: serializeQuery({ ...query, [flag]: false })
      });
    } else {
      this.updateQuery({ [flag]: value });
    }
  }

  handleToggleInternal = () => {
    this.toggleFlag('internal', 'internal');
  };

  handleToggleDeprecated = () => {
    this.toggleFlag('deprecated', 'deprecatedSince');
  };

  render() {
    const splat = this.props.params.splat || '';
    const query = parseQuery(this.props.location.query);
    const { domains } = this.state;

    const domain = domains.find(domain => isDomainPathActive(domain.path, splat));

    return (
      <div className="layout-page">
        <Suggestions suggestions="api_documentation" />
        <Helmet title={translate('api_documentation.page')} />
        <ScreenPositionHelper className="layout-page-side-outer">
          {({ top }) => (
            <div className="layout-page-side" style={{ top }}>
              <div className="layout-page-side-inner">
                <div className="layout-page-filters">
                  <A11ySkipTarget anchor="webapi_main" />

                  <div className="web-api-page-header">
                    <Link to="/web_api/">
                      <h1>{translate('api_documentation.page')}</h1>
                    </Link>
                  </div>

                  <Search
                    onSearch={this.handleSearch}
                    onToggleDeprecated={this.handleToggleDeprecated}
                    onToggleInternal={this.handleToggleInternal}
                    query={query}
                  />

                  <Menu domains={this.state.domains} query={query} splat={splat} />
                </div>
              </div>
            </div>
          )}
        </ScreenPositionHelper>

        <div className="layout-page-main">
          <div className="layout-page-main-inner">
            {domain && <Domain domain={domain} key={domain.path} query={query} />}
          </div>
        </div>
      </div>
    );
  }
}

export default withRouter(WebApiApp);

/** Checks if all actions are deprecated, and returns the latest deprecated one */
function getLatestDeprecatedAction(domain: Pick<T.WebApi.Domain, 'actions'>) {
  const noVersion = { major: 0, minor: 0 };
  const allActionsDeprecated = domain.actions.every(
    ({ deprecatedSince }) => deprecatedSince !== undefined
  );
  const latestDeprecation =
    allActionsDeprecated &&
    maxBy(domain.actions, action => {
      const version = (action.deprecatedSince && parseVersion(action.deprecatedSince)) || noVersion;
      return version.major * 1024 + version.minor;
    });
  return latestDeprecation || undefined;
}
