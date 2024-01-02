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
import { debounce, difference, size, sortBy } from 'lodash';
import * as React from 'react';
import { searchProjectTags } from '../../../api/components';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Dict, RawQuery } from '../../../types/types';
import { Facet } from '../types';
import Filter from './Filter';
import FilterHeader from './FilterHeader';
import SearchableFilterFooter from './SearchableFilterFooter';
import SearchableFilterOption from './SearchableFilterOption';

interface Props {
  facet?: Facet;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  property?: string;
  query: Dict<any>;
  value?: string[];
}

interface State {
  isLoading: boolean;
  search: string;
  tags: string[];
}

const LIST_SIZE = 10;

function renderAccessibleLabel(option: string) {
  return translateWithParameters(
    'projects.facets.label_text_x',
    translate('projects.facets.tags'),
    option
  );
}

export default class TagsFilter extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      isLoading: false,
      search: '',
      tags: [],
    };
    this.handleSearch = debounce(this.handleSearch, 250);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getSearchOptions = () => {
    let tagsCopy = [...this.state.tags];
    if (this.props.facet) {
      tagsCopy = difference(tagsCopy, Object.keys(this.props.facet));
    }
    return tagsCopy.slice(0, LIST_SIZE).map((tag) => ({ label: tag, value: tag }));
  };

  handleSearch = (search?: string) => {
    if (search !== this.state.search) {
      search = search || '';
      this.setState({ search, isLoading: true });
      searchProjectTags({
        q: search,
        ps: size(this.props.facet || {}) + LIST_SIZE,
      }).then(
        (result) => {
          if (this.mounted) {
            this.setState({ isLoading: false, tags: result.tags });
          }
        },
        () => {}
      );
    }
  };

  getSortedOptions = (facet: Facet = {}) =>
    sortBy(Object.keys(facet), [(option: string) => -facet[option], (option: string) => option]);

  renderOption = (option: string) => <SearchableFilterOption optionKey={option} />;

  render() {
    const { property = 'tags' } = this.props;

    return (
      <Filter
        facet={this.props.facet}
        footer={
          <SearchableFilterFooter
            isLoading={this.state.isLoading}
            onInputChange={this.handleSearch}
            onOpen={this.handleSearch}
            onQueryChange={this.props.onQueryChange}
            options={this.getSearchOptions()}
            property={property}
            query={this.props.query}
          />
        }
        header={<FilterHeader name={translate('projects.facets.tags')} />}
        maxFacetValue={this.props.maxFacetValue}
        onQueryChange={this.props.onQueryChange}
        options={this.getSortedOptions(this.props.facet)}
        property={property}
        renderAccessibleLabel={renderAccessibleLabel}
        renderOption={this.renderOption}
        value={this.props.value}
      />
    );
  }
}
