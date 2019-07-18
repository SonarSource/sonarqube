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
import { difference, sortBy } from 'lodash';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getLanguageByKey } from '../../../store/languages';
import { Facet } from '../types';
import Filter from './Filter';
import FilterHeader from './FilterHeader';
import SearchableFilterFooter from './SearchableFilterFooter';
import SearchableFilterOption from './SearchableFilterOption';

interface Props {
  facet?: Facet;
  languages: T.Languages;
  maxFacetValue?: number;
  onQueryChange: (change: T.RawQuery) => void;
  organization?: { key: string };
  property?: string;
  query: T.Dict<any>;
  value?: string[];
}

export default class LanguagesFilter extends React.Component<Props> {
  getSearchOptions = () => {
    const { facet, languages } = this.props;
    let languageKeys = Object.keys(languages);
    if (facet) {
      languageKeys = difference(languageKeys, Object.keys(facet));
    }
    return languageKeys.map(key => ({ label: languages[key].name, value: key }));
  };

  getSortedOptions = (facet: Facet = {}) =>
    sortBy(Object.keys(facet), [(option: string) => -facet[option], (option: string) => option]);

  getFacetValueForOption = (facet: Facet = {}, option: string) => facet[option];

  renderOption = (option: string) => (
    <SearchableFilterOption
      option={getLanguageByKey(this.props.languages, option)}
      optionKey={option}
    />
  );

  render() {
    const { property = 'languages' } = this.props;

    return (
      <Filter
        facet={this.props.facet}
        footer={
          <SearchableFilterFooter
            onQueryChange={this.props.onQueryChange}
            options={this.getSearchOptions()}
            organization={this.props.organization}
            property={property}
            query={this.props.query}
          />
        }
        getFacetValueForOption={this.getFacetValueForOption}
        header={<FilterHeader name={translate('projects.facets.languages')} />}
        maxFacetValue={this.props.maxFacetValue}
        onQueryChange={this.props.onQueryChange}
        options={this.getSortedOptions(this.props.facet)}
        organization={this.props.organization}
        property={property}
        query={this.props.query}
        renderOption={this.renderOption}
        value={this.props.value}
      />
    );
  }
}
