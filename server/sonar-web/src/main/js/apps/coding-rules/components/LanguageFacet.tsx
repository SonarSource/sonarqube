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
import { uniqBy } from 'lodash';
import * as React from 'react';
import { connect } from 'react-redux';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { highlightTerm } from 'sonar-ui-common/helpers/search';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { getLanguages, Store } from '../../../store/rootReducer';
import { BasicProps } from './Facet';

interface InstalledLanguage {
  key: string;
  name: string;
}

interface Props extends BasicProps {
  disabled?: boolean;
  installedLanguages: InstalledLanguage[];
}

class LanguageFacet extends React.PureComponent<Props> {
  getLanguageName = (languageKey: string) => {
    const language = this.props.installedLanguages.find(l => l.key === languageKey);
    return language ? language.name : languageKey;
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

  renderSearchResult = ({ name }: InstalledLanguage, term: string) => {
    return highlightTerm(name, term);
  };

  render() {
    return (
      <ListStyleFacet<InstalledLanguage>
        disabled={this.props.disabled}
        disabledHelper={translate('coding_rules.filters.language.inactive')}
        facetHeader={translate('coding_rules.facet.languages')}
        fetching={false}
        getFacetItemText={this.getLanguageName}
        getSearchResultKey={language => language.key}
        getSearchResultText={language => language.name}
        minSearchLength={1}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="languages"
        renderFacetItem={this.getLanguageName}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_languages')}
        stats={this.props.stats}
        values={this.props.values}
      />
    );
  }
}

const mapStateToProps = (state: Store) => ({
  installedLanguages: Object.values(getLanguages(state))
});

export default connect(mapStateToProps)(LanguageFacet);
