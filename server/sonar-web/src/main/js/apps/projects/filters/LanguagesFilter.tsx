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

import { uniqBy } from 'lodash';
import * as React from 'react';
import { RawQuery } from '~sonar-aligned/types/router';
import withLanguagesContext from '../../../app/components/languages/withLanguagesContext';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';
import { Language, Languages } from '../../../types/languages';
import { Dict } from '../../../types/types';
import { ListStyleFacet } from '../../issues/sidebar/ListStyleFacet';
import { Facet } from '../types';

interface Props {
  facet?: Facet;
  languages: Languages;
  loadSearchResultCount: (property: string, values: string[]) => Promise<Dict<number>>;
  onQueryChange: (change: RawQuery) => void;
  query: Dict<any>;
  value?: string[];
}

export function LanguagesFilter(props: Props) {
  const { facet, languages, loadSearchResultCount, query, onQueryChange, value = [] } = props;

  const searchOptions = React.useMemo(() => {
    // add any language that presents in the facet, but might not be installed
    // for such language we don't know their display name, so let's just use their key
    // and make sure we reference each language only once
    return uniqBy(
      [...Object.values(languages), ...Object.keys(facet ?? {}).map((key) => ({ key, name: key }))],
      (language) => language.key,
    );
  }, [languages, facet]);

  const handleChange = React.useCallback(
    (newValue: Dict<string[]>) => {
      const { languages } = newValue;
      onQueryChange({ languages: languages.join(',') });
    },
    [onQueryChange],
  );

  const handleSearch = React.useCallback(
    (query: string) => {
      const results = searchOptions.filter((lang) =>
        lang.name.toLowerCase().includes(query.toLowerCase()),
      );

      const paging = { pageIndex: 1, pageSize: results.length, total: results.length };

      return Promise.resolve({
        paging,
        results,
      });
    },
    [searchOptions],
  );

  const handleSearchResultCount = React.useCallback(
    (languages: Language[]) => {
      return loadSearchResultCount(
        'languages',
        languages.map((l) => l.key),
      );
    },
    [loadSearchResultCount],
  );

  const renderSearchResults = React.useCallback(
    (lang: Language, term: string) => highlightTerm(lang.name, term),
    [],
  );

  const renderLanguageName = React.useCallback(
    (key: string) => {
      if (key === '<null>') {
        return translate('unknown');
      }

      return languages[key]?.name || key;
    },
    [languages],
  );

  return (
    <ListStyleFacet<Language>
      facetHeader={translate('projects.facets.languages')}
      fetching={false}
      getFacetItemText={renderLanguageName}
      getSearchResultKey={(language) => language.key}
      getSearchResultText={(language) => language.name}
      loadSearchResultCount={handleSearchResultCount}
      minSearchLength={1}
      onChange={handleChange}
      onSearch={handleSearch}
      query={query}
      open
      property="languages"
      renderFacetItem={renderLanguageName}
      renderSearchResult={renderSearchResults}
      searchPlaceholder={translate('search.search_for_languages')}
      showStatBar
      stats={facet}
      values={value}
    />
  );
}

export default withLanguagesContext(LanguagesFilter);
