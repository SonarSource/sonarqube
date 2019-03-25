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
import { omit, sortBy, without } from 'lodash';
import { searchAssignees, Query, ReferencedUser, SearchedAssignee, Facet } from '../utils';
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';

export interface Props {
  assigned: boolean;
  assignees: string[];
  fetching: boolean;
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  organization: string | undefined;
  query: Query;
  stats: T.Dict<number> | undefined;
  referencedUsers: T.Dict<ReferencedUser>;
}

export default class AssigneeFacet extends React.PureComponent<Props> {
  handleSearch = (query: string, page?: number) => {
    return searchAssignees(query, this.props.organization, page);
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
      this.props.onChange({ assigned: true, assignees: newValue });
    } else {
      this.props.onChange({
        assigned: true,
        assignees: assignees.includes(itemValue) && assignees.length < 2 ? [] : [itemValue]
      });
    }
  };

  handleClear = () => {
    this.props.onChange({ assigned: true, assignees: [] });
  };

  getAssigneeName = (assignee: string) => {
    if (assignee === '') {
      return translate('unassigned');
    } else {
      const user = this.props.referencedUsers[assignee];
      return user ? user.name : assignee;
    }
  };

  loadSearchResultCount = (assignees: SearchedAssignee[]) => {
    return this.props.loadSearchResultCount('assignees', {
      assigned: undefined,
      assignees: assignees.map(assignee => assignee.login)
    });
  };

  getSortedItems = () => {
    const { stats = {} } = this.props;
    return sortBy(
      Object.keys(stats),
      // put "not assigned" first
      key => (key === '' ? 0 : 1),
      // the sort by number
      key => -stats[key]
    );
  };

  renderFacetItem = (assignee: string) => {
    if (assignee === '') {
      return translate('unassigned');
    }

    const user = this.props.referencedUsers[assignee];
    return user ? (
      <>
        <Avatar className="little-spacer-right" hash={user.avatar} name={user.name} size={16} />
        {user.name}
      </>
    ) : (
      assignee
    );
  };

  renderSearchResult = (result: SearchedAssignee, query: string) => {
    return (
      <>
        {result.avatar !== undefined && (
          <Avatar
            className="little-spacer-right"
            hash={result.avatar}
            name={result.name}
            size={16}
          />
        )}
        {highlightTerm(result.name, query)}
      </>
    );
  };

  render() {
    const values = [...this.props.assignees];
    if (!this.props.assigned) {
      values.push('');
    }

    return (
      <ListStyleFacet<SearchedAssignee>
        facetHeader={translate('issues.facet.assignees')}
        fetching={this.props.fetching}
        getFacetItemText={this.getAssigneeName}
        getSearchResultKey={user => user.login}
        getSearchResultText={user => user.name}
        // put "not assigned" item first
        getSortedItems={this.getSortedItems}
        loadSearchResultCount={this.loadSearchResultCount}
        onChange={this.props.onChange}
        onClear={this.handleClear}
        onItemClick={this.handleItemClick}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="assignees"
        query={omit(this.props.query, 'assigned', 'assignees')}
        renderFacetItem={this.renderFacetItem}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_users')}
        stats={this.props.stats}
        values={values}
      />
    );
  }
}
