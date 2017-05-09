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
// @flow
import React from 'react';
import Helmet from 'react-helmet';
import { Link } from 'react-router';
import { fetchWebApi } from '../../../api/web-api';
import Menu from './Menu';
import Search from './Search';
import Domain from './Domain';
import { getActionKey, isDomainPathActive } from '../utils';
import { translate } from '../../../helpers/l10n';
import type { Domain as DomainType } from '../../../api/web-api';
import '../styles/web-api.css';

type State = {
  domains: Array<DomainType>,
  searchQuery: string,
  showDeprecated: boolean,
  showInternal: boolean
};

export default class WebApiApp extends React.PureComponent {
  mounted: boolean;
  scrollToAction: () => void;
  state: State = {
    domains: [],
    searchQuery: '',
    showDeprecated: false,
    showInternal: false
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchList();
    const footer = document.getElementById('footer');
    if (footer) {
      footer.classList.add('search-navigator-footer');
    }
  }

  componentDidUpdate() {
    this.toggleInternalInitially();
    this.scrollToAction();
  }

  componentWillUnmount() {
    this.mounted = false;
    const footer = document.getElementById('footer');
    if (footer) {
      footer.classList.remove('search-navigator-footer');
    }
  }

  fetchList(cb?: () => void) {
    fetchWebApi().then(domains => {
      if (this.mounted) {
        this.setState({ domains }, cb);
      }
    });
  }

  scrollToAction = () => {
    const splat = this.props.params.splat || '';
    this.scrollToElement(splat);
  };

  scrollToElement(id: string) {
    const element = document.getElementById(id);

    if (element) {
      const rect = element.getBoundingClientRect();
      const top = rect.top + window.pageYOffset - 20;

      window.scrollTo(0, top);
    } else {
      window.scrollTo(0, 0);
    }
  }

  toggleInternalInitially() {
    const splat = this.props.params.splat || '';
    const { domains, showInternal } = this.state;

    if (!showInternal) {
      domains.forEach(domain => {
        if (domain.path === splat && domain.internal) {
          this.setState({ showInternal: true });
        }
        domain.actions.forEach(action => {
          const actionKey = getActionKey(domain.path, action.key);
          if (actionKey === splat && action.internal) {
            this.setState({ showInternal: true });
          }
        });
      });
    }
  }

  handleSearch = (searchQuery: string) => {
    this.setState({ searchQuery });
  };

  handleToggleInternal = () => {
    const splat = this.props.params.splat || '';
    const { router } = this.context;
    const { domains } = this.state;
    const domain = domains.find(domain => isDomainPathActive(domain.path, splat));
    const showInternal = !this.state.showInternal;

    if (domain && domain.internal && !showInternal) {
      router.push('/web_api');
    }

    this.setState({ showInternal });
  };

  handleToggleDeprecated = () => {
    this.setState(state => ({ showDeprecated: !state.showDeprecated }));
  };

  render() {
    const splat = this.props.params.splat || '';
    const { domains, showInternal, showDeprecated, searchQuery } = this.state;

    const domain = domains.find(domain => isDomainPathActive(domain.path, splat));

    return (
      <div className="search-navigator sticky">
        <Helmet title={translate('api_documentation.page')} />
        <div className="search-navigator-side search-navigator-side-light" style={{ top: 30 }}>
          <div className="web-api-page-header">
            <Link to="/web_api/">
              <h1>{translate('api_documentation.page')}</h1>
            </Link>
          </div>

          <Search
            showDeprecated={showDeprecated}
            showInternal={showInternal}
            onSearch={this.handleSearch}
            onToggleInternal={this.handleToggleInternal}
            onToggleDeprecated={this.handleToggleDeprecated}
          />

          <Menu
            domains={this.state.domains}
            showDeprecated={showDeprecated}
            showInternal={showInternal}
            searchQuery={searchQuery}
            splat={splat}
          />
        </div>

        <div className="search-navigator-workspace">
          {domain &&
            <Domain
              key={domain.path}
              domain={domain}
              showDeprecated={showDeprecated}
              showInternal={showInternal}
              searchQuery={searchQuery}
            />}
        </div>
      </div>
    );
  }
}

WebApiApp.contextTypes = {
  router: React.PropTypes.object.isRequired
};
