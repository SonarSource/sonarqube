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
import { sortBy, uniq, without } from 'lodash';
import {
  searchAssignees,
  formatFacetStat,
  Query,
  ReferencedUser,
  SearchedAssignee
} from '../utils';
import { Component, Paging } from '../../../app/types';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import MultipleSelectionHint from '../../../components/facet/MultipleSelectionHint';
import SearchBox from '../../../components/controls/SearchBox';
import ListFooter from '../../../components/controls/ListFooter';
import { highlightTerm } from '../../../helpers/search';

export interface Props {
  assigned: boolean;
  assignees: string[];
  component: Component | undefined;
  fetching: boolean;
  loading?: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  organization: string | undefined;
  stats: { [x: string]: number } | undefined;
  referencedUsers: { [login: string]: ReferencedUser };
}

interface State {
  query: string;
  searching: boolean;
  searchResults?: SearchedAssignee[];
  searchPaging?: Paging;
}

export default class AssigneeFacet extends React.PureComponent<Props, State> {
  mounted = false;
  property = 'assignees';

  state: State = {
    query: '',
    searching: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopSearching = () => {
    if (this.mounted) {
      this.setState({ searching: false });
    }
  };

  search = (query: string) => {
    if (query.length >= 2) {
      this.setState({ query, searching: true });
      searchAssignees(query, this.props.organization).then(({ paging, results }) => {
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
      searchAssignees(query, this.props.organization, searchPaging.pageIndex + 1).then(
        ({ paging, results }) => {
          if (this.mounted) {
            this.setState({
              searching: false,
              searchResults: [...searchResults, ...results],
              searchPaging: paging
            });
          }
        },
        this.stopSearching
      );
    }
  };

  handleItemClick = (itemValue: string, multiple: boolean) => {
    const { assignees } = this.props;
    if (itemValue === '') {
      // unassigned
      this.props.onChange({ assigned: !this.props.assigned, assignees: [] });
    } else if (multiple) {
      const newValue = sortBy(
        assignees.includes(itemValue) ? without(assignees, itemValue) : [...assignees, itemValue]
      );
      this.props.onChange({ assigned: true, [this.property]: newValue });
    } else {
      this.props.onChange({
        assigned: true,
        [this.property]: assignees.includes(itemValue) && assignees.length < 2 ? [] : [itemValue]
      });
    }
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ assigned: true, assignees: [] });
  };

  handleSelect = (option: { value: string }) => {
    const { assignees } = this.props;
    this.props.onChange({ assigned: true, [this.property]: uniq([...assignees, option.value]) });
  };

  isAssigneeActive(assignee: string) {
    return assignee === '' ? !this.props.assigned : this.props.assignees.includes(assignee);
  }

  getAssigneeNameAndTooltip(assignee: string) {
    if (assignee === '') {
      return { name: translate('unassigned'), tooltip: translate('unassigned') };
    } else {
      const { referencedUsers } = this.props;
      if (referencedUsers[assignee]) {
        return {
          name: (
            <span>
              <Avatar
                className="little-spacer-right"
                hash={referencedUsers[assignee].avatar}
                name={referencedUsers[assignee].name}
                size={16}
              />
              {referencedUsers[assignee].name}
            </span>
          ),
          tooltip: referencedUsers[assignee].name
        };
      } else {
        return { name: assignee, tooltip: assignee };
      }
    }
  }

  getStat(assignee: string) {
    const { stats } = this.props;
    return stats ? stats[assignee] : undefined;
  }

  getValues() {
    const values = this.props.assignees.map(assignee => {
      const user = this.props.referencedUsers[assignee];
      return user ? user.name : assignee;
    });
    if (!this.props.assigned) {
      values.push(translate('unassigned'));
    }
    return values;
  }

  renderOption = (option: { avatar: string; label: string }) => {
    return this.renderAssignee(option.avatar, option.label);
  };

  renderAssignee = (avatar: string | undefined, name: string) => (
    <span>
      {avatar !== undefined && (
        <Avatar className="little-spacer-right" hash={avatar} name={name} size={16} />
      )}
      {name}
    </span>
  );

  renderListItem(assignee: string) {
    const { name, tooltip } = this.getAssigneeNameAndTooltip(assignee);
    return (
      <FacetItem
        active={this.isAssigneeActive(assignee)}
        key={assignee}
        loading={this.props.loading}
        name={name}
        onClick={this.handleItemClick}
        stat={formatFacetStat(this.getStat(assignee))}
        tooltip={tooltip}
        value={assignee}
      />
    );
  }

  renderList() {
    const { stats } = this.props;

    if (!stats) {
      return null;
    }

    const assignees = sortBy(
      Object.keys(stats),
      // put unassigned first
      key => (key === '' ? 0 : 1),
      // the sort by number
      key => -stats[key]
    );

    return (
      <FacetItemsList>{assignees.map(assignee => this.renderListItem(assignee))}</FacetItemsList>
    );
  }

  renderSearch() {
    if (!this.props.stats || !Object.keys(this.props.stats).length) {
      return null;
    }

    return (
      <SearchBox
        autoFocus={true}
        className="little-spacer-top spacer-bottom"
        loading={this.state.searching}
        minLength={2}
        onChange={this.search}
        placeholder={translate('search.search_for_users')}
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

  renderSearchResult(result: SearchedAssignee) {
    const active = this.props.assignees.includes(result.login);
    const stat = this.getStat(result.login);
    return (
      <FacetItem
        active={active}
        disabled={!active && stat === 0}
        key={result.login}
        loading={this.props.loading}
        name={
          <>
            {result.avatar !== undefined && (
              <Avatar
                className="little-spacer-right"
                hash={result.avatar}
                name={result.name}
                size={16}
              />
            )}
            {highlightTerm(result.name, this.state.query)}
          </>
        }
        onClick={this.handleItemClick}
        stat={stat && formatFacetStat(stat)}
        tooltip={result.name}
        value={result.login}
      />
    );
  }

  render() {
    const { assignees, stats = {} } = this.props;
    return (
      <FacetBox property={this.property}>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={this.getValues()}
        />

        <DeferredSpinner loading={this.props.fetching} />
        {this.props.open && (
          <>
            {this.renderSearch()}
            {this.state.query && this.state.searchResults !== undefined
              ? this.renderSearchResults()
              : this.renderList()}
            <MultipleSelectionHint options={Object.keys(stats).length} values={assignees.length} />
          </>
        )}
      </FacetBox>
    );
  }
}
