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
import { uniqBy, omit } from 'lodash';
import { connect } from 'react-redux';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { Query, ReferencedLanguage, Facet } from '../utils';
import { getLanguages, Store } from '../../../store/rootReducer';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';

interface InstalledLanguage {
  key: string;
  name: string;
}

interface Props {
  fetching: boolean;
  installedLanguages: InstalledLanguage[];
  languages: string[];
  loadSearchResultCount: (property: string, changes: Partial<Query>) => Promise<Facet>;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  query: Query;
  referencedLanguages: T.Dict<ReferencedLanguage>;
  stats: T.Dict<number> | undefined;
}

class LanguageFacet extends React.PureComponent<Props> {
  getLanguageName = (language: string) => {
    const { referencedLanguages } = this.props;
    return referencedLanguages[language] ? referencedLanguages[language].name : language;
  };

  handleSearch = (query: string) => {
    const options = this.getAllPossibleOptions();
    const results = options.filter(language =>
      language.name.toLowerCase().includes(query.toLowerCase())
    );
    const paging = { pageIndex: 1, pageSize: results.length, total: results.length };
    return Promise.resolve({ paging, results });
  };

  getAllPossibleOptions = () => {
    const { installedLanguages, stats = {} } = this.props;

    // add any language that presents in the facet, but might not be installed
    // for such language we don't know their display name, so let's just use their key
    // and make sure we reference each language only once
    return uniqBy(
      [...installedLanguages, ...Object.keys(stats).map(key => ({ key, name: key }))],
      language => language.key
    );
  };

  loadSearchResultCount = (languages: InstalledLanguage[]) => {
    return this.props.loadSearchResultCount('languages', {
      languages: languages.map(language => language.key)
    });
  };

  renderSearchResult = ({ name }: InstalledLanguage, term: string) => {
    return highlightTerm(name, term);
  };

  render() {
    return (
      <ListStyleFacet<InstalledLanguage>
        facetHeader={translate('issues.facet.languages')}
        fetching={this.props.fetching}
        getFacetItemText={this.getLanguageName}
        getSearchResultKey={language => language.key}
        getSearchResultText={language => language.name}
        loadSearchResultCount={this.loadSearchResultCount}
        minSearchLength={1}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="languages"
        query={omit(this.props.query, 'languages')}
        renderFacetItem={this.getLanguageName}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_languages')}
        stats={this.props.stats}
        values={this.props.languages}
      />
    );
  }
}

const mapStateToProps = (state: Store) => ({
  installedLanguages: Object.values(getLanguages(state))
});

export default connect(mapStateToProps)(LanguageFacet);
