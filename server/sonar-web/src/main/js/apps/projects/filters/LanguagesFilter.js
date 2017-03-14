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
import difference from 'lodash/difference';
import sortBy from 'lodash/sortBy';
import Filter from './Filter';
import SearchableFilterFooter from './SearchableFilterFooter';
import SearchableFilterOption from './SearchableFilterOption';
import { getLanguageByKey } from '../../../store/languages/reducer';

type Props = {
  query: {},
  isFavorite?: boolean,
  organization?: {},
  languages: {},
  value?: Array<string>,
  facet?: {},
  maxFacetValue?: number,
  router: { push: (path: string, query?: {}) => void }
};

export default class LanguagesFilter extends React.PureComponent {
  props: Props;
  property = 'languages';

  renderOption = option => (
    <SearchableFilterOption
      optionKey={option}
      option={getLanguageByKey(this.props.languages, option)}/>
  );

  renderFooter = () => (
    <SearchableFilterFooter
      property={this.property}
      query={this.props.query}
      value={this.props.value}
      facet={this.props.facet}
      options={this.props.languages}
      getOptions={this.getSearchOptions}
      isFavorite={this.props.isFavorite}
      organization={this.props.organization}
      router={this.props.router}/>
  );

  getSearchOptions = () => {
    const { facet, languages } = this.props;
    let languageKeys = Object.keys(languages);
    if (facet) {
      languageKeys = difference(languageKeys, Object.keys(facet));
    }
    return languageKeys.map(key => ({ label: languages[key].name, value: key }));
  };

  getSortedOptions (facet) {
    if (!facet) {
      return [];
    }
    return sortBy(Object.keys(facet), [option => -facet[option]]);
  }

  getFacetValueForOption = (facet, option) => facet[option];

  renderName = () => 'Languages';

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
        // we need to pass the languages so the footer is correctly updated if it changes
        languages={this.props.languages}/>
    );
  }
}
