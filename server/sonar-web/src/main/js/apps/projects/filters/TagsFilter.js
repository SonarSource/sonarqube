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
import debounce from 'lodash/debounce';
import difference from 'lodash/difference';
import sortBy from 'lodash/sortBy';
import Filter from './Filter';
import SearchableFilterFooter from './SearchableFilterFooter';
import SearchableFilterOption from './SearchableFilterOption';
import { searchProjectTags } from '../../../api/components';

type Props = {
  query: {},
  isFavorite?: boolean,
  organization?: {},
  value?: Array<string>,
  facet?: {},
  maxFacetValue?: number,
  router: { push: (path: string, query?: {}) => void }
};

type State = {
  isLoading: boolean,
  search: string,
  tags: Array<string>
};

const PAGE_SIZE = 20;

export default class TagsFilter extends React.PureComponent {
  getSearchOptions: () => [{ label: string, value: string }];
  props: Props;
  state: State = {
    isLoading: false,
    search: '',
    tags: []
  };
  property = 'tags';

  constructor (props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch.bind(this), 250);
  }

  renderOption = (option: string) => <SearchableFilterOption optionKey={option}/>;

  renderFooter = () => (
    <SearchableFilterFooter
      property={this.property}
      query={this.props.query}
      value={this.props.value}
      options={this.state.tags}
      isLoading={this.state.isLoading}
      getOptions={this.getSearchOptions}
      onOpen={this.handleSearch}
      onInputChange={this.handleSearch}
      isFavorite={this.props.isFavorite}
      organization={this.props.organization}
      router={this.props.router}/>
  );

  getSearchOptions = () => {
    const { facet } = this.props;
    const { tags } = this.state;
    let tagsCopy = [...tags];
    if (facet) {
      tagsCopy = difference(tagsCopy, Object.keys(facet));
    }
    return tagsCopy.map(tag => ({ label: tag, value: tag }));
  };

  getSortedOptions (facet: {}) {
    if (!facet) {
      return [];
    }
    return sortBy(Object.keys(facet), [option => -facet[option]]);
  }

  getFacetValueForOption = (facet: {}, option: string) => facet[option];

  handleSearch = (search?: string) => {
    if (search !== this.state.search) {
      search = search || '';
      this.setState({ search, isLoading: true });
      searchProjectTags({ q: search, ps: PAGE_SIZE }).then(result => {
        this.setState({ isLoading: false, tags: result.tags });
      });
    }
  };

  renderName = () => 'Tags';

  render () {
    return (
      <Filter
        property={this.property}
        getOptions={this.getSortedOptions}
        renderName={this.renderName}
        renderOption={this.renderOption}
        renderFooter={this.renderFooter}
        getFacetValueForOption={this.getFacetValueForOption}
        query={this.props.query}
        value={this.props.value}
        facet={this.props.facet}
        maxFacetValue={this.props.maxFacetValue}
        isFavorite={this.props.isFavorite}
        organization={this.props.organization}
        // we need to pass the tags and isLoading so the footer is correctly updated if it changes
        tags={this.state.tags}
        isLoading={this.state.isLoading}/>
    );
  }
}
