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
import { difference, sortBy } from 'lodash';
import Filter from './Filter';
import FilterHeader from './FilterHeader';
import SearchableFilterFooter from './SearchableFilterFooter';
import SearchableFilterOption from './SearchableFilterOption';
import { getLanguageByKey } from '../../../store/languages/reducer';

type Props = {
  query: {},
  languages: {},
  router: { push: ({ pathname: string, query?: {} }) => void },
  value?: Array<string>,
  facet?: {},
  isFavorite?: boolean,
  organization?: {},
  maxFacetValue?: number
};

const LIST_SIZE = 10;

export default class LanguagesFilter extends React.PureComponent {
  getSearchOptions: () => [{ label: string, value: string }];
  props: Props;
  property = 'languages';

  renderOption = (option: string) => (
    <SearchableFilterOption
      optionKey={option}
      option={getLanguageByKey(this.props.languages, option)}
    />
  );

  getSearchOptions(facet: {}, languages: {}) {
    let languageKeys = Object.keys(languages);
    if (facet) {
      languageKeys = difference(languageKeys, Object.keys(facet));
    }
    return languageKeys
      .slice(0, LIST_SIZE)
      .map(key => ({ label: languages[key].name, value: key }));
  }

  getSortedOptions(facet: {} = {}) {
    return sortBy(Object.keys(facet), [option => -facet[option], option => option]);
  }

  getFacetValueForOption = (facet: {} = {}, option: string) => facet[option];

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
        highlightUnder={1}
        header={<FilterHeader name="Languages" />}
        footer={
          <SearchableFilterFooter
            property={this.property}
            query={this.props.query}
            options={this.getSearchOptions(this.props.facet, this.props.languages)}
            isFavorite={this.props.isFavorite}
            organization={this.props.organization}
            router={this.props.router}
          />
        }
      />
    );
  }
}
