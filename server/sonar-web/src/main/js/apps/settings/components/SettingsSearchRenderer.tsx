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
import classNames from 'classnames';
import * as React from 'react';
import Link from '../../../components/common/Link';
import { DropdownOverlay } from '../../../components/controls/Dropdown';
import OutsideClickHandler from '../../../components/controls/OutsideClickHandler';
import SearchBox from '../../../components/controls/SearchBox';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { scrollToElement } from '../../../helpers/scrolling';
import { ExtendedSettingDefinition } from '../../../types/settings';
import { Component } from '../../../types/types';
import { buildSettingLink, isRealSettingKey } from '../utils';

export interface SettingsSearchRendererProps {
  className?: string;
  component?: Component;
  results?: ExtendedSettingDefinition[];
  searchQuery: string;
  selectedResult?: string;
  showResults: boolean;
  onClickOutside: () => void;
  onMouseOverResult: (key: string) => void;
  onSearchInputChange: (query: string) => void;
  onSearchInputFocus: () => void;
  onSearchInputKeyDown: (event: React.KeyboardEvent) => void;
}

export default function SettingsSearchRenderer(props: SettingsSearchRendererProps) {
  const { className, component, results, searchQuery, selectedResult, showResults } = props;

  const scrollableNodeRef = React.useRef(null);
  const selectedNodeRef = React.useRef<HTMLLIElement>(null);

  React.useEffect(() => {
    const parent = scrollableNodeRef.current;
    const selectedNode = selectedNodeRef.current;
    if (selectedNode && parent) {
      scrollToElement(selectedNode, { topOffset: 30, bottomOffset: 30, parent });
    }
  });

  return (
    <OutsideClickHandler onClickOutside={props.onClickOutside}>
      <div className={classNames('dropdown', className)}>
        <SearchBox
          onChange={props.onSearchInputChange}
          onFocus={props.onSearchInputFocus}
          onKeyDown={props.onSearchInputKeyDown}
          placeholder={translate('settings.search.placeholder')}
          value={searchQuery}
        />
        {showResults && (
          <DropdownOverlay noPadding={true}>
            <ul className="settings-search-results menu" ref={scrollableNodeRef}>
              {results && results.length > 0 ? (
                results.map((r) => (
                  <li
                    key={r.key}
                    className={classNames('spacer-bottom spacer-top', {
                      active: selectedResult === r.key,
                    })}
                    ref={selectedResult === r.key ? selectedNodeRef : undefined}
                  >
                    <Link
                      onClick={props.onClickOutside}
                      onMouseEnter={() => props.onMouseOverResult(r.key)}
                      to={buildSettingLink(r, component)}
                    >
                      <div className="settings-search-result-title display-flex-space-between">
                        <h3>{r.name || r.subCategory}</h3>
                      </div>
                      {isRealSettingKey(r.key) && (
                        <div className="note spacer-top">
                          {translateWithParameters('settings.key_x', r.key)}
                        </div>
                      )}
                    </Link>
                  </li>
                ))
              ) : (
                <div className="big-padded">{translate('no_results')}</div>
              )}
            </ul>
          </DropdownOverlay>
        )}
      </div>
    </OutsideClickHandler>
  );
}
