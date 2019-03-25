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
import * as React from 'react';
import * as key from 'keymaster';
import { debounce, keyBy, uniqBy } from 'lodash';
import { FormattedMessage } from 'react-intl';
import { withRouter, WithRouterProps } from 'react-router';
import { sortQualifiers, More, Results, ComponentResult } from './utils';
import RecentHistory from '../RecentHistory';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import ClockIcon from '../../../components/icons-components/ClockIcon';
import OutsideClickHandler from '../../../components/controls/OutsideClickHandler';
import SearchBox from '../../../components/controls/SearchBox';
import { lazyLoad } from '../../../components/lazyLoad';
import { getSuggestions } from '../../../api/components';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { scrollToElement } from '../../../helpers/scrolling';
import { getProjectUrl, getCodeUrl } from '../../../helpers/urls';
import './Search.css';

const SearchResults = lazyLoad(() => import('./SearchResults'));
const SearchResult = lazyLoad(() => import('./SearchResult'));

interface OwnProps {
  appState: Pick<T.AppState, 'organizationsEnabled'>;
  currentUser: T.CurrentUser;
}

type Props = OwnProps & WithRouterProps;

interface State {
  loading: boolean;
  loadingMore?: string;
  more: More;
  open: boolean;
  organizations: T.Dict<{ name: string }>;
  projects: T.Dict<{ name: string }>;
  query: string;
  results: Results;
  selected?: string;
  shortQuery: boolean;
}

export class Search extends React.PureComponent<Props, State> {
  input?: HTMLInputElement | null;
  node?: HTMLElement | null;
  nodes: T.Dict<HTMLElement>;
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.nodes = {};
    this.search = debounce(this.search, 250);
    this.state = {
      loading: false,
      more: {},
      open: false,
      organizations: {},
      projects: {},
      query: '',
      results: {},
      shortQuery: false
    };
  }

  componentDidMount() {
    this.mounted = true;
    key('s', () => {
      this.focusInput();
      this.openSearch();
      return false;
    });
  }

  componentWillUpdate() {
    this.nodes = {};
  }

  componentDidUpdate(_prevProps: Props, prevState: State) {
    if (prevState.selected !== this.state.selected) {
      this.scrollToSelected();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    key.unbind('s');
  }

  focusInput = () => {
    if (this.input) {
      this.input.focus();
    }
  };

  handleClickOutside = () => {
    this.closeSearch(false);
  };

  handleFocus = () => {
    if (!this.state.open) {
      // simulate click to close any other dropdowns
      const body = document.documentElement;
      if (body) {
        body.click();
      }
    }
    this.openSearch();
  };

  openSearch = () => {
    if (!this.state.open && !this.state.query) {
      this.search('');
    }
    this.setState({ open: true });
  };

  closeSearch = (clear = true) => {
    if (this.input) {
      this.input.blur();
    }
    if (clear) {
      this.setState({
        more: {},
        open: false,
        organizations: {},
        projects: {},
        query: '',
        results: {},
        selected: undefined,
        shortQuery: false
      });
    } else {
      this.setState({ open: false });
    }
  };

  getPlainComponentsList = (results: Results, more: More) =>
    sortQualifiers(Object.keys(results)).reduce((components, qualifier) => {
      const next = [...components, ...results[qualifier].map(component => component.key)];
      if (more[qualifier]) {
        next.push('qualifier###' + qualifier);
      }
      return next;
    }, []);

  findFile = (key: string) => {
    const findInResults = (results: ComponentResult[] | undefined) =>
      results && results.find(r => r.key === key);

    const file = findInResults(this.state.results['FIL']);
    if (file) {
      return file;
    }

    const test = findInResults(this.state.results['UTS']);
    if (test) {
      return test;
    }

    return undefined;
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  search = (query: string) => {
    if (query.length === 0 || query.length >= 2) {
      this.setState({ loading: true });
      const recentlyBrowsed = RecentHistory.get().map(component => component.key);
      getSuggestions(query, recentlyBrowsed).then(response => {
        // compare `this.state.query` and `query` to handle two request done almost at the same time
        // in this case only the request that matches the current query should be taken
        if (this.mounted && this.state.query === query) {
          const results: Results = {};
          const more: More = {};
          response.results.forEach(group => {
            results[group.q] = group.items.map(item => ({ ...item, qualifier: group.q }));
            more[group.q] = group.more;
          });
          const list = this.getPlainComponentsList(results, more);
          this.setState(state => ({
            loading: false,
            more,
            organizations: { ...state.organizations, ...keyBy(response.organizations, 'key') },
            projects: { ...state.projects, ...keyBy(response.projects, 'key') },
            results,
            selected: list.length > 0 ? list[0] : undefined,
            shortQuery: query.length > 2 && response.warning === 'short_input'
          }));
        }
      }, this.stopLoading);
    } else {
      this.setState({ loading: false });
    }
  };

  searchMore = (qualifier: string) => {
    const { query } = this.state;
    if (query.length === 1) {
      return;
    }

    this.setState({ loading: true, loadingMore: qualifier });
    const recentlyBrowsed = RecentHistory.get().map(component => component.key);
    getSuggestions(query, recentlyBrowsed, qualifier).then(response => {
      if (this.mounted) {
        const group = response.results.find(group => group.q === qualifier);
        const moreResults = (group ? group.items : []).map(item => ({ ...item, qualifier }));
        this.setState(state => ({
          loading: false,
          loadingMore: undefined,
          more: { ...state.more, [qualifier]: 0 },
          organizations: { ...state.organizations, ...keyBy(response.organizations, 'key') },
          projects: { ...state.projects, ...keyBy(response.projects, 'key') },
          results: {
            ...state.results,
            [qualifier]: uniqBy([...state.results[qualifier], ...moreResults], 'key')
          },
          selected: moreResults.length > 0 ? moreResults[0].key : state.selected
        }));
        this.focusInput();
      }
    }, this.stopLoading);
  };

  handleQueryChange = (query: string) => {
    this.setState({ query, shortQuery: query.length === 1 });
    this.search(query);
  };

  selectPrevious = () => {
    this.setState(({ more, results, selected }) => {
      if (selected) {
        const list = this.getPlainComponentsList(results, more);
        const index = list.indexOf(selected);
        return index > 0 ? { selected: list[index - 1] } : null;
      } else {
        return null;
      }
    });
  };

  selectNext = () => {
    this.setState(({ more, results, selected }) => {
      if (selected) {
        const list = this.getPlainComponentsList(results, more);
        const index = list.indexOf(selected);
        return index >= 0 && index < list.length - 1 ? { selected: list[index + 1] } : null;
      } else {
        return null;
      }
    });
  };

  openSelected = () => {
    const { selected } = this.state;

    if (selected) {
      if (selected.startsWith('qualifier###')) {
        this.searchMore(selected.substr(12));
      } else {
        const file = this.findFile(selected);
        if (file) {
          this.props.router.push(getCodeUrl(file.project!, undefined, file.key));
        } else {
          this.props.router.push(getProjectUrl(selected));
        }
        this.closeSearch();
      }
    }
  };

  scrollToSelected = () => {
    if (this.state.selected) {
      const node = this.nodes[this.state.selected];
      if (node && this.node) {
        scrollToElement(node, { topOffset: 30, bottomOffset: 30, parent: this.node });
      }
    }
  };

  handleKeyDown = (event: React.KeyboardEvent) => {
    switch (event.keyCode) {
      case 13:
        event.preventDefault();
        this.openSelected();
        return;
      case 38:
        event.preventDefault();
        this.selectPrevious();
        return;
      case 40:
        event.preventDefault();
        this.selectNext();
        // keep this return to prevent fall-through in case more cases will be adder later
        // eslint-disable-next-line no-useless-return
        return;
    }
  };

  handleSelect = (selected: string) => {
    this.setState({ selected });
  };

  innerRef = (component: string, node: HTMLElement | null) => {
    if (node) {
      this.nodes[component] = node;
    }
  };

  searchInputRef = (node: HTMLInputElement | null) => {
    this.input = node;
  };

  renderResult = (component: ComponentResult) => (
    <SearchResult
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

  renderNoResults = () => (
    <div className="navbar-search-no-results">
      {translateWithParameters('no_results_for_x', this.state.query)}
    </div>
  );

  render() {
    const search = (
      <li className="navbar-search dropdown">
        <DeferredSpinner className="navbar-search-icon" loading={this.state.loading} />

        <SearchBox
          autoFocus={this.state.open}
          innerRef={this.searchInputRef}
          minLength={2}
          onChange={this.handleQueryChange}
          onFocus={this.handleFocus}
          onKeyDown={this.handleKeyDown}
          placeholder={translate('search.placeholder')}
          value={this.state.query}
        />

        {this.state.shortQuery && (
          <span className="navbar-search-input-hint">
            {translateWithParameters('select2.tooShort', 2)}
          </span>
        )}

        {this.state.open && Object.keys(this.state.results).length > 0 && (
          <DropdownOverlay noPadding={true}>
            <div className="global-navbar-search-dropdown" ref={node => (this.node = node)}>
              <SearchResults
                allowMore={this.state.query.length !== 1}
                loadingMore={this.state.loadingMore}
                more={this.state.more}
                onMoreClick={this.searchMore}
                onSelect={this.handleSelect}
                renderNoResults={this.renderNoResults}
                renderResult={this.renderResult}
                results={this.state.results}
                selected={this.state.selected}
              />
              <div className="dropdown-bottom-hint">
                <div className="pull-right">
                  <ClockIcon className="little-spacer-right" size={12} />
                  {translate('recently_browsed')}
                </div>
                <FormattedMessage
                  defaultMessage={translate('search.shortcut_hint')}
                  id="search.shortcut_hint"
                  values={{
                    shortcut: <span className="shortcut-button shortcut-button-small">s</span>
                  }}
                />
              </div>
            </div>
          </DropdownOverlay>
        )}
      </li>
    );

    return this.state.open ? (
      <OutsideClickHandler onClickOutside={this.handleClickOutside}>{search}</OutsideClickHandler>
    ) : (
      search
    );
  }
}

export default withRouter<OwnProps>(Search);
