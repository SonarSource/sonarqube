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
import * as React from 'react';
import { getRuleRepositories } from '../../../api/rules';
import withLanguagesContext from '../../../app/components/languages/withLanguagesContext';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { translate } from '../../../helpers/l10n';
import { highlightTerm } from '../../../helpers/search';
import { Languages } from '../../../types/languages';
import { Dict } from '../../../types/types';
import { BasicProps } from './Facet';

interface StateProps {
  languages: Languages;
}

interface Props extends BasicProps, StateProps {
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
}

export class RepositoryFacet extends React.PureComponent<Props> {
  getLanguageName = (languageKey: string) => {
    const { languages } = this.props;
    const language = languages[languageKey];
    return (language && language.name) || languageKey;
  };

  handleSearch(query: string) {
    return getRuleRepositories({ q: query }).then((repos) => {
      return {
        paging: { pageIndex: 1, pageSize: repos.length, total: repos.length },
        results: repos.map((r) => r.key),
      };
    });
  }

  renderName = (repositoryKey: string) => {
    const { referencedRepositories } = this.props;
    const repository = referencedRepositories[repositoryKey];
    return repository ? (
      <>
        {repository.name}
        <span className="note little-spacer-left">{this.getLanguageName(repository.language)}</span>
      </>
    ) : (
      repositoryKey
    );
  };

  renderTextName = (repositoryKey: string) => {
    const { referencedRepositories } = this.props;
    const repository = referencedRepositories[repositoryKey];
    return (repository && repository.name) || repositoryKey;
  };

  renderSearchTextName = (repositoryKey: string, query: string) => {
    const { referencedRepositories } = this.props;
    const repository = referencedRepositories[repositoryKey];

    return repository ? (
      <>
        {highlightTerm(repository.name, query)}
        <span className="note little-spacer-left">{this.getLanguageName(repository.language)}</span>
      </>
    ) : (
      repositoryKey
    );
  };

  render() {
    return (
      <ListStyleFacet<string>
        facetHeader={translate('coding_rules.facet.repositories')}
        fetching={false}
        getFacetItemText={this.renderTextName}
        getSearchResultKey={(rep) => rep}
        getSearchResultText={this.renderTextName}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="repositories"
        renderFacetItem={this.renderName}
        renderSearchResult={this.renderSearchTextName}
        searchPlaceholder={translate('search.search_for_repositories')}
        stats={this.props.stats}
        values={this.props.values}
      />
    );
  }
}

export default withLanguagesContext(RepositoryFacet);
