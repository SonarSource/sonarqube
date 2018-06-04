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
import Helmet from 'react-helmet';
import { Link } from 'react-router';
import Menu from './Menu';
import Search from './Search';
import Domain from './Domain';
import { Domain as DomainType, fetchWebApi } from '../../../api/web-api';
import ScreenPositionHelper from '../../../components/common/ScreenPositionHelper';
import { getActionKey, isDomainPathActive, Query, serializeQuery, parseQuery } from '../utils';
import { scrollToElement } from '../../../helpers/scrolling';
import { translate } from '../../../helpers/l10n';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { RawQuery } from '../../../helpers/query';
import '../styles/web-api.css';

interface Props {
  location: { pathname: string; query: RawQuery };
  params: { splat?: string };
}

interface State {
  domains: DomainType[];
}

export default class WebApiApp extends React.PureComponent<Props, State> {
  mounted = false;

  static contextTypes = {
    router: PropTypes.object.isRequired
  };

  constructor(props: Props) {
    super(props);
    this.state = { domains: [] };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchList();
    const footer = document.getElementById('footer');
    if (footer) {
      footer.classList.add('page-footer-with-sidebar');
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
      footer.classList.remove('page-footer-with-sidebar');
    }
  }

  fetchList() {
    fetchWebApi().then(
      domains => {
        if (this.mounted) {
          this.setState({ domains });
        }
      },
      () => {}
    );
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
    this.context.router.push({ pathname: this.props.location.pathname, query });
  };

  toggleInternalInitially() {
    const splat = this.props.params.splat || '';
    const { domains } = this.state;
    const query = parseQuery(this.props.location.query);

    if (!query.internal && splat) {
      const domain = domains.find(domain => domain.path.startsWith(splat));
      if (domain) {
        let action;
        if (domain.path !== splat) {
          action = domain.actions.find(action => getActionKey(domain.path, action.key) === splat);
        }
        if (domain.internal || (action && action.internal)) {
          this.updateQuery({ internal: true });
        }
      }
    }
  }

  handleSearch = (search: string) => {
    this.updateQuery({ search });
  };

  handleToggleInternal = () => {
    const splat = this.props.params.splat || '';
    const { router } = this.context;
    const { domains } = this.state;
    const domain = domains.find(domain => isDomainPathActive(domain.path, splat));
    const query = parseQuery(this.props.location.query);
    const internal = !query.internal;

    if (domain && domain.internal && !internal) {
      router.push({
        pathname: '/web_api',
        query: { ...serializeQuery(query), internal: false }
      });
      return;
    }

    this.updateQuery({ internal });
  };

  handleToggleDeprecated = () => {
    const query = parseQuery(this.props.location.query);
    this.updateQuery({ deprecated: !query.deprecated });
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
