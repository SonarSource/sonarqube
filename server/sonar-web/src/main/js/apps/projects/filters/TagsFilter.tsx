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
import { size } from 'lodash';
import * as React from 'react';
import { RawQuery } from '~sonar-aligned/types/router';
import { searchProjectTags } from '../../../api/components';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';
import { Dict } from '../../../types/types';
import { ListStyleFacet } from '../../issues/sidebar/ListStyleFacet';
import { Facet } from '../types';

interface Props {
  facet?: Facet;
  loadSearchResultCount: (property: string, values: string[]) => Promise<Dict<number>>;
  onQueryChange: (change: RawQuery) => void;
  query: Dict<any>;
  value?: string[];
}

interface State {
  isLoading: boolean;
}

const LIST_SIZE = 10;
const SEARCH_SIZE = 100;

export default class TagsFacet extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      isLoading: false,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleSearch = (search = ''): Promise<{ maxResults: boolean; results: string[] }> => {
    this.setState({ isLoading: true });

    return searchProjectTags({
      q: search,
      ps: size(this.props.facet ?? {}) + LIST_SIZE,
    }).then((result) => {
      if (this.mounted) {
        this.setState({ isLoading: false });
      }
      return { maxResults: result.tags.length === SEARCH_SIZE, results: result.tags };
    });
  };

  handleChange = (newValue: Dict<string[]>) => {
    const { tags } = newValue;
    this.props.onQueryChange({ tags: tags.join(',') });
  };

  loadSearchResultCount = (tags: string[]) => {
    return this.props.loadSearchResultCount('tags', tags);
  };

  render() {
    const { facet, query, value = [] } = this.props;
    const { isLoading } = this.state;

    return (
      <ListStyleFacet<string>
        facetHeader={translate('projects.facets.tags')}
        fetching={isLoading}
        loadSearchResultCount={this.loadSearchResultCount}
        onChange={this.handleChange}
        onSearch={this.handleSearch}
        query={query}
        open
        property="tags"
        renderSearchResult={highlightTerm}
        searchPlaceholder={translate('search.search_for_tags')}
        showStatBar
        stats={facet}
        values={value}
      />
    );
  }
}
