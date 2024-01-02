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
import { debounce, uniqBy } from 'lodash';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { getSuggestions } from '../../../api/components';
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import FocusOutHandler from '../../../components/controls/FocusOutHandler';
import OutsideClickHandler from '../../../components/controls/OutsideClickHandler';
import SearchBox from '../../../components/controls/SearchBox';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import ClockIcon from '../../../components/icons/ClockIcon';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getKeyboardShortcutEnabled } from '../../../helpers/preferences';
import { scrollToElement } from '../../../helpers/scrolling';
import { getComponentOverviewUrl } from '../../../helpers/urls';
import { ComponentQualifier } from '../../../types/component';
import { Dict } from '../../../types/types';
import RecentHistory from '../RecentHistory';
import './Search.css';
import SearchResult from './SearchResult';
import SearchResults from './SearchResults';
import { ComponentResult, More, Results, sortQualifiers } from './utils';

interface Props {
  router: Router;
}
interface State {
  loading: boolean;
  loadingMore?: string;
  more: More;
  open: boolean;
  query: string;
  results: Results;
  selected?: string;
  shortQuery: boolean;
}

const MIN_SEARCH_QUERY_LENGTH = 2;

export class Search extends React.PureComponent<Props, State> {
  input?: HTMLInputElement | null;
  node?: HTMLElement | null;
  nodes: Dict<HTMLElement>;
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.nodes = {};
    this.search = debounce(this.search, 250);
    this.state = {
      loading: false,
      more: {},
      open: false,
      query: '',
      results: {},
      shortQuery: false,
    };
  }

  componentDidMount() {
    this.mounted = true;
    document.addEventListener('keydown', this.handleKeyDown);
    document.addEventListener('keydown', this.handleSKeyDown);
  }

  componentDidUpdate(_prevProps: Props, prevState: State) {
    if (prevState.selected !== this.state.selected) {
      this.scrollToSelected();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    document.removeEventListener('keydown', this.handleSKeyDown);
    document.removeEventListener('keydown', this.handleKeyDown);
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
        query: '',
        results: {},
        selected: undefined,
        shortQuery: false,
      });
    } else {
      this.setState({ open: false });
    }
  };

  getPlainComponentsList = (results: Results, more: More) =>
    sortQualifiers(Object.keys(results)).reduce((components, qualifier) => {
      const next = [...components, ...results[qualifier].map((component) => component.key)];
      if (more[qualifier]) {
        next.push('qualifier###' + qualifier);
      }
      return next;
    }, []);

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  search = (query: string) => {
    if (query.length === 0 || query.length >= MIN_SEARCH_QUERY_LENGTH) {
      this.setState({ loading: true });
      const recentlyBrowsed = RecentHistory.get().map((component) => component.key);
      getSuggestions(query, recentlyBrowsed).then((response) => {
        // compare `this.state.query` and `query` to handle two request done almost at the same time
        // in this case only the request that matches the current query should be taken
        if (this.mounted && this.state.query === query) {
          const results: Results = {};
          const more: More = {};
          this.nodes = {};
          response.results.forEach((group) => {
            results[group.q] = group.items.map((item) => ({ ...item, qualifier: group.q }));
            more[group.q] = group.more;
          });
          const list = this.getPlainComponentsList(results, more);
          this.setState({
            loading: false,
            more,
            results,
            selected: list.length > 0 ? list[0] : undefined,
            shortQuery:
              query.length > MIN_SEARCH_QUERY_LENGTH && response.warning === 'short_input',
          });
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
    const recentlyBrowsed = RecentHistory.get().map((component) => component.key);
    getSuggestions(query, recentlyBrowsed, qualifier).then((response) => {
      if (this.mounted) {
        const group = response.results.find((group) => group.q === qualifier);
        const moreResults = (group ? group.items : []).map((item) => ({ ...item, qualifier }));
        this.setState((state) => ({
          loading: false,
          loadingMore: undefined,
          more: { ...state.more, [qualifier]: 0 },
          results: {
            ...state.results,
            [qualifier]: uniqBy([...state.results[qualifier], ...moreResults], 'key'),
          },
          selected: moreResults.length > 0 ? moreResults[0].key : state.selected,
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
      }
      return null;
    });
  };

  selectNext = () => {
    this.setState(({ more, results, selected }) => {
      if (selected) {
        const list = this.getPlainComponentsList(results, more);
        const index = list.indexOf(selected);
        return index >= 0 && index < list.length - 1 ? { selected: list[index + 1] } : null;
      }
      return null;
    });
  };

  openSelected = () => {
    const { results, selected } = this.state;

    if (!selected) {
      return;
    }

    if (selected.startsWith('qualifier###')) {
      this.searchMore(selected.substr(12));
    } else {
      let qualifier = ComponentQualifier.Project;

      if ((results[ComponentQualifier.Portfolio] ?? []).find((r) => r.key === selected)) {
        qualifier = ComponentQualifier.Portfolio;
      } else if ((results[ComponentQualifier.SubPortfolio] ?? []).find((r) => r.key === selected)) {
        qualifier = ComponentQualifier.SubPortfolio;
      }

      this.props.router.push(getComponentOverviewUrl(selected, qualifier));

      this.closeSearch();
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

  handleSKeyDown = (event: KeyboardEvent) => {
    if (!getKeyboardShortcutEnabled() || isInput(event) || isShortcut(event)) {
      return true;
    }
    if (event.key === KeyboardKeys.KeyS) {
      event.preventDefault();
      this.focusInput();
      this.openSearch();
    }
  };

  handleKeyDown = (event: KeyboardEvent) => {
    if (!this.state.open) {
      return;
    }

    switch (event.key) {
      case KeyboardKeys.Enter:
        event.preventDefault();
        event.stopPropagation();
        this.openSelected();
        break;
      case KeyboardKeys.UpArrow:
        event.preventDefault();
        event.stopPropagation();
        this.selectPrevious();
        break;
      case KeyboardKeys.Escape:
        event.preventDefault();
        event.stopPropagation();
        this.closeSearch();
        break;
      case KeyboardKeys.DownArrow:
        event.preventDefault();
        event.stopPropagation();
        this.selectNext();
        break;
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
      component={component}
      innerRef={this.innerRef}
      key={component.key}
      onClose={this.closeSearch}
      onSelect={this.handleSelect}
      selected={this.state.selected === component.key}
    />
  );

  renderNoResults = () => (
    <div className="navbar-search-no-results" aria-live="assertive">
      {translateWithParameters('no_results_for_x', this.state.query)}
    </div>
  );

  render() {
    const search = (
      <div className="navbar-search dropdown">
        <DeferredSpinner className="navbar-search-icon" loading={this.state.loading} />

        <SearchBox
          autoFocus={this.state.open}
          innerRef={this.searchInputRef}
          minLength={2}
          onChange={this.handleQueryChange}
          onFocus={this.handleFocus}
          placeholder={translate('search.placeholder')}
          value={this.state.query}
        />

        {this.state.shortQuery && (
          <span className="navbar-search-input-hint" aria-live="assertive">
            {translateWithParameters('select2.tooShort', MIN_SEARCH_QUERY_LENGTH)}
          </span>
        )}

        {this.state.open && Object.keys(this.state.results).length > 0 && (
          <DropdownOverlay noPadding={true}>
            <div className="global-navbar-search-dropdown" ref={(node) => (this.node = node)}>
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
                <div className="pull-right" aria-hidden={true}>
                  <ClockIcon className="little-spacer-right" size={12} />
                  {translate('recently_browsed')}
                </div>
                <FormattedMessage
                  defaultMessage={translate('search.shortcut_hint')}
                  id="search.shortcut_hint"
                  values={{
                    shortcut: <span className="shortcut-button shortcut-button-small">s</span>,
                  }}
                />
              </div>
            </div>
          </DropdownOverlay>
        )}
      </div>
    );

    return this.state.open ? (
      <FocusOutHandler onFocusOut={this.handleClickOutside}>
        <OutsideClickHandler onClickOutside={this.handleClickOutside}>{search}</OutsideClickHandler>
      </FocusOutHandler>
    ) : (
      search
    );
  }
}

export default withRouter(Search);
