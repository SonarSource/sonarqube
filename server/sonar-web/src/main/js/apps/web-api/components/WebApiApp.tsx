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

import styled from '@emotion/styled';
import { maxBy } from 'lodash';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Params, useParams } from 'react-router-dom';
import {
  LAYOUT_FOOTER_HEIGHT,
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LargeCenteredLayout,
  PageContentFontWrapper,
  Title,
} from '~design-system';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { Location, Router } from '~sonar-aligned/types/router';
import { fetchWebApi } from '../../../api/web-api';
import { translate } from '../../../helpers/l10n';
import { WebApi } from '../../../types/types';
import '../styles/web-api.css';
import {
  Query,
  getActionKey,
  isDomainPathActive,
  parseQuery,
  parseVersion,
  serializeQuery,
} from '../utils';
import Domain from './Domain';
import Menu from './Menu';
import Search from './Search';

interface Props {
  location: Location;
  params: Params;
  router: Router;
}

interface State {
  domains: WebApi.Domain[];
}

export class WebApiApp extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { domains: [] };

  componentDidMount() {
    this.mounted = true;
    this.fetchList();
  }

  componentDidUpdate() {
    this.enforceFlags();
    this.scrollToAction();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchList() {
    fetchWebApi().then(
      (domains) => {
        if (this.mounted) {
          this.setState({ domains: this.parseDomains(domains) });
        }
      },
      () => {},
    );
  }

  parseDomains(domains: any[]): WebApi.Domain[] {
    return domains.map((domain) => {
      const deprecated = getLatestDeprecatedAction(domain);
      const internal = !domain.actions.find((action: any) => !action.internal);
      return { ...domain, deprecatedSince: deprecated?.deprecatedSince, internal };
    });
  }

  scrollToAction = () => {
    const splat = this.props.params.splat || '';
    const action = document.getElementById(splat);
    action?.scrollIntoView({
      block: 'center',
    });
  };

  updateQuery = (newQuery: Partial<Query>) => {
    const query = serializeQuery({ ...parseQuery(this.props.location.query), ...newQuery });
    this.props.router.push({ pathname: this.props.location.pathname, query });
  };

  enforceFlags() {
    const splat = this.props.params.splat || '';
    const { domains } = this.state;
    const query = parseQuery(this.props.location.query);

    const domain = domains.find((domain) => splat.startsWith(domain.path));
    if (domain) {
      const action = domain.actions.find(
        (action) => getActionKey(domain.path, action.key) === splat,
      );
      const internal = Boolean(!query.internal && (domain.internal || action?.internal));
      const deprecated = Boolean(
        !query.deprecated && (domain.deprecatedSince || action?.deprecatedSince),
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
    const domain = domains.find((domain) => isDomainPathActive(domain.path, splat));
    const query = parseQuery(this.props.location.query);
    const value = !query[flag];

    if (domain?.[domainFlag] && !value) {
      this.props.router.push({
        pathname: '/web_api',
        query: serializeQuery({ ...query, [flag]: false }),
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
    const splat = this.props.params.splat ?? '';
    const query = parseQuery(this.props.location.query);
    const { domains } = this.state;

    const domain = domains.find((domain) => isDomainPathActive(domain.path, splat));

    return (
      <LargeCenteredLayout>
        <PageContentFontWrapper className="sw-typo-default sw-w-full sw-flex">
          <Helmet defer={false} title={translate('api_documentation.page')} />
          <div className="sw-w-full sw-flex">
            <NavContainer
              aria-label={translate('api_documentation.domain_nav')}
              className="sw--mx-2"
            >
              <A11ySkipTarget anchor="webapi_main" />

              <Title>{translate('api_documentation.page')}</Title>

              <Search
                onSearch={this.handleSearch}
                onToggleDeprecated={this.handleToggleDeprecated}
                onToggleInternal={this.handleToggleInternal}
                query={query}
              />
              <div className="sw-w-[300px] sw-mr-2">
                <Menu domains={this.state.domains} query={query} splat={splat} />
              </div>
            </NavContainer>
            <main
              style={{
                height: `calc(100vh - ${LAYOUT_FOOTER_HEIGHT + LAYOUT_GLOBAL_NAV_HEIGHT}px`,
              }}
              className="sw-box-border sw-overflow-y-auto sw-relative sw-flex-1 sw-min-w-0 sw-ml-8 sw-py-8"
            >
              {domain && <Domain domain={domain} key={domain.path} query={query} />}
            </main>
          </div>
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    );
  }
}

function WebApiAppWithParams(props: { location: Location; router: Router }) {
  const params = useParams();

  return <WebApiApp {...props} params={{ splat: params['*'] }} />;
}

export default withRouter(WebApiAppWithParams);

/** Checks if all actions are deprecated, and returns the latest deprecated one */
function getLatestDeprecatedAction(domain: Pick<WebApi.Domain, 'actions'>) {
  const noVersion = { major: 0, minor: 0 };
  const allActionsDeprecated = domain.actions.every(
    ({ deprecatedSince }) => deprecatedSince !== undefined,
  );
  const latestDeprecation =
    allActionsDeprecated &&
    maxBy(domain.actions, (action) => {
      const version = (action.deprecatedSince && parseVersion(action.deprecatedSince)) || noVersion;
      return version.major * 1024 + version.minor;
    });
  return latestDeprecation || undefined;
}

const NavContainer = styled.nav`
  scrollbar-gutter: stable;
  overflow-y: auto;
  overflow-x: hidden;
  box-sizing: border-box;
  height: calc(100vh - ${LAYOUT_FOOTER_HEIGHT + LAYOUT_GLOBAL_NAV_HEIGHT}px);
  padding-top: 1.5rem;
  padding-bottom: 1.5rem;
`;
