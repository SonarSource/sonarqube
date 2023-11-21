/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import * as React from 'react';
import tw from 'twin.macro';
import { PopupPlacement, PopupZLevel } from '../helpers';
import { themeColor, themeContrast } from '../helpers/theme';
import { Dropdown } from './Dropdown';
import { LightLabel } from './Text';
import { WrapperButton } from './buttons';

interface Props {
  allowUpdate?: boolean;
  ariaTagsListLabel: string;
  className?: string;
  emptyText: string;
  menuId?: string;
  onClose?: VoidFunction;
  open?: boolean;
  overlay?: React.ReactNode;
  popupPlacement?: PopupPlacement;
  tags: string[];
  tagsToDisplay?: number;
  tooltip?: React.ComponentType<React.PropsWithChildren<{ overlay: React.ReactNode }>>;
}

export function Tags({
  allowUpdate = false,
  ariaTagsListLabel,
  className,
  emptyText,
  menuId = '',
  overlay,
  popupPlacement,
  tags,
  tagsToDisplay = 3,
  tooltip,
  open,
  onClose,
}: Props) {
  const displayedTags = tags.slice(0, tagsToDisplay);
  const extraTags = tags.slice(tagsToDisplay);
  const Tooltip = tooltip || React.Fragment;

  const displayedTagsContent = (open = false) => (
    <Tooltip overlay={open ? undefined : tags.join(', ')}>
      <span className="sw-inline-flex sw-items-center sw-gap-1">
        {/* Display first 3 (tagsToDisplay) tags */}
        {displayedTags.map((tag) => (
          <TagLabel key={tag}>{tag}</TagLabel>
        ))}

        {/* Show ellipsis if there are more tags */}
        {extraTags.length > 0 ? <TagLabel>...</TagLabel> : null}

        {/* Handle no tags with its own styling */}
        {tags.length === 0 && <LightLabel>{emptyText}</LightLabel>}
      </span>
    </Tooltip>
  );

  return (
    <span
      aria-label={`${ariaTagsListLabel}: ${tags.join(', ')}`}
      className={classNames('sw-cursor-default sw-flex sw-items-center', className)}
    >
      {allowUpdate ? (
        <Dropdown
          allowResizing
          closeOnClick={false}
          id={menuId}
          onClose={onClose}
          openDropdown={open}
          overlay={overlay}
          placement={popupPlacement}
          zLevel={PopupZLevel.Global}
        >
          {({ a11yAttrs, onToggleClick, open }) => (
            <WrapperButton
              className="sw-flex sw-items-center sw-gap-1 sw-p-0 sw-h-auto sw-rounded-0"
              onClick={onToggleClick}
              {...a11yAttrs}
            >
              {displayedTagsContent(open)}
              <TagLabel className="sw-cursor-pointer">+</TagLabel>
            </WrapperButton>
          )}
        </Dropdown>
      ) : (
        <span>{displayedTagsContent()}</span>
      )}
    </span>
  );
}

const TagLabel = styled.span`
  color: ${themeContrast('tag')};
  background: ${themeColor('tag')};

  ${tw`sw-body-sm`}
  ${tw`sw-box-border`}
  ${tw`sw-truncate`}
  ${tw`sw-rounded-1/2`}
  ${tw`sw-px-1 sw-py-1/2`}
  ${tw`sw-max-w-32`}
`;
