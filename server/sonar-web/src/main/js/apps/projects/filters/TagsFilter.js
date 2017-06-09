/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import { debounce, difference, sortBy, size } from 'lodash';
import Filter from './Filter';
import FilterHeader from './FilterHeader';
import SearchableFilterFooter from './SearchableFilterFooter';
import SearchableFilterOption from './SearchableFilterOption';
import { searchProjectTags } from '../../../api/components';
import { translate } from '../../../helpers/l10n';

type Props = {
  query: {},
  router: { push: ({ pathname: string, query?: {} }) => void },
  value?: Array<string>,
  facet?: {},
  isFavorite?: boolean,
  organization?: {},
  maxFacetValue?: number
};

type State = {
  isLoading: boolean,
  search: string,
  tags: Array<string>
};

const LIST_SIZE = 10;

export default class TagsFilter extends React.PureComponent {
  props: Props;
  state: State;
  property: string;

  constructor(props: Props) {
    super(props);
    this.state = {
      isLoading: false,
      search: '',
      tags: []
    };
    this.property = 'tags';
    this.handleSearch = debounce(this.handleSearch.bind(this), 250);
  }

  getSearchOptions(facet?: {}, tags: Array<string>): Array<{ label: string, value: string }> {
    let tagsCopy = [...tags];
    if (facet) {
      tagsCopy = difference(tagsCopy, Object.keys(facet));
    }
    return tagsCopy.slice(0, LIST_SIZE).map(tag => ({ label: tag, value: tag }));
  }

  handleSearch = (search?: string) => {
    if (search !== this.state.search) {
      search = search || '';
      this.setState({ search, isLoading: true });
      searchProjectTags({
        q: search,
        ps: size(this.props.facet || {}) + LIST_SIZE
      }).then(result => {
        this.setState({ isLoading: false, tags: result.tags });
      });
    }
  };

  getSortedOptions(facet: {} = {}) {
    return sortBy(Object.keys(facet), [option => -facet[option], option => option]);
  }

  getFacetValueForOption = (facet: {}, option: string) => facet[option];

  renderOption = (option: string) => <SearchableFilterOption optionKey={option} />;

  render() {
    return (
      <Filter
        property={this.property}
        options={this.getSortedOptions(this.props.facet)}
        query={this.props.query}
        renderOption={this.renderOption}
        value={this.props.value}
        facet={this.props.facet}
        maxFacetValue={this.props.maxFacetValue}
        isFavorite={this.props.isFavorite}
        organization={this.props.organization}
        getFacetValueForOption={this.getFacetValueForOption}
        header={<FilterHeader name={translate('projects.facets.tags')} />}
        footer={
          <SearchableFilterFooter
            property={this.property}
            query={this.props.query}
            options={this.getSearchOptions(this.props.facet, this.state.tags)}
            isLoading={this.state.isLoading}
            onOpen={this.handleSearch}
            onInputChange={this.handleSearch}
            isFavorite={this.props.isFavorite}
            organization={this.props.organization}
            router={this.props.router}
          />
        }
      />
    );
  }
}
