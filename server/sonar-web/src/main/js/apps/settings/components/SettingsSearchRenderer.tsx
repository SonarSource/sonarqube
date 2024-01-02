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
import styled from '@emotion/styled';
import classNames from 'classnames';
import {
  DropdownMenu,
  InputSearch,
  LinkBox,
  Note,
  OutsideClickHandler,
  Popup,
  PopupPlacement,
  themeColor,
} from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { ExtendedSettingDefinition } from '../../../types/settings';
import { Component } from '../../../types/types';
import { buildSettingLink, isRealSettingKey } from '../utils';

const SEARCH_INPUT_ID = 'settings-search-input';
export interface SettingsSearchRendererProps {
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

export default function SettingsSearchRenderer(props: Readonly<SettingsSearchRendererProps>) {
  const { component, results, searchQuery, selectedResult, showResults } = props;

  const selectedNodeRef = React.useRef<HTMLLIElement>(null);

  React.useEffect(() => {
    selectedNodeRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  });

  return (
    <OutsideClickHandler onClickOutside={props.onClickOutside}>
      <Popup
        allowResizing
        placement={PopupPlacement.BottomLeft}
        overlay={
          showResults && (
            <DropdownMenu
              className="sw-overflow-y-auto sw-overflow-x-hidden"
              maxHeight="50vh"
              size="auto"
              aria-owns={SEARCH_INPUT_ID}
            >
              {results && results.length > 0 ? (
                results.map((r) => (
                  <ResultItem
                    key={r.key}
                    active={selectedResult === r.key}
                    resultKey={r.key}
                    onMouseEnter={props.onMouseOverResult}
                    innerRef={selectedResult === r.key ? selectedNodeRef : undefined}
                  >
                    <LinkBox
                      className="sw-block sw-py-2 sw-px-4"
                      onClick={props.onClickOutside}
                      to={buildSettingLink(r, component)}
                    >
                      <h3 className="sw-body-sm-highlight">{r.name ?? r.subCategory}</h3>
                      {isRealSettingKey(r.key) && (
                        <StyledNote>{translateWithParameters('settings.key_x', r.key)}</StyledNote>
                      )}
                    </LinkBox>
                  </ResultItem>
                ))
              ) : (
                <div className="sw-p-4">{translate('no_results')}</div>
              )}
            </DropdownMenu>
          )
        }
      >
        <InputSearch
          id={SEARCH_INPUT_ID}
          onChange={props.onSearchInputChange}
          onFocus={props.onSearchInputFocus}
          onKeyDown={props.onSearchInputKeyDown}
          placeholder={translate('settings.search.placeholder')}
          value={searchQuery}
        />
      </Popup>
    </OutsideClickHandler>
  );
}

interface ResultItemProps {
  active: boolean;
  children: React.ReactNode;
  innerRef?: React.Ref<HTMLLIElement>;
  onMouseEnter: (resultKey: string) => void;
  resultKey: string;
}

function ResultItem({
  active,
  onMouseEnter,
  children,
  resultKey,
  innerRef,
}: Readonly<ResultItemProps>) {
  const handleMouseEnter = React.useCallback(() => {
    onMouseEnter(resultKey);
  }, [onMouseEnter, resultKey]);

  return (
    <StyledItem className={classNames({ active })} onMouseEnter={handleMouseEnter} ref={innerRef}>
      {children}
    </StyledItem>
  );
}

const StyledItem = styled.li`
  &:focus,
  &.active {
    background-color: ${themeColor('dropdownMenuHover')};

    h3 {
      color: ${themeColor('linkActive')};
    }
  }
`;

const StyledNote = styled(Note)`
  .active & {
    text-decoration: underline;
  }
`;
