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
import {
  Dropdown,
  InputMultiSelect,
  MultiSelector,
  PopupPlacement,
  PopupZLevel,
} from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  allowCreation: boolean;
  inputId?: string;
  onChange: (selected: string[]) => void;
  onSearch: (query: string) => Promise<string[]>;
  selectedTags: string[];
}

export default function TagsSelect(props: Props) {
  const { allowCreation, inputId, onSearch, onChange, selectedTags } = props;
  const [searchResults, setSearchResults] = React.useState<string[]>([]);

  const doSearch = React.useCallback(
    async (query: string) => {
      const results = await onSearch(query);
      setSearchResults(results);
    },
    [onSearch, setSearchResults],
  );

  const onSelect = React.useCallback(
    (newTag: string) => {
      onChange([...selectedTags, newTag]);
    },
    [onChange, selectedTags],
  );

  const onUnselect = React.useCallback(
    (toRemove: string) => {
      onChange(selectedTags.filter((tag) => tag !== toRemove));
    },
    [onChange, selectedTags],
  );

  return (
    <Dropdown
      allowResizing
      closeOnClick={false}
      id="tag-selector"
      overlay={
        // eslint-disable-next-line jsx-a11y/no-static-element-interactions
        <div onMouseDown={handleMousedown}>
          <MultiSelector
            allowNewElements={allowCreation}
            createElementLabel={translateWithParameters('issue.create_tag')}
            headerLabel={translate('issue_bulk_change.select_tags')}
            noResultsLabel={translate('no_results')}
            onSelect={onSelect}
            onUnselect={onUnselect}
            searchInputAriaLabel={translate('search.search_for_tags')}
            selectedElements={selectedTags}
            onSearch={doSearch}
            elements={searchResults}
          />
        </div>
      }
      placement={PopupPlacement.BottomLeft}
      zLevel={PopupZLevel.Global}
    >
      {({ onToggleClick }) => (
        <InputMultiSelect
          className="sw-w-abs-300"
          id={inputId}
          onClick={onToggleClick}
          placeholder={translate('select_verb')}
          selectedLabel={translate('issue_bulk_change.selected_tags')}
          count={selectedTags.length}
        />
      )}
    </Dropdown>
  );
}

/*
 * Prevent click from triggering a change of focus that would close the dropdown
 */
function handleMousedown(e: React.MouseEvent) {
  if ((e.target as HTMLElement).tagName !== 'INPUT') {
    e.preventDefault();
    e.stopPropagation();
  }
}
