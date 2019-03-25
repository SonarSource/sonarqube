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
import * as classNames from 'classnames';
import { sortBy, without } from 'lodash';
import FacetBox from './FacetBox';
import FacetHeader from './FacetHeader';
import FacetItem from './FacetItem';
import FacetItemsList from './FacetItemsList';
import ListStyleFacetFooter from './ListStyleFacetFooter';
import MultipleSelectionHint from './MultipleSelectionHint';
import { Alert } from '../ui/Alert';
import DeferredSpinner from '../common/DeferredSpinner';
import ListFooter from '../controls/ListFooter';
import SearchBox from '../controls/SearchBox';
import Tooltip from '../controls/Tooltip';
import { formatMeasure } from '../../helpers/measures';
import { queriesEqual, RawQuery } from '../../helpers/query';
import { translate } from '../../helpers/l10n';

interface SearchResponse<S> {
  maxResults?: boolean;
  results: S[];
  paging?: T.Paging;
}

export interface Props<S> {
  className?: string;
  disabled?: boolean;
  disabledHelper?: string;
  facetHeader: string;
  fetching: boolean;
  getFacetItemText: (item: string) => string;
  getSearchResultKey: (result: S) => string;
  getSearchResultText: (result: S) => string;
  loadSearchResultCount?: (result: S[]) => Promise<T.Dict<number>>;
  maxInitialItems: number;
  maxItems: number;
  minSearchLength: number;
  onChange: (changes: T.Dict<string | string[]>) => void;
  onClear?: () => void;
  onItemClick?: (itemValue: string, multiple: boolean) => void;
  onSearch: (query: string, page?: number) => Promise<SearchResponse<S>>;
  onToggle: (property: string) => void;
  open: boolean;
  property: string;
  query?: RawQuery;
  renderFacetItem: (item: string) => React.ReactNode;
  renderSearchResult: (result: S, query: string) => React.ReactNode;
  searchPlaceholder: string;
  getSortedItems?: () => string[];
  stats: T.Dict<number> | undefined;
  values: string[];
}

interface State<S> {
  autoFocus: boolean;
  query: string;
  searching: boolean;
  searchMaxResults?: boolean;
  searchPaging?: T.Paging;
  searchResults?: S[];
  searchResultsCounts: T.Dict<number>;
  showFullList: boolean;
}

export default class ListStyleFacet<S> extends React.Component<Props<S>, State<S>> {
  mounted = false;

  static defaultProps = {
    maxInitialItems: 15,
    maxItems: 100,
    minSearchLength: 2
  };

  state: State<S> = {
    autoFocus: false,
    query: '',
    searching: false,
    searchResultsCounts: {},
    showFullList: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentDidUpdate(prevProps: Props<S>) {
    if (!prevProps.open && this.props.open) {
      // focus search field *only* if it was manually open
      this.setState({ autoFocus: true });
    } else if (
      (prevProps.open && !this.props.open) ||
      !queriesEqual(prevProps.query || {}, this.props.query || {})
    ) {
      // reset state when closing the facet, or when query changes
      this.setState({
        query: '',
        searchMaxResults: undefined,
        searchResults: undefined,
        searching: false,
        searchResultsCounts: {},
        showFullList: false
      });
    } else if (
      prevProps.stats !== this.props.stats &&
      Object.keys(this.props.stats || {}).length < this.props.maxInitialItems
    ) {
      // show limited list if `stats` changed and there are less than 15 items
      this.setState({ showFullList: false });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleItemClick = (itemValue: string, multiple: boolean) => {
    if (this.props.onItemClick) {
      this.props.onItemClick(itemValue, multiple);
    } else {
      const { values } = this.props;
      if (multiple) {
        const newValue = sortBy(
          values.includes(itemValue) ? without(values, itemValue) : [...values, itemValue]
        );
        this.props.onChange({ [this.props.property]: newValue });
      } else {
        this.props.onChange({
          [this.props.property]: values.includes(itemValue) && values.length < 2 ? [] : [itemValue]
        });
      }
    }
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.props.property);
  };

  handleClear = () => {
    if (this.props.onClear) {
      this.props.onClear();
    } else this.props.onChange({ [this.props.property]: [] });
  };

  stopSearching = () => {
    if (this.mounted) {
      this.setState({ searching: false });
    }
  };

  search = (query: string) => {
    if (query.length >= this.props.minSearchLength) {
      this.setState({ query, searching: true });
      this.props
        .onSearch(query)
        .then(this.loadCountsForSearchResults)
        .then(({ maxResults, paging, results, stats }) => {
          if (this.mounted) {
            this.setState(state => ({
              searching: false,
              searchMaxResults: maxResults,
              searchResults: results,
              searchPaging: paging,
              searchResultsCounts: { ...state.searchResultsCounts, ...stats }
            }));
          }
        })
        .catch(this.stopSearching);
    } else {
      this.setState({ query, searching: false, searchResults: [] });
    }
  };

  searchMore = () => {
    const { query, searchPaging, searchResults } = this.state;
    if (query && searchResults && searchPaging) {
      this.setState({ searching: true });
      this.props
        .onSearch(query, searchPaging.pageIndex + 1)
        .then(this.loadCountsForSearchResults)
        .then(({ paging, results, stats }) => {
          if (this.mounted) {
            this.setState(state => ({
              searching: false,
              searchResults: [...searchResults, ...results],
              searchPaging: paging,
              searchResultsCounts: { ...state.searchResultsCounts, ...stats }
            }));
          }
        })
        .catch(this.stopSearching);
    }
  };

  loadCountsForSearchResults = (response: SearchResponse<S>) => {
    const { loadSearchResultCount = () => Promise.resolve({}) } = this.props;
    const resultsToLoad = response.results.filter(result => {
      const key = this.props.getSearchResultKey(result);
      return this.getStat(key) === undefined && this.state.searchResultsCounts[key] === undefined;
    });
    if (resultsToLoad.length > 0) {
      return loadSearchResultCount(resultsToLoad).then(stats => ({ ...response, stats }));
    } else {
      return { ...response, stats: {} };
    }
  };

  getStat(item: string) {
    const { stats } = this.props;
    return stats && stats[item] !== undefined ? stats && stats[item] : undefined;
  }

  showFullList = () => {
    this.setState({ showFullList: true });
  };

  hideFullList = () => {
    this.setState({ showFullList: false });
  };

  renderList() {
    const { stats } = this.props;

    if (!stats) {
      return null;
    }

    const sortedItems = this.props.getSortedItems
      ? this.props.getSortedItems()
      : sortBy(Object.keys(stats), key => -stats[key], key => this.props.getFacetItemText(key));

    const limitedList = this.state.showFullList
      ? sortedItems
      : sortedItems.slice(0, this.props.maxInitialItems);

    // make sure all selected items are displayed
    const selectedBelowLimit = this.state.showFullList
      ? []
      : sortedItems
          .slice(this.props.maxInitialItems)
          .filter(item => this.props.values.includes(item));

    const mightHaveMoreResults = sortedItems.length >= this.props.maxItems;

    return (
      <>
        <FacetItemsList>
          {limitedList.map(item => (
            <FacetItem
              active={this.props.values.includes(item)}
              key={item}
              name={this.props.renderFacetItem(item)}
              onClick={this.handleItemClick}
              stat={formatFacetStat(this.getStat(item))}
              tooltip={this.props.getFacetItemText(item)}
              value={item}
            />
          ))}
        </FacetItemsList>
        {selectedBelowLimit.length > 0 && (
          <>
            <div className="note spacer-bottom text-center">⋯</div>
            <FacetItemsList>
              {selectedBelowLimit.map(item => (
                <FacetItem
                  active={true}
                  key={item}
                  name={this.props.renderFacetItem(item)}
                  onClick={this.handleItemClick}
                  stat={formatFacetStat(this.getStat(item))}
                  tooltip={this.props.getFacetItemText(item)}
                  value={item}
                />
              ))}
            </FacetItemsList>
          </>
        )}
        <ListStyleFacetFooter
          count={limitedList.length + selectedBelowLimit.length}
          showLess={this.state.showFullList ? this.hideFullList : undefined}
          showMore={this.showFullList}
          total={sortedItems.length}
        />
        {mightHaveMoreResults && this.state.showFullList && (
          <Alert className="spacer-top" variant="warning">
            {translate('facet_might_have_more_results')}
          </Alert>
        )}
      </>
    );
  }

  renderSearch() {
    if (!this.props.stats || !Object.keys(this.props.stats).length) {
      return null;
    }

    return (
      <SearchBox
        autoFocus={this.state.autoFocus}
        className="little-spacer-top spacer-bottom"
        loading={this.state.searching}
        minLength={this.props.minSearchLength}
        onChange={this.search}
        placeholder={this.props.searchPlaceholder}
        value={this.state.query}
      />
    );
  }

  renderSearchResults() {
    const { searching, searchMaxResults, searchResults, searchPaging } = this.state;

    if (!searching && (!searchResults || !searchResults.length)) {
      return <div className="note spacer-bottom">{translate('no_results')}</div>;
    }

    if (!searchResults) {
      // initial search
      return null;
    }

    return (
      <>
        <FacetItemsList>
          {searchResults.map(result => this.renderSearchResult(result))}
        </FacetItemsList>
        {searchMaxResults && (
          <Alert className="spacer-top" variant="warning">
            {translate('facet_might_have_more_results')}
          </Alert>
        )}
        {searchPaging && (
          <ListFooter
            className="spacer-bottom"
            count={searchResults.length}
            loadMore={this.searchMore}
            ready={!searching}
            total={searchPaging.total}
          />
        )}
      </>
    );
  }

  renderSearchResult(result: S) {
    const key = this.props.getSearchResultKey(result);
    const active = this.props.values.includes(key);
    const stat = this.getStat(key) || this.state.searchResultsCounts[key];
    const disabled = !active && stat === 0;
    return (
      <FacetItem
        active={active}
        disabled={disabled}
        key={key}
        name={this.props.renderSearchResult(result, this.state.query)}
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat)}
        tooltip={this.props.getSearchResultText(result)}
        value={key}
      />
    );
  }

  render() {
    const { disabled, stats = {} } = this.props;
    const { query, searching, searchResults } = this.state;
    const values = this.props.values.map(item => this.props.getFacetItemText(item));
    const loadingResults =
      query !== '' && searching && (searchResults === undefined || searchResults.length === 0);
    const showList = !query || loadingResults;
    return (
      <FacetBox
        className={classNames(this.props.className, {
          'search-navigator-facet-box-forbidden': disabled
        })}
        property={this.props.property}>
        <FacetHeader
          name={
            <Tooltip overlay={disabled ? this.props.disabledHelper : undefined}>
              <span>{this.props.facetHeader}</span>
            </Tooltip>
          }
          onClear={this.handleClear}
          onClick={disabled ? undefined : this.handleHeaderClick}
          open={this.props.open && !disabled}
          values={values}
        />

        <DeferredSpinner loading={this.props.fetching} />
        {this.props.open && !disabled && (
          <>
            {this.renderSearch()}
            {showList ? this.renderList() : this.renderSearchResults()}
            <MultipleSelectionHint options={Object.keys(stats).length} values={values.length} />
          </>
        )}
      </FacetBox>
    );
  }
}

function formatFacetStat(stat: number | undefined) {
  return stat && formatMeasure(stat, 'SHORT_INT');
}
