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
import FacetBox from './FacetBox';
import FacetHeader from './FacetHeader';
import FacetItem from './FacetItem';
import FacetItemsList from './FacetItemsList';
import MultipleSelectionHint from './MultipleSelectionHint';
import { translate } from '../../helpers/l10n';
import DeferredSpinner from '../common/DeferredSpinner';
import { Paging } from '../../app/types';
import SearchBox from '../controls/SearchBox';
import ListFooter from '../controls/ListFooter';
import { formatMeasure } from '../../helpers/measures';

export interface Props<S> {
  facetHeader: string;
  fetching: boolean;
  getFacetItemText: (item: string) => string;
  getSearchResultKey: (result: S) => string;
  getSearchResultText: (result: S) => string;
  loading?: boolean;
  onChange: (changes: { [x: string]: string | string[] }) => void;
  onSearch: (query: string, page?: number) => Promise<{ results: S[]; paging: Paging }>;
  onToggle: (property: string) => void;
  open: boolean;
  property: string;
  renderFacetItem: (item: string) => React.ReactNode;
  renderSearchResult: (result: S, query: string) => React.ReactNode;
  searchPlaceholder: string;
  values: string[];
  stats: { [x: string]: number } | undefined;
}

interface State<S> {
  autoFocus: boolean;
  query: string;
  searching: boolean;
  searchResults?: S[];
  searchPaging?: Paging;
}

export default class ListStyleFacet<S> extends React.Component<Props<S>, State<S>> {
  mounted = false;

  state: State<S> = {
    autoFocus: false,
    query: '',
    searching: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentDidUpdate(prevProps: Props<S>) {
    // focus search field *only* if it was manually open
    if (!prevProps.open && this.props.open) {
      this.setState({ autoFocus: true });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleItemClick = (itemValue: string, multiple: boolean) => {
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
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.props.property);
  };

  handleClear = () => {
    this.props.onChange({ [this.props.property]: [] });
  };

  stopSearching = () => {
    if (this.mounted) {
      this.setState({ searching: false });
    }
  };

  search = (query: string) => {
    if (query.length >= 2) {
      this.setState({ query, searching: true });
      this.props.onSearch(query).then(({ paging, results }) => {
        if (this.mounted) {
          this.setState({ searching: false, searchResults: results, searchPaging: paging });
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

  getStat(item: string) {
    const { stats } = this.props;
    return stats ? stats[item] : undefined;
  }

  renderList() {
    const { stats } = this.props;

    if (!stats) {
      return null;
    }

    const items = sortBy(
      Object.keys(stats),
      key => -stats[key],
      key => this.props.getFacetItemText(key)
    );

    return (
      <FacetItemsList>
        {items.map(item => (
          <FacetItem
            active={this.props.values.includes(item)}
            key={item}
            loading={this.props.loading}
            name={this.props.renderFacetItem(item)}
            onClick={this.handleItemClick}
            stat={formatFacetStat(this.getStat(item))}
            tooltip={this.props.getFacetItemText(item)}
            value={item}
          />
        ))}
      </FacetItemsList>
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
        minLength={2}
        onChange={this.search}
        placeholder={this.props.searchPlaceholder}
        value={this.state.query}
      />
    );
  }

  renderSearchResults() {
    const { searching, searchResults, searchPaging } = this.state;

    if (!searching && (!searchResults || !searchResults.length)) {
      return <div className="note spacer-bottom">{translate('no_results')}</div>;
    }

    if (!searchResults || !searchPaging) {
      // initial search
      return null;
    }

    return (
      <>
        <FacetItemsList>
          {searchResults.map(result => this.renderSearchResult(result))}
        </FacetItemsList>
        <ListFooter
          count={searchResults.length}
          loadMore={this.searchMore}
          ready={!searching}
          total={searchPaging.total}
        />
      </>
    );
  }

  renderSearchResult(result: S) {
    const key = this.props.getSearchResultKey(result);
    const active = this.props.values.includes(key);
    const stat = this.getStat(key);
    return (
      <FacetItem
        active={active}
        disabled={!active && stat === 0}
        key={key}
        loading={this.props.loading}
        name={this.props.renderSearchResult(result, this.state.query)}
        onClick={this.handleItemClick}
        stat={stat && formatFacetStat(stat)}
        tooltip={this.props.getSearchResultText(result)}
        value={key}
      />
    );
  }

  render() {
    const { stats = {} } = this.props;
    const values = this.props.values.map(item => this.props.getFacetItemText(item));
    return (
      <FacetBox property={this.props.property}>
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
            {this.state.query && this.state.searchResults !== undefined
              ? this.renderSearchResults()
              : this.renderList()}
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
