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
import React from 'react';
import {
  LAYOUT_FILTERBAR_HEADER,
  LAYOUT_FOOTER_HEIGHT,
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LAYOUT_PROJECT_NAV_HEIGHT,
  themeBorder,
  themeColor,
} from '~design-system';
import { translate } from '../../helpers/l10n';

import useFollowScroll from '../../hooks/useFollowScroll';

export type LayoutFilterBarSize = 'default' | 'large';

const HEADER_PADDING_BOTTOM = 24;
const HEADER_PADDING = 32 + HEADER_PADDING_BOTTOM; //32 padding top and 24 padding bottom

interface Props {
  className?: string;
  content: React.ReactNode;
  contentClassName?: string;
  filterbar: React.ReactNode;
  filterbarContentClassName?: string;
  filterbarHeader?: React.ReactNode;
  filterbarHeaderClassName?: string;
  filterbarRef?: React.RefObject<HTMLDivElement>;
  header?: React.ReactNode;
  headerHeight?: number;
  id?: string;
  size?: LayoutFilterBarSize;
  withBorderLeft?: boolean;
}

export default function FilterBarTemplate(props: Readonly<Props>) {
  const {
    className,
    content,
    contentClassName,
    header,
    headerHeight = 0,
    id,
    filterbarRef,
    filterbar,
    filterbarHeader,
    filterbarHeaderClassName,
    filterbarContentClassName,
    size = 'default',
    withBorderLeft = false,
  } = props;

  const headerHeightWithPadding = headerHeight ? headerHeight + HEADER_PADDING : 0;
  const { top: topScroll, scrolledOnce } = useFollowScroll();
  const distanceFromBottom = topScroll + window.innerHeight - document.body.scrollHeight;
  const footerVisibleHeight =
    (scrolledOnce &&
      (distanceFromBottom > -LAYOUT_FOOTER_HEIGHT
        ? LAYOUT_FOOTER_HEIGHT + distanceFromBottom
        : 0)) ||
    0;

  return (
    <>
      {header && (
        <div
          className="sw-flex sw-pb-6 sw-box-border"
          style={{
            height: `${headerHeight + HEADER_PADDING_BOTTOM}px`,
          }}
        >
          {header}
        </div>
      )}
      <div
        className={classNames(
          'sw-grid sw-grid-cols-12 sw-w-full sw-px-14 sw-box-border',
          className,
        )}
        id={id}
      >
        <Filterbar
          className={classNames('sw-z-filterbar', {
            'sw-col-span-3': size === 'default',
            'sw-col-span-4': size === 'large',
            bordered: Boolean(header),
            'sw-mt-0': Boolean(header),
            'sw-rounded-t-1': Boolean(header),
            'border-left': withBorderLeft,
          })}
          ref={filterbarRef}
          style={{
            height: `calc(100vh - ${
              LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_PROJECT_NAV_HEIGHT
            }px - ${footerVisibleHeight}px)`,
            top: LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_PROJECT_NAV_HEIGHT + headerHeightWithPadding,
          }}
        >
          {filterbarHeader && (
            <FilterbarHeader
              className={classNames(
                'sw-w-full sw-top-0 sw-px-4 sw-py-2 sw-z-filterbar-header',
                filterbarHeaderClassName,
              )}
            >
              {filterbarHeader}
            </FilterbarHeader>
          )}
          <FilterbarContent
            aria-label={translate('secondary')}
            className={classNames('sw-p-4 js-page-filter', filterbarContentClassName)}
          >
            {filterbar}
          </FilterbarContent>
        </Filterbar>
        <Main
          className={classNames(
            'sw-relative sw-pl-12',
            {
              'sw-col-span-9': size === 'default',
              'sw-col-span-8': size === 'large',
            },
            'js-page-main',
            contentClassName,
          )}
        >
          {content}
        </Main>
      </div>
    </>
  );
}

const Filterbar = styled.div`
  position: sticky;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
  background-color: ${themeColor('filterbar')};
  border-right: ${themeBorder('default', 'filterbarBorder')};

  &.border-left {
    border-left: ${themeBorder('default', 'filterbarBorder')};
  }

  &.bordered {
    border: ${themeBorder('default', 'filterbarBorder')};
  }
`;

const FilterbarContent = styled.nav`
  position: relative;
  box-sizing: border-box;
  width: 100%;
`;

const FilterbarHeader = styled.div`
  position: sticky;
  box-sizing: border-box;
  height: ${LAYOUT_FILTERBAR_HEADER}px;
  background-color: inherit;
  border-bottom: ${themeBorder('default')};
`;

const Main = styled.div`
  flex-grow: 1;
`;
