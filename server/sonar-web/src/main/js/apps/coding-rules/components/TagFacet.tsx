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
import { uniq } from 'lodash';
import * as React from 'react';
import TagsIcon from 'sonar-ui-common/components/icons/TagsIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { highlightTerm } from 'sonar-ui-common/helpers/search';
import { getRuleTags } from '../../../api/rules';
import { colors } from '../../../app/theme';
import ListStyleFacet from '../../../components/facet/ListStyleFacet';
import { BasicProps } from './Facet';

interface Props extends BasicProps {
  organization: string | undefined;
}

export default class TagFacet extends React.PureComponent<Props> {
  handleSearch = (query: string) => {
    return getRuleTags({ organization: this.props.organization, ps: 50, q: query }).then(tags => ({
      paging: { pageIndex: 1, pageSize: tags.length, total: tags.length },
      results: tags
    }));
  };

  handleSelect = (option: { value: string }) => {
    this.props.onChange({ tags: uniq([...this.props.values, option.value]) });
  };

  getTagName = (tag: string) => {
    return tag;
  };

  renderTag = (tag: string) => (
    <>
      <TagsIcon className="little-spacer-right" fill={colors.gray60} />
      {tag}
    </>
  );

  renderSearchResult = (tag: string, term: string) => (
    <>
      <TagsIcon className="little-spacer-right" fill={colors.gray60} />
      {highlightTerm(tag, term)}
    </>
  );

  render() {
    return (
      <ListStyleFacet<string>
        facetHeader={translate('coding_rules.facet.tags')}
        fetching={false}
        getFacetItemText={this.getTagName}
        getSearchResultKey={tag => tag}
        getSearchResultText={tag => tag}
        onChange={this.props.onChange}
        onSearch={this.handleSearch}
        onToggle={this.props.onToggle}
        open={this.props.open}
        property="tags"
        renderFacetItem={this.renderTag}
        renderSearchResult={this.renderSearchResult}
        searchPlaceholder={translate('search.search_for_tags')}
        stats={this.props.stats}
        values={this.props.values}
      />
    );
  }
}
