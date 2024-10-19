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
import { ButtonIcon, ButtonVariety, IconSearch } from '@sonarsource/echoes-react';
import { DropdownMenu, InputSearch, Popup, PopupZLevel, TextMuted } from 'design-system';
import { debounce, isEmpty, uniqBy } from 'lodash';
import * as React from 'react';
import { withRouter } from '~sonar-aligned/components/hoc/withRouter';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { Router } from '~sonar-aligned/types/router';
import { getSuggestions } from '../../../api/components';
import FocusOutHandler from '../../../components/controls/FocusOutHandler';
import OutsideClickHandler from '../../../components/controls/OutsideClickHandler';
import { PopupPlacement } from '../../../components/ui/popups';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getKeyboardShortcutEnabled } from '../../../helpers/preferences';
import { getComponentOverviewUrl } from '../../../helpers/urls';
import { Dict } from '../../../types/types';
import RecentHistory from '../RecentHistory';
import GlobalSearchResult from './GlobalSearchResult';
import GlobalSearchResults from './GlobalSearchResults';
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
}
const MIN_SEARCH_QUERY_LENGTH = 2;

export class GlobalSearch extends React.PureComponent<Props, State> {
  input?: HTMLInputElement | null;
  node?: HTMLElement | null;
  nodes: Dict<HTMLElement | undefined>;
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
    };
  }

  componentDidMount() {
    this.mounted = true;
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
      document.documentElement.click();
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
      });
    } else {
      this.setState({ open: false });
    }
  };

  getPlainComponentsList = (results: Results, more: More) =>
    sortQualifiers(Object.keys(results)).reduce((components, qualifier) => {
      const next = [...components, ...(results[qualifier] ?? []).map((component) => component.key)];
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
            [qualifier]: uniqBy([...(state.results[qualifier] ?? []), ...moreResults], 'key'),
          },
          selected: moreResults.length > 0 ? moreResults[0].key : state.selected,
        }));

        this.focusInput();
      }
    }, this.stopLoading);
  };

  handleQueryChange = (query: string) => {
    this.setState({ query });
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
      this.searchMore(selected.substring('qualifier###'.length));
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
        node.scrollIntoView({
          block: 'center',
          behavior: 'smooth',
        });
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

  handleKeyDown = (event: React.KeyboardEvent) => {
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
    <GlobalSearchResult
      component={component}
      innerRef={this.innerRef}
      key={component.key}
      onClose={this.closeSearch}
      onSelect={this.handleSelect}
      selected={this.state.selected === component.key}
    />
  );

  renderNoResults = () => (
    <div className="sw-px-3 sw-py-2" aria-live="assertive">
      {translateWithParameters('no_results_for_x', this.state.query)}
    </div>
  );

  render() {
    const { open, query, results, more, loadingMore, selected, loading } = this.state;

    const list = this.getPlainComponentsList(results, more);

    const search = (
      <div className="sw-min-w-abs-200 sw-max-w-abs-350 sw-w-full">
        <Popup
          allowResizing
          overlay={
            open && (
              <DropdownMenu
                className="it__global-navbar-search-dropdown sw-overflow-y-auto sw-overflow-x-hidden"
                maxHeight="38rem"
                innerRef={(node: HTMLUListElement | null) => (this.node = node)}
                size="auto"
                aria-owns="global-search-input"
              >
                <GlobalSearchResults
                  query={query}
                  loadingMore={loadingMore}
                  more={more}
                  onMoreClick={this.searchMore}
                  onSelect={this.handleSelect}
                  renderNoResults={this.renderNoResults}
                  renderResult={this.renderResult}
                  results={results}
                  selected={selected}
                />
                {list.length > 0 && (
                  <li className="sw-px-3 sw-pt-1">
                    <TextMuted text={translate('global_search.shortcut_hint')} />
                  </li>
                )}
              </DropdownMenu>
            )
          }
          placement={PopupPlacement.BottomLeft}
          zLevel={PopupZLevel.Global}
        >
          <InputSearch
            id="global-search-input"
            className="sw-w-full"
            autoFocus={open}
            innerRef={this.searchInputRef}
            loading={loading}
            minLength={MIN_SEARCH_QUERY_LENGTH}
            onChange={this.handleQueryChange}
            onFocus={this.handleFocus}
            onKeyDown={this.handleKeyDown}
            placeholder={translate('search.search_for_projects')}
            size="auto"
            value={query}
            searchInputAriaLabel={translate('search_verb')}
          />
        </Popup>
      </div>
    );

    return (
      <form role="search">
        {!open && isEmpty(query) ? (
          <ButtonIcon
            Icon={IconSearch}
            ariaLabel={translate('search_verb')}
            className="it__search-icon"
            onClick={this.handleFocus}
            variety={ButtonVariety.DefaultGhost}
          />
        ) : (
          <FocusOutHandler onFocusOut={this.handleClickOutside}>
            <OutsideClickHandler onClickOutside={this.handleClickOutside}>
              {search}
            </OutsideClickHandler>
          </FocusOutHandler>
        )}
      </form>
    );
  }
}

export default withRouter(GlobalSearch);
