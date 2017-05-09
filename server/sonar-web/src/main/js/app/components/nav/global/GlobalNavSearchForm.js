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
import classNames from 'classnames';
import key from 'keymaster';
import { debounce, groupBy, keyBy, sortBy, uniqBy } from 'lodash';
import GlobalNavSearchFormComponent from './GlobalNavSearchFormComponent';
import type { Component } from './GlobalNavSearchFormComponent';
import RecentHistory from '../../RecentHistory';
import DeferredSpinner from '../../../../components/common/DeferredSpinner';
import { getSuggestions } from '../../../../api/components';
import { getFavorites } from '../../../../api/favorites';
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import { scrollToElement } from '../../../../helpers/scrolling';
import { getProjectUrl } from '../../../../helpers/urls';

type Props = {|
  appState: { organizationsEnabled: boolean },
  currentUser: { isLoggedIn: boolean }
|};

type State = {
  loading: boolean,
  loadingMore: ?string,
  more: { [string]: number },
  open: boolean,
  organizations: { [string]: { name: string } },
  projects: { [string]: { name: string } },
  query: string,
  results: { [qualifier: string]: Array<Component> },
  selected: ?string,
  shortQuery: boolean
};

const ORDER = ['DEV', 'VW', 'SVW', 'TRK', 'BRC', 'FIL', 'UTS'];

export default class GlobalNavSearchForm extends React.PureComponent {
  input: HTMLElement;
  mounted: boolean;
  node: HTMLElement;
  nodes: { [string]: HTMLElement };
  props: Props;
  state: State;

  static contextTypes = {
    router: React.PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.nodes = {};
    this.search = debounce(this.search, 250);
    this.fetchFavoritesAndRecentlyBrowsed = debounce(this.fetchFavoritesAndRecentlyBrowsed, 250, {
      leading: true
    });
    this.state = {
      loading: false,
      loadingMore: null,
      more: {},
      open: false,
      organizations: {},
      projects: {},
      query: '',
      results: {},
      selected: null,
      shortQuery: false
    };
  }

  componentDidMount() {
    this.mounted = true;
    key('s', () => {
      this.input.focus();
      this.openSearch();
      return false;
    });
    this.fetchFavoritesAndRecentlyBrowsed();
  }

  componentWillUpdate() {
    this.nodes = {};
  }

  componentDidUpdate(prevProps: Props, prevState: State) {
    if (prevState.selected !== this.state.selected) {
      this.scrollToSelected();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    key.unbind('s');
    window.removeEventListener('click', this.handleClickOutside);
  }

  handleClickOutside = (event: { target: HTMLElement }) => {
    if (!this.node || !this.node.contains(event.target)) {
      this.closeSearch();
    }
  };

  openSearch = () => {
    window.addEventListener('click', this.handleClickOutside);
    if (!this.state.open) {
      this.fetchFavoritesAndRecentlyBrowsed();
    }
    this.setState({ open: true });
  };

  closeSearch = () => {
    if (this.input) {
      this.input.blur();
    }
    window.removeEventListener('click', this.handleClickOutside);
    this.setState({
      more: {},
      open: false,
      organizations: {},
      projects: {},
      query: '',
      results: {},
      selected: null,
      shortQuery: false
    });
  };

  getPlainComponentsList = (results: { [qualifier: string]: Array<Component> }): Array<Component> =>
    this.sortQualifiers(Object.keys(results)).reduce(
      (components, qualifier) => [...components, ...results[qualifier]],
      []
    );

  mergeWithRecentlyBrowsed = (components: Array<Component>) => {
    const recentlyBrowsed = RecentHistory.get().map(component => ({
      ...component,
      isRecentlyBrowsed: true,
      qualifier: component.icon.toUpperCase()
    }));
    return uniqBy([...components, ...recentlyBrowsed], 'key');
  };

  fetchFavoritesAndRecentlyBrowsed = () => {
    const done = (components: Array<Component>) => {
      const results = groupBy(this.mergeWithRecentlyBrowsed(components), 'qualifier');
      const list = this.getPlainComponentsList(results);
      this.setState({
        loading: false,
        more: {},
        results,
        selected: list.length > 0 ? list[0].key : null
      });
    };

    if (this.props.currentUser.isLoggedIn) {
      this.setState({ loading: true });
      getFavorites().then(response => {
        if (this.mounted) {
          done(response.favorites.map(component => ({ ...component, isFavorite: true })));
        }
      });
    } else {
      done([]);
    }
  };

  search = (query: string) => {
    this.setState({ loading: true });
    const recentlyBrowsed = RecentHistory.get().map(component => component.key);
    getSuggestions(query, recentlyBrowsed).then(response => {
      if (this.mounted) {
        const results = {};
        const more = {};
        response.results.forEach(group => {
          results[group.q] = group.items.map(item => ({ ...item, qualifier: group.q }));
          more[group.q] = group.more;
        });
        const list = this.getPlainComponentsList(results);
        this.setState(state => ({
          loading: false,
          more,
          organizations: { ...state.organizations, ...keyBy(response.organizations, 'key') },
          projects: { ...state.projects, ...keyBy(response.projects, 'key') },
          results,
          selected: list.length > 0 ? list[0].key : null,
          shortQuery: response.warning === 'short_input'
        }));
      }
    });
  };

  searchMore = (qualifier: string) => {
    this.setState({ loading: true, loadingMore: qualifier });
    const recentlyBrowsed = RecentHistory.get().map(component => component.key);
    getSuggestions(this.state.query, recentlyBrowsed, qualifier).then(response => {
      if (this.mounted) {
        const group = response.results.find(group => group.q === qualifier);
        const moreResults = (group ? group.items : []).map(item => ({ ...item, qualifier }));
        this.setState(state => ({
          loading: false,
          loadingMore: null,
          more: { ...state.more, [qualifier]: 0 },
          organizations: { ...state.organizations, ...keyBy(response.organizations, 'key') },
          projects: { ...state.projects, ...keyBy(response.projects, 'key') },
          results: {
            ...state.results,
            [qualifier]: uniqBy([...state.results[qualifier], ...moreResults], 'key')
          }
        }));
      }
    });
  };

  handleQueryChange = (event: { currentTarget: HTMLInputElement }) => {
    const query = event.currentTarget.value;
    this.setState({ query, shortQuery: query.length === 1 });
    if (query.length === 0) {
      this.fetchFavoritesAndRecentlyBrowsed();
    } else if (query.length >= 2) {
      this.search(query);
    }
  };

  selectPrevious = () => {
    this.setState((state: State) => {
      const list = this.getPlainComponentsList(state.results);
      const index = list.findIndex(component => component.key === state.selected);
      return index > 0 ? { selected: list[index - 1].key } : undefined;
    });
  };

  selectNext = () => {
    this.setState((state: State) => {
      const list = this.getPlainComponentsList(state.results);
      const index = list.findIndex(component => component.key === state.selected);
      return index >= 0 && index < list.length - 1 ? { selected: list[index + 1].key } : undefined;
    });
  };

  openSelected = () => {
    if (this.state.selected) {
      this.context.router.push(getProjectUrl(this.state.selected));
      this.closeSearch();
    }
  };

  scrollToSelected = () => {
    if (this.state.selected) {
      const node = this.nodes[this.state.selected];
      if (node) {
        scrollToElement(node, { topOffset: 30, bottomOffset: 30, parent: this.node });
      }
    }
  };

  handleKeyDown = (event: KeyboardEvent) => {
    switch (event.keyCode) {
      case 13:
        event.preventDefault();
        this.openSelected();
        return;
      case 27:
        event.preventDefault();
        this.closeSearch();
        return;
      case 38:
        event.preventDefault();
        this.selectPrevious();
        return;
      case 40:
        event.preventDefault();
        this.selectNext();
        return;
    }
  };

  handleSelect = (selected: string) => {
    this.setState({ selected });
  };

  handleMoreClick = (event: MouseEvent & { currentTarget: HTMLElement }) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();
    const { qualifier } = event.currentTarget.dataset;
    this.searchMore(qualifier);
  };

  sortQualifiers = (qualifiers: Array<string>) =>
    sortBy(qualifiers, qualifier => ORDER.indexOf(qualifier));

  innerRef = (component: string, node: HTMLElement) => {
    this.nodes[component] = node;
  };

  renderComponent = (component: Component) => (
    <GlobalNavSearchFormComponent
      appState={this.props.appState}
      component={component}
      innerRef={this.innerRef}
      key={component.key}
      onClose={this.closeSearch}
      onSelect={this.handleSelect}
      organizations={this.state.organizations}
      projects={this.state.projects}
      selected={this.state.selected === component.key}
    />
  );

  renderComponents = () => {
    const qualifiers = Object.keys(this.state.results);
    const renderedComponents = [];

    this.sortQualifiers(qualifiers).forEach(qualifier => {
      const components = this.state.results[qualifier];

      if (components.length > 0 && renderedComponents.length > 0) {
        renderedComponents.push(<li key={`divider-${qualifier}`} className="divider" />);
      }

      if (components.length > 0) {
        renderedComponents.push(
          <li key={`header-${qualifier}`} className="dropdown-header">
            {translate('qualifiers', qualifier)}
          </li>
        );
      }

      components.forEach(component => {
        renderedComponents.push(this.renderComponent(component));
      });

      const more = this.state.more[qualifier];
      if (more != null && more > 0) {
        renderedComponents.push(
          <li key={`more-${qualifier}`} className="menu-footer">
            <DeferredSpinner
              className="navbar-search-icon"
              loading={this.state.loadingMore === qualifier}>
              <a data-qualifier={qualifier} href="#" onClick={this.handleMoreClick}>
                {translate('show_more')}
              </a>
            </DeferredSpinner>
          </li>
        );
      }
    });

    return renderedComponents;
  };

  render() {
    const dropdownClassName = classNames('dropdown', 'navbar-search', { open: this.state.open });

    return (
      <li className={dropdownClassName}>
        <DeferredSpinner className="navbar-search-icon" loading={this.state.loading}>
          <i className="navbar-search-icon icon-search" />
        </DeferredSpinner>

        <input
          autoComplete="off"
          className="navbar-search-input js-search-input"
          maxLength="30"
          name="q"
          onChange={this.handleQueryChange}
          onClick={event => event.stopPropagation()}
          onFocus={this.openSearch}
          onKeyDown={this.handleKeyDown}
          ref={node => (this.input = node)}
          placeholder={translate('search.placeholder')}
          type="search"
          value={this.state.query}
        />

        {this.state.shortQuery &&
          <span
            className={classNames('navbar-search-input-hint', {
              'is-shifted': this.state.query.length > 5
            })}>
            {translateWithParameters('select2.tooShort', 2)}
          </span>}

        {this.state.open &&
          Object.keys(this.state.results).length > 0 &&
          <div
            className="dropdown-menu dropdown-menu-right global-navbar-search-dropdown"
            ref={node => (this.node = node)}>
            <ul className="menu">
              {this.renderComponents()}
            </ul>
            <div
              className="navbar-search-shortcut-hint"
              dangerouslySetInnerHTML={{
                __html: translateWithParameters('search.shortcut_hint', 's')
              }}
            />
          </div>}
      </li>
    );
  }
}
