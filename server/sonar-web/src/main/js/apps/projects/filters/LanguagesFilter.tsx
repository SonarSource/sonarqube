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
import { difference, sortBy } from 'lodash';
import Filter from './Filter';
import FilterHeader from './FilterHeader';
import SearchableFilterFooter from './SearchableFilterFooter';
import SearchableFilterOption from './SearchableFilterOption';
import { getLanguageByKey, Languages } from '../../../store/languages/reducer';
import { translate } from '../../../helpers/l10n';
import { Facet } from '../types';
import { RawQuery } from '../../../helpers/query';

interface Props {
  facet?: Facet;
  languages: Languages;
  maxFacetValue?: number;
  onQueryChange: (change: RawQuery) => void;
  organization?: { key: string };
  property?: string;
  query: { [x: string]: any };
  value?: Array<string>;
}

const LIST_SIZE = 10;

export default class LanguagesFilter extends React.Component<Props> {
  getSearchOptions = () => {
    let languageKeys = Object.keys(this.props.languages);
    if (this.props.facet) {
      languageKeys = difference(languageKeys, Object.keys(this.props.facet));
    }
    return languageKeys
      .slice(0, LIST_SIZE)
      .map(key => ({ label: this.props.languages[key].name, value: key }));
  };

  getSortedOptions = (facet: Facet = {}) =>
    sortBy(Object.keys(facet), [(option: string) => -facet[option], (option: string) => option]);

  getFacetValueForOption = (facet: Facet = {}, option: string) => facet[option];

  renderOption = (option: string) => (
    <SearchableFilterOption
      optionKey={option}
      option={getLanguageByKey(this.props.languages, option)}
    />
  );

  render() {
    const { property = 'languages' } = this.props;

    return (
      <Filter
        onQueryChange={this.props.onQueryChange}
        property={property}
        options={this.getSortedOptions(this.props.facet)}
        query={this.props.query}
        renderOption={this.renderOption}
        value={this.props.value}
        facet={this.props.facet}
        maxFacetValue={this.props.maxFacetValue}
        organization={this.props.organization}
        getFacetValueForOption={this.getFacetValueForOption}
        header={<FilterHeader name={translate('projects.facets.languages')} />}
        footer={
          <SearchableFilterFooter
            onQueryChange={this.props.onQueryChange}
            organization={this.props.organization}
            options={this.getSearchOptions()}
            property={property}
            query={this.props.query}
          />
        }
      />
    );
  }
}
