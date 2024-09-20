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
import { useIntl } from 'react-intl';
import tw from 'twin.macro';
import {
  LAYOUT_VIEWPORT_MAX_WIDTH_LARGE,
  PopupPlacement,
  PopupZLevel,
  themeColor,
  themeContrast,
} from '../helpers';
import { useResizeObserver } from '../hooks/useResizeObserver';
import { Dropdown } from './Dropdown';
import { InteractiveIcon } from './InteractiveIcon';
import { Tooltip } from './Tooltip';
import { ChevronDownIcon, ChevronRightIcon } from './icons';

const WIDTH_OF_BREADCRUMB_DROPDOWN = 32;

interface Props {
  actions?: React.ReactNode;
  ariaLabel?: string;
  breadcrumbLimit?: number;
  children: React.ReactNode;
  className?: string;
  expandButtonLabel?: string;
  innerRef?: React.RefObject<HTMLElement>;
  maxWidth?: number;
}

export function Breadcrumbs(props: Props) {
  const {
    ariaLabel,
    breadcrumbLimit,
    className,
    children,
    expandButtonLabel,
    innerRef,
    actions,
    maxWidth = LAYOUT_VIEWPORT_MAX_WIDTH_LARGE,
  } = props;
  const [lengthOfChildren, setLengthOfChildren] = React.useState<number[]>([]);

  const intl = useIntl();

  const breadcrumbRef = React.useCallback((node: HTMLLIElement, index: number) => {
    setLengthOfChildren((value) => {
      if (value[index] === node.offsetWidth) {
        return value;
      }

      const newValue = [...value];
      newValue[index] = node.offsetWidth;
      return newValue;
    });
  }, []);

  let hiddenBreadcrumbsCount = 0;

  const modifiedChildren = React.useMemo(() => {
    const childrenArray = React.Children.toArray(children).reverse();

    setLengthOfChildren((value) => {
      if (childrenArray.length === value.length) {
        return value;
      }
      return value.slice(0, childrenArray.length);
    });

    return React.Children.map(childrenArray, (child, index) => {
      const isLast = index === 0;
      return (
        <li
          ref={(node) => {
            if (node !== null) {
              breadcrumbRef(node, index);
            }
          }}
        >
          {child}
          {!isLast && <ChevronRightIcon data-testid="chevron-right" />}
        </li>
      );
    });
  }, [children, breadcrumbRef]);

  const onlyVisibleBreadcrumbs: JSX.Element[] = [];
  const widthOfChildrens = lengthOfChildren.reduce((sum, value) => sum + value, 0);
  if (widthOfChildrens > Math.ceil(maxWidth)) {
    let accumulatedBreadcrumbSize = WIDTH_OF_BREADCRUMB_DROPDOWN;
    lengthOfChildren.forEach((breadcrumbSize, index) => {
      const isBelowExplicitBreadcrumbLimit = breadcrumbLimit ? index + 1 <= breadcrumbLimit : true;
      accumulatedBreadcrumbSize += breadcrumbSize;

      const isLastBreadcrumb = index === 0; // always render the last breadcrumb

      if (isLastBreadcrumb && breadcrumbSize > maxWidth) {
        onlyVisibleBreadcrumbs.push(
          <Tooltip
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            content={modifiedChildren[index].props.children as React.ReactNode}
            key={modifiedChildren[index].key}
          >
            {modifiedChildren[index]}
          </Tooltip>,
        );
      } else if (
        isLastBreadcrumb ||
        (accumulatedBreadcrumbSize <= maxWidth && isBelowExplicitBreadcrumbLimit)
      ) {
        onlyVisibleBreadcrumbs.push(modifiedChildren[index]);
      } else {
        hiddenBreadcrumbsCount += 1;
      }
    });
  }

  const showDropdownMenu = hiddenBreadcrumbsCount > 0;
  const breadcrumbsToShow = showDropdownMenu ? onlyVisibleBreadcrumbs : modifiedChildren;

  return (
    <BreadcrumbWrapper
      aria-label={ariaLabel ?? intl.formatMessage({ id: 'breadcrumbs' })}
      className={classNames('js-breadcrumbs', className)}
      ref={innerRef}
    >
      {showDropdownMenu && (
        <Dropdown
          allowResizing
          className="sw-px-2"
          id="breadcrumb-menu"
          overlay={React.Children.map(children, (child) => (
            <li>{child}</li>
          ))}
          placement={PopupPlacement.BottomLeft}
          zLevel={PopupZLevel.Global}
        >
          <InteractiveIcon
            Icon={ChevronDownIcon}
            aria-label={expandButtonLabel ?? intl.formatMessage({ id: 'expand_breadcrumb' })}
            className="sw-m-1 sw-mr-2"
            size="small"
          />
        </Dropdown>
      )}
      <ul className="sw-truncate sw-leading-6 sw-flex">{[...breadcrumbsToShow].reverse()}</ul>
      {actions && <div className="sw-mx-2">{actions}</div>}
    </BreadcrumbWrapper>
  );
}

export function BreadcrumbsFullWidth(props: Omit<Props, 'innerRef' | 'maxWidth'>) {
  const containerRef = React.useRef(null);
  const [width = LAYOUT_VIEWPORT_MAX_WIDTH_LARGE] = useResizeObserver(containerRef);

  return <Breadcrumbs {...props} innerRef={containerRef} maxWidth={width} />;
}

const BreadcrumbWrapper = styled.nav`
  ${tw`sw-flex sw-items-center`}
  ${tw`sw-truncate`}
  ${tw`sw-typo-default`}

  color: ${themeContrast('breadcrumb')};
  background-color: ${themeColor('breadcrumb')};
`;
