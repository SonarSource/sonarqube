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
import { sortBy, without } from 'lodash';
import * as classNames from 'classnames';
import FacetBox from './FacetBox';
import FacetHeader from './FacetHeader';
import FacetItem from './FacetItem';
import FacetItemsList from './FacetItemsList';
import ListStyleFacetFooter from './ListStyleFacetFooter';
import MultipleSelectionHint from './MultipleSelectionHint';
import { translate } from '../../helpers/l10n';
import DeferredSpinner from '../common/DeferredSpinner';
import { Paging } from '../../app/types';
import SearchBox from '../controls/SearchBox';
import ListFooter from '../controls/ListFooter';
import { formatMeasure } from '../../helpers/measures';
import MouseOverHandler from '../controls/MouseOverHandler';
import { queriesEqual, RawQuery } from '../../helpers/query';
import './ListStyleFacet.css';

export interface Props<S> {
  className?: string;
  facetHeader: string;
  fetching: boolean;
  getFacetItemText: (item: string) => string;
  getSearchResultKey: (result: S) => string;
  getSearchResultText: (result: S) => string;
  loadSearchResultCount?: (result: S) => Promise<number>;
  maxInitialItems?: number;
  maxItems?: number;
  minSearchLength?: number;
  onChange: (changes: { [x: string]: string | string[] }) => void;
  onClear?: () => void;
  onItemClick?: (itemValue: string, multiple: boolean) => void;
  onSearch: (
    query: string,
    page?: number
  ) => Promise<{ maxResults?: boolean; results: S[]; paging?: Paging }>;
  onToggle: (property: string) => void;
  open: boolean;
  property: string;
  query?: RawQuery;
  renderFacetItem: (item: string) => React.ReactNode;
  renderSearchResult: (result: S, query: string) => React.ReactNode;
  searchPlaceholder: string;
  getSortedItems?: () => string[];
  stats: { [x: string]: number } | undefined;
  values: string[];
}

interface State<S> {
  autoFocus: boolean;
  query: string;
  searching: boolean;
  searchMaxResults?: boolean;
  searchPaging?: Paging;
  searchResults?: S[];
  searchResultsCounts: { [key: string]: number };
  searchResultsCountLoading: { [key: string]: boolean };
  showFullList: boolean;
}

export default class ListStyleFacet<S> extends React.Component<Props<S>, State<S>> {
  mounted = false;

  static defaultProps = {
    maxInitialItems: 15,
    maxItems: 100
  };

  state: State<S> = {
    autoFocus: false,
    query: '',
    searching: false,
    searchResultsCounts: {},
    searchResultsCountLoading: {},
    showFullList: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentDidUpdate(prevProps: Props<S>) {
    // always remember issue counts from `stats`
    if (prevProps.stats !== this.props.stats) {
      this.setState(state => ({
        searchResultsCounts: {
          ...state.searchResultsCounts,
          ...this.props.stats
        }
      }));
    }

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
        searchResultsCountLoading: {},
        showFullList: false
      });
    } else if (
      prevProps.stats !== this.props.stats &&
      Object.keys(this.props.stats || {}).length < this.props.maxInitialItems!
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
    if (query.length >= 2) {
      this.setState({ query, searching: true });
      this.props.onSearch(query).then(({ maxResults, paging, results }) => {
        if (this.mounted) {
          this.setState({
            searching: false,
            searchMaxResults: maxResults,
            searchResults: results,
            searchPaging: paging
          });
        }
      }, this.stopSearching);
    } else {
      this.setState({ query, searching: false, searchResults: [] });
    }
  };

  searchMore = () => {
    const { query, searchPaging, searchResults } = this.state;
    if (query && searchResults && searchPaging) {
      this.setState({ searching: true });
      this.props.onSearch(query, searchPaging.pageIndex + 1).then(({ paging, results }) => {
        if (this.mounted) {
          this.setState({
            searching: false,
            searchResults: [...searchResults, ...results],
            searchPaging: paging
          });
        }
      }, this.stopSearching);
    }
  };

  handleSearchResultMouseOver = (result: S) => {
    if (
      this.props.loadSearchResultCount &&
      this.state.searchResultsCounts[this.props.getSearchResultKey(result)] === undefined
    ) {
      this.setState(state => ({
        searchResultsCountLoading: {
          ...state.searchResultsCountLoading,
          [this.props.getSearchResultKey(result)]: true
        }
      }));

      this.props.loadSearchResultCount(result).then(
        count => {
          if (this.mounted) {
            this.setState(state => ({
              searchResultsCounts: {
                ...state.searchResultsCounts,
                [this.props.getSearchResultKey(result)]: count
              },
              searchResultsCountLoading: {
                ...state.searchResultsCountLoading,
                [this.props.getSearchResultKey(result)]: false
              }
            }));
          }
        },
        () => {
          if (this.mounted) {
            this.setState(state => ({
              searchResultsCountLoading: {
                ...state.searchResultsCountLoading,
                [this.props.getSearchResultKey(result)]: false
              }
            }));
          }
        }
      );
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

  getLastActiveIndex = (list: string[]) => {
    for (let i = list.length - 1; i >= 0; i--) {
      if (this.props.values.includes(list[i])) {
        return i;
      }
    }
    return 0;
  };

  renderList() {
    const { stats } = this.props;

    if (!stats) {
      return null;
    }

    const sortedItems = this.props.getSortedItems
      ? this.props.getSortedItems()
      : sortBy(Object.keys(stats), key => -stats[key], key => this.props.getFacetItemText(key));

    // limit the number of items to this.props.maxInitialItems,
    // but make sure all (in other words, the last) selected items are displayed
    const lastSelectedIndex = this.getLastActiveIndex(sortedItems);
    const countToDisplay = Math.max(this.props.maxInitialItems!, lastSelectedIndex + 1);
    const limitedList = this.state.showFullList
      ? sortedItems
      : sortedItems.slice(0, countToDisplay);

    const mightHaveMoreResults = sortedItems.length >= this.props.maxItems!;

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
        <ListStyleFacetFooter
          count={limitedList.length}
          showLess={this.state.showFullList ? this.hideFullList : undefined}
          showMore={this.showFullList}
          total={sortedItems.length}
        />
        {mightHaveMoreResults &&
          this.state.showFullList && (
            <div className="alert alert-warning spacer-top">
              {translate('facet_might_have_more_results')}
            </div>
          )}
      </>
    );
  }

  renderSearch() {
    if (!this.props.stats || !Object.keys(this.props.stats).length) {
      return null;
    }

    const { minSearchLength = 2 } = this.props;

    return (
      <SearchBox
        autoFocus={this.state.autoFocus}
        className="little-spacer-top spacer-bottom"
        loading={this.state.searching}
        minLength={minSearchLength}
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
          <div className="alert alert-warning spacer-top">
            {translate('facet_might_have_more_results')}
          </div>
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

    // default to 0 if we're sure there are not more results
    const isFacetExhaustive = Object.keys(this.props.stats || {}).length < this.props.maxItems!;

    let stat: number | undefined = this.getStat(key);
    let disabled = isFacetExhaustive && !active && stat === 0;
    if (stat === undefined) {
      stat = this.state.searchResultsCounts[key];
      disabled = false; // do not disable facet if the count was requested after mouse over
    }
    if (stat === undefined && isFacetExhaustive) {
      stat = 0;
      disabled = !active;
    }

    const loading = this.state.searchResultsCountLoading[key];
    const canBeLoaded =
      !loading && this.props.loadSearchResultCount !== undefined && stat === undefined;

    const facetItem = (
      <FacetItem
        active={active}
        className={classNames({ 'list-style-facet-mouse-over-animation': canBeLoaded })}
        disabled={disabled}
        loading={loading}
        name={this.props.renderSearchResult(result, this.state.query)}
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat)}
        tooltip={this.props.getSearchResultText(result)}
        value={key}
      />
    );

    return (
      <React.Fragment key={key}>
        {canBeLoaded ? (
          <MouseOverHandler delay={500} onOver={() => this.handleSearchResultMouseOver(result)}>
            {facetItem}
          </MouseOverHandler>
        ) : (
          facetItem
        )}
      </React.Fragment>
    );
  }

  render() {
    const { stats = {} } = this.props;
    const { query, searching, searchResults } = this.state;
    const values = this.props.values.map(item => this.props.getFacetItemText(item));
    const loadingResults =
      query !== '' && searching && (searchResults === undefined || searchResults.length === 0);
    const showList = !query || loadingResults;
    return (
      <FacetBox className={this.props.className} property={this.props.property}>
        <FacetHeader
          name={this.props.facetHeader}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        <DeferredSpinner loading={this.props.fetching} />
        {this.props.open && (
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
