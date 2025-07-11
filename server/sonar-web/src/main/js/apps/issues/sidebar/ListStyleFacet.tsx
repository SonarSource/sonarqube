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

import { max, sortBy, values, without } from 'lodash';
import * as React from 'react';
import { FacetBox, FacetItem, FlagMessage, InputSearch, Note } from '~design-system';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricType } from '~sonar-aligned/types/metrics';
import { RawQuery } from '~sonar-aligned/types/router';
import ListFooter from '../../../components/controls/ListFooter';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { queriesEqual } from '../../../helpers/query';
import { isDefined } from '../../../helpers/types';
import { Dict, Paging } from '../../../types/types';
import { FacetItemsList } from './FacetItemsList';
import { ListStyleFacetFooter } from './ListStyleFacetFooter';
import { MultipleSelectionHint } from './MultipleSelectionHint';

interface SearchResponse<S> {
  maxResults?: boolean;
  paging?: Paging;
  results: S[];
}

export interface Props<S> {
  disableZero?: boolean;
  disabled?: boolean;
  disabledHelper?: string;
  facetHeader: string;
  fetching: boolean;
  getFacetItemText: (item: string) => string;
  getSearchResultKey: (result: S) => string;
  getSearchResultText: (result: S) => string;
  getSortedItems?: () => string[];
  inner?: boolean;
  loadSearchResultCount?: (result: S[]) => Promise<Dict<number>>;
  maxInitialItems: number;
  maxItems: number;
  minSearchLength: number;
  onChange: (changes: Dict<string | string[]>) => void;
  onClear?: () => void;
  onItemClick?: (itemValue: string, multiple: boolean) => void;
  onSearch: (query: string, page?: number) => Promise<SearchResponse<S>>;
  onToggle?: (property: string) => void;
  open: boolean;
  property: string;
  query?: RawQuery;
  renderFacetItem: (item: string) => string | JSX.Element;
  renderSearchResult: (result: S, query: string) => React.ReactNode;
  searchInputAriaLabel?: string;
  searchPlaceholder: string;
  showLessAriaLabel?: string;
  showMoreAriaLabel?: string;
  showStatBar?: boolean;
  stats: Dict<number> | undefined;
  values: string[];
  ShowSearch?: string;
}

interface State<S> {
  autoFocus: boolean;
  query: string;
  searchMaxResults?: boolean;
  searchPaging?: Paging;
  searchResults?: S[];
  searchResultsCounts: Dict<number>;
  searching: boolean;
  showFullList: boolean;
}

export class ListStyleFacet<S> extends React.Component<Props<S>, State<S>> {
  
  mounted = false;

  static defaultProps = {
    getFacetItemText: (item: string) => item,
    getSearchResultKey: (result: unknown) => result,
    getSearchResultText: (result: unknown) => result,
    maxInitialItems: 15,
    maxItems: 100,
    minSearchLength: 2,
    renderFacetItem: (item: string) => item,
    renderSearchResult: (result: unknown, _query: string) => result,
  };

  state: State<S> = {
    autoFocus: false,
    query: '',
    searching: false,
    searchResultsCounts: {},
    showFullList: false,
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
        showFullList: false,
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
          values.includes(itemValue) ? without(values, itemValue) : [...values, itemValue],
        );

        this.props.onChange({ [this.props.property]: newValue });
      } else {
        this.props.onChange({
          [this.props.property]: values.includes(itemValue) && values.length < 2 ? [] : [itemValue],
        });
      }
    }
  };

  handleHeaderClick = () => {
    this.props.onToggle?.(this.props.property);
  };

  handleClear = () => {
    if (this.props.onClear) {
      this.props.onClear();
    } else {
      this.props.onChange({ [this.props.property]: [] });
    }
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
            this.setState((state) => ({
              searching: false,
              searchMaxResults: maxResults,
              searchResults: results,
              searchPaging: paging,
              searchResultsCounts: { ...state.searchResultsCounts, ...stats },
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
            this.setState((state) => ({
              searching: false,
              searchResults: [...searchResults, ...results],
              searchPaging: paging,
              searchResultsCounts: { ...state.searchResultsCounts, ...stats },
            }));
          }
        })
        .catch(this.stopSearching);
    }
  };

  loadCountsForSearchResults = (response: SearchResponse<S>) => {
    const { loadSearchResultCount = () => Promise.resolve({}) } = this.props;

    const resultsToLoad = response.results.filter((result) => {
      const key = this.props.getSearchResultKey(result);

      return this.getStat(key) === undefined && this.state.searchResultsCounts[key] === undefined;
    });

    if (resultsToLoad.length > 0) {
      return loadSearchResultCount(resultsToLoad).then((stats) => ({ ...response, stats }));
    }

    return { ...response, stats: {} };
  };

  getStat(item: string) {
    const { stats } = this.props;

    return stats?.[item];
  }

  getStatBarPercent = (item: string) => {
    const { showStatBar, stats } = this.props;
    const { searchResultsCounts } = this.state;

    if (!showStatBar) {
      return undefined;
    }

    const combined = { ...stats, ...searchResultsCounts };

    const maxFacetValue = max(values(combined));
    const facetValue = combined?.[item];

    return isDefined(facetValue) && isDefined(maxFacetValue) && maxFacetValue > 0
      ? facetValue / maxFacetValue
      : undefined;
  };

  getFacetHeaderId = (property: string) => {
    return `facet_${property}`;
  };

  showFullList = () => {
    this.setState({ showFullList: true });
  };

  hideFullList = () => {
    this.setState({ showFullList: false });
  };

  renderList() {
    const {
      disableZero,
      maxInitialItems,
      maxItems,
      property,
      stats,
      showMoreAriaLabel,
      showLessAriaLabel,
      values,
      facetHeader,
    } = this.props;

    if (!stats) {
      return null;
    }

    const sortedItems = this.props.getSortedItems
      ? this.props.getSortedItems()
      : sortBy(
          Object.keys(stats),
          (key) => -stats[key],
          (key) => this.props.getFacetItemText(key),
        );

    const limitedList = this.state.showFullList
      ? sortedItems
      : sortedItems.slice(0, maxInitialItems);

    // make sure all selected items are displayed
    const selectedBelowLimit = this.state.showFullList
      ? []
      : sortedItems.slice(maxInitialItems).filter((item) => values.includes(item));

    const mightHaveMoreResults = sortedItems.length >= maxItems;

    return (
      <>
        <FacetItemsList labelledby={this.getFacetHeaderId(property)}>
          {limitedList.map((item) => (
            <FacetItem
              active={this.props.values.includes(item)}
              disableZero={disableZero}
              className="it__search-navigator-facet"
              key={item}
              name={this.props.renderFacetItem(item)}
              onClick={this.handleItemClick}
              stat={formatFacetStat(this.getStat(item)) ?? 0}
              statBarPercent={this.getStatBarPercent(item)}
              tooltip={this.props.getFacetItemText(item)}
              value={item}
            />
          ))}
        </FacetItemsList>

        {selectedBelowLimit.length > 0 && (
          <>
            <Note as="div" className="sw-mb-2 sw-text-center">
              ⋯
            </Note>

            <FacetItemsList labelledby={this.getFacetHeaderId(property)}>
              {selectedBelowLimit.map((item) => (
                <FacetItem
                  active
                  className="it__search-navigator-facet"
                  disableZero={disableZero}
                  key={item}
                  name={this.props.renderFacetItem(item)}
                  onClick={this.handleItemClick}
                  stat={formatFacetStat(this.getStat(item)) ?? 0}
                  statBarPercent={this.getStatBarPercent(item)}
                  tooltip={this.props.getFacetItemText(item)}
                  value={item}
                />
              ))}
            </FacetItemsList>
          </>
        )}

        <ListStyleFacetFooter
          nbShown={limitedList.length + selectedBelowLimit.length}
          showLess={this.state.showFullList ? this.hideFullList : undefined}
          showMoreAriaLabel={
            showMoreAriaLabel ?? translateWithParameters('show_more_filter_x', facetHeader)
          }
          showLessAriaLabel={
            showLessAriaLabel ?? translateWithParameters('show_less_filter_x', facetHeader)
          }
          showMore={this.showFullList}
          total={sortedItems.length}
        />

        {mightHaveMoreResults && this.state.showFullList && (
          <FlagMessage className="sw-flex sw-my-4" variant="warning">
            {translate('facet_might_have_more_results')}
          </FlagMessage>
        )}
      </>
    );
  }

  renderSearch() {
    const { minSearchLength, searchInputAriaLabel } = this.props;
    return (
      <InputSearch
        className="it__search-box-input sw-mb-4 sw-w-full"
        autoFocus={this.state.autoFocus}
        onChange={this.search}
        placeholder={this.props.searchPlaceholder}
        size="auto"
        value={this.state.query}
        searchInputAriaLabel={searchInputAriaLabel ?? translate('search_verb')}
        minLength={minSearchLength}
      />
    );
  }

  renderSearchResults() {
    const { property, showMoreAriaLabel } = this.props;
    const { searching, searchMaxResults, searchResults, searchPaging } = this.state;

    if (!searching && !searchResults?.length) {
      return <div className="sw-mb-2">{translate('no_results')}</div>;
    }

    if (!searchResults) {
      // initial search
      return null;
    }

    return (
      <>
        <FacetItemsList labelledby={this.getFacetHeaderId(property)}>
          {searchResults.map((result) => this.renderSearchResult(result))}
        </FacetItemsList>

        {searchMaxResults && (
          <FlagMessage className="sw-flex sw-my-4" variant="warning">
            {translate('facet_might_have_more_results')}
          </FlagMessage>
        )}

        {searchPaging && (
          <ListFooter
            className="sw-mb-2"
            count={searchResults.length}
            loadMore={this.searchMore}
            loadMoreAriaLabel={showMoreAriaLabel}
            ready={!searching}
            total={searchPaging.total}
          />
        )}
      </>
    );
  }

  renderSearchResult(result: S) {
    const { disableZero } = this.props;
    const key = this.props.getSearchResultKey(result);
    const active = this.props.values.includes(key);
    const stat = formatFacetStat(this.getStat(key) ?? this.state.searchResultsCounts[key]) ?? 0;

    return (
      <FacetItem
        active={active}
        className="it__search-navigator-facet"
        disableZero={disableZero}
        key={key}
        name={this.props.renderSearchResult(result, this.state.query)}
        onClick={this.handleItemClick}
        stat={stat}
        statBarPercent={this.getStatBarPercent(key)}
        tooltip={this.props.getSearchResultText(result)}
        value={key}
      />
    );
  }

  render() {
    const {
      disabled,
      disabledHelper,
      facetHeader,
      fetching,
      inner,
      minSearchLength,
      open,
      property,
      stats = {},
      values: propsValues,
    } = this.props;

    const { query, searching, searchResults } = this.state;

    const values = propsValues.map((item) => this.props.getFacetItemText(item));

    const loadingResults =
      query !== '' && searching && (searchResults === undefined || searchResults.length === 0);

    const showList = query.length < minSearchLength || loadingResults;

    const nbSelectableItems = Object.keys(stats).length;
    const nbSelectedItems = values.length;

    return (
      <FacetBox
        className="it__search-navigator-facet-box it__search-navigator-facet-header"
        count={nbSelectedItems}
        countLabel={translateWithParameters('x_selected', nbSelectedItems)}
        data-property={property}
        disabled={disabled}
        disabledHelper={disabledHelper}
        tooltipComponent={Tooltip}
        id={this.getFacetHeaderId(property)}
        inner={inner}
        loading={fetching}
        name={facetHeader}
        onClear={this.handleClear}
        onClick={disabled || !this.props.onToggle ? undefined : this.handleHeaderClick}
        open={open && !disabled}
      >
        {!disabled && (
          <span className="it__search-navigator-facet-list">
            {this.props.ShowSearch===undefined && this.renderSearch()}

            <output role={query ? 'status' : ''}>
              {showList ? this.renderList() : this.renderSearchResults()}
            </output>

            <MultipleSelectionHint
              nbSelectableItems={nbSelectableItems}
              nbSelectedItems={nbSelectedItems}
            />
          </span>
        )}
      </FacetBox>
    );
  }
}

function formatFacetStat(stat: number | undefined) {
  return stat && formatMeasure(stat, MetricType.ShortInteger);
}
