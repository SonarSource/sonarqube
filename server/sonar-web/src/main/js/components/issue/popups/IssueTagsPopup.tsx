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
import { MultiSelector } from 'design-system';
import { difference, noop, without } from 'lodash';
import * as React from 'react';
import { searchIssueTags } from '../../../api/issues';
import { translate } from '../../../helpers/l10n';

interface IssueTagsPopupProps {
  selectedTags: string[];
  setTags: (tags: string[]) => void;
}

function IssueTagsPopup({ selectedTags, setTags }: IssueTagsPopupProps) {
  const [searchResult, setSearchResult] = React.useState<string[]>([]);
  const LIST_SIZE = 10;

  function onSearch(query: string) {
    return searchIssueTags({
      q: query,
      ps: Math.min(selectedTags.length - 1 + LIST_SIZE, 100),
    }).then((tags: string[]) => {
      setSearchResult(tags);
    }, noop);
  }

  function onSelect(tag: string) {
    setTags([...selectedTags, tag]);
  }

  function onUnselect(tag: string) {
    setTags(without(selectedTags, tag));
  }

  const availableTags = difference(searchResult, selectedTags);

  return (
    <MultiSelector
      headerLabel={translate('issue.tags')}
      searchInputAriaLabel={translate('search.search_for_tags')}
      createElementLabel={translate('issue.create_tag')}
      noResultsLabel={translate('no_results')}
      onSearch={onSearch}
      onSelect={onSelect}
      onUnselect={onUnselect}
      selectedElements={selectedTags}
      elements={availableTags}
    />
  );
}

export default IssueTagsPopup;
