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
import * as React from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../helpers';
import { isDefined } from '../helpers/types';
import { ButtonProps, ButtonSecondary } from '../sonar-aligned/components/buttons';

export type FacetItemProps = Omit<ButtonProps, 'name' | 'onClick'> & {
  active?: boolean;
  /** Disable the item if its value is 0. True by default. */
  disableZero?: boolean;
  name: string | React.ReactNode;
  onClick: (x: string, multiple?: boolean) => void;
  small?: boolean;
  stat?: React.ReactNode;
  statBarPercent?: number;
  /** Textual version of `name` */
  tooltip?: string;
  value: string;
};

const STATBAR_MAX_WIDTH = 60;

export function BaseFacetItem({
  active = false,
  className,
  disabled: disabledProp = false,
  disableZero = true,
  icon,
  name,
  onClick,
  small,
  stat,
  statBarPercent,
  tooltip,
  value,
}: FacetItemProps) {
  // alow an active facet to be disabled even if it now has a "0" stat
  // (it was activated when a different value of My issues/All/New code was selected)
  const disabled = disabledProp || (disableZero && !active && stat !== undefined && stat === 0);

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();

    onClick(value, event.ctrlKey || event.metaKey);
  };

  return (
    <StyledItem active={active} className={classNames({ active }, className)} role="listitem">
      <StyledButton
        active={active}
        aria-checked={active}
        aria-label={typeof name === 'string' ? name : undefined}
        data-facet={value}
        disabled={disabled}
        icon={icon}
        onClick={handleClick}
        role="checkbox"
        small={small}
        title={tooltip}
      >
        <div className="container">
          <span className="name">{name}</span>
          <div>
            <span className="stat">{stat}</span>
            {isDefined(statBarPercent) && (
              <FacetStatBar>
                <FacetStatBarInner
                  style={{ '--statBarWidth': `${statBarPercent * STATBAR_MAX_WIDTH}px` }}
                />
              </FacetStatBar>
            )}
          </div>
        </div>
      </StyledButton>
    </StyledItem>
  );
}

BaseFacetItem.displayName = 'FacetItem'; // so that tests don't see the obfuscated production name

export const FacetItem = styled(BaseFacetItem)``;

const StyledButton = styled(ButtonSecondary)<{ active?: boolean; small?: boolean }>`
  ${tw`sw-body-sm`};
  ${tw`sw-box-border`};
  ${tw`sw-h-7`};
  ${tw`sw-px-1`};
  ${tw`sw-rounded-1`};
  ${tw`sw-w-full`};

  ${({ small }) => (small ? tw`sw-body-xs sw-pr-0` : '')};

  --background: ${({ active }) => (active ? themeColor('facetItemSelected') : 'transparent')};
  --backgroundHover: ${({ active }) => (active ? themeColor('facetItemSelected') : 'transparent')};

  --border: none;

  & div.container {
    ${tw`sw-container`};
    ${tw`sw-flex`};
    ${tw`sw-items-center`};
    ${tw`sw-justify-between`};

    & span.name {
      ${tw`sw-pr-1`};
      ${tw`sw-truncate`};

      & mark {
        background-color: ${themeColor('searchHighlight')};
        font-weight: 400;
      }
    }

    & span.stat {
      color: ${themeColor('facetItemLight')};
    }
  }

  &:disabled {
    background-color: transparent;
    border-color: transparent;

    & span.container span.stat {
      color: ${themeContrast('buttonDisabled')};
    }

    &:hover {
      background-color: transparent;
      border-color: transparent;
    }
  }
`;

/*&:hover {
    --border: ${themeBorder('default', 'facetItemSelectedBorder')};
  }*/

const StyledItem = styled.span<{ active: boolean }>`
  border: ${({ active }) =>
    active
      ? themeBorder('default', 'facetItemSelectedBorder')
      : themeBorder('default', 'transparent')};

  border-radius: 0.25rem;

  &:hover,
  &:active,
  &:focus {
    border-color: ${themeColor('facetItemSelectedBorder')};
  }
`;

const FacetStatBar = styled.div`
  ${tw`sw-inline-block`}
  ${tw`sw-ml-2`}

  width: ${STATBAR_MAX_WIDTH}px;
`;

const FacetStatBarInner = styled.div`
  width: var(--statBarWidth);
  min-width: 5px;
  height: 10px;
  background-color: ${themeColor('facetItemGraph')};
  transition: width 0.3s ease;
`;

export const HighlightedFacetItems = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;

  ${FacetItem} {
    &:is(:hover, .active) {
      border-color: ${themeColor('facetItemSelectedBorder')};
      padding-bottom: 1px;
      border-bottom-width: 0;
      border-bottom-right-radius: 0rem;
      border-bottom-left-radius: 0rem;

      &:last-of-type {
        padding-bottom: 0;
        border-bottom-width: 1px;
        border-radius: 0.25rem;
      }

      & ~ ${FacetItem} {
        border-color: ${themeColor('facetItemSelectedBorder')};
        padding-bottom: 1px;
        padding-top: 1px;
        border-top-width: 0;
        border-bottom-width: 0;
        border-radius: 0;
      }

      & ~ ${FacetItem}:last-of-type {
        padding-bottom: 0;
        border-bottom-width: 1px;
        border-bottom-right-radius: 0.25rem;
        border-bottom-left-radius: 0.25rem;
      }
    }

    &.active {
      background-color: ${themeColor('facetItemSelected')};

      & ~ ${FacetItem} {
        background-color: ${themeColor('facetItemSelected')};
      }

      & ~ ${FacetItem}:hover, & ~ ${FacetItem}:hover ~ ${FacetItem} {
        background-color: ${themeColor('facetItemSelectedHover')};
      }
    }

    &.active ~ ${FacetItem}:hover, &:hover ~ ${FacetItem}.active {
      padding-top: 0;
      border-top-width: 1px;
    }
  }
`;
