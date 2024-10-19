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
import { uniqueId } from 'lodash';
import * as React from 'react';
import tw from 'twin.macro';
import { themeColor } from '../helpers';
import { BareButton } from '../sonar-aligned/components/buttons';
import { Badge } from './Badge';
import { DestructiveIcon } from './InteractiveIcon';
import { Spinner } from './Spinner';
import { Tooltip as SCTooltip } from './Tooltip';
import { OpenCloseIndicator } from './icons';
import { CloseIcon } from './icons/CloseIcon';

export interface FacetBoxProps {
  ariaLabel?: string;
  children: React.ReactNode;
  className?: string;
  clearIconLabel?: string;
  count?: number;
  countLabel?: string;
  'data-property'?: string;
  disabled?: boolean;
  disabledHelper?: string;
  hasEmbeddedFacets?: boolean;
  help?: React.ReactNode;
  id?: string;
  inner?: boolean;
  loading?: boolean;
  name: string;
  onClear?: () => void;
  onClick?: (isOpen: boolean) => void;
  open?: boolean;
  tooltipComponent?: React.ComponentType<React.PropsWithChildren<{ content: React.ReactNode }>>;
}

export function FacetBox(props: FacetBoxProps) {
  const {
    ariaLabel,
    children,
    className,
    clearIconLabel,
    count,
    countLabel,
    'data-property': dataProperty,
    disabled = false,
    disabledHelper,
    hasEmbeddedFacets = false,
    help,
    id: idProp,
    inner = false,
    loading = false,
    name,
    onClear,
    onClick,
    open = false,
    tooltipComponent,
  } = props;

  const clearable = !disabled && Boolean(onClear) && count !== undefined && count > 0;
  const counter = count ?? 0;
  const expandable = !disabled && Boolean(onClick);
  const id = React.useMemo(() => idProp ?? uniqueId('filter-facet-'), [idProp]);
  const Tooltip = tooltipComponent ?? SCTooltip;
  return (
    <Accordion
      className={classNames(className, { open })}
      data-property={dataProperty}
      hasEmbeddedFacets={hasEmbeddedFacets}
      inner={inner}
    >
      <Header>
        <TitleWithHelp>
          <ChevronAndTitle
            aria-controls={`${id}-panel`}
            aria-disabled={!expandable}
            aria-expanded={open}
            aria-label={ariaLabel ?? name}
            expandable={expandable}
            id={`${id}-header`}
            onClick={() => {
              if (!disabled) {
                onClick?.(!open);
              }
            }}
          >
            {expandable && <OpenCloseIndicator aria-hidden open={open} />}

            {disabled ? (
              <Tooltip content={disabledHelper}>
                <HeaderTitle
                  aria-disabled
                  aria-label={`${name}, ${disabledHelper ?? ''}`}
                  disabled={disabled}
                >
                  {name}
                </HeaderTitle>
              </Tooltip>
            ) : (
              <HeaderTitle>{name}</HeaderTitle>
            )}
          </ChevronAndTitle>
          {help && <span className="sw-ml-1">{help}</span>}
        </TitleWithHelp>

        {<Spinner loading={loading} />}

        {counter > 0 && (
          <BadgeAndIcons>
            <Badge title={countLabel} variant="counter">
              {counter}
            </Badge>

            {Boolean(clearable) && (
              <Tooltip content={clearIconLabel}>
                <ClearIcon
                  Icon={CloseIcon}
                  aria-label={clearIconLabel ?? ''}
                  data-testid={`clear-${name}`}
                  onClick={onClear}
                  size="small"
                />
              </Tooltip>
            )}
          </BadgeAndIcons>
        )}
      </Header>

      {open && (
        <div aria-labelledby={`${id}-header`} id={`${id}-panel`} role="group">
          {children}
        </div>
      )}
    </Accordion>
  );
}

FacetBox.displayName = 'FacetBox'; // so that tests don't see the obfuscated production name

const Accordion = styled.div<{
  hasEmbeddedFacets?: boolean;
  inner?: boolean;
}>`
  ${tw`sw-flex-col`};
  ${tw`sw-flex`};
  ${tw`sw-gap-3`};

  ${({ hasEmbeddedFacets }) => (hasEmbeddedFacets ? tw`sw-gap-0` : '')};

  ${({ inner }) => (inner ? tw`sw-gap-1 sw-ml-3 sw-mt-1` : '')};
`;

const BadgeAndIcons = styled.div`
  ${tw`sw-flex`};
  ${tw`sw-gap-2`};
`;

const TitleWithHelp = styled.div`
  ${tw`sw-flex`};
  ${tw`sw-items-center`};
`;

const ChevronAndTitle = styled(BareButton)<{
  expandable?: boolean;
}>`
  ${tw`sw-flex`};
  ${tw`sw-gap-1`};
  ${tw`sw-h-9`};
  ${tw`sw-items-center`};

  cursor: ${({ expandable }) => (expandable ? 'pointer' : 'default')};
`;

const ClearIcon = styled(DestructiveIcon)`
  --color: ${themeColor('dangerButton')};
`;

const Header = styled.div`
  ${tw`sw-flex`};
  ${tw`sw-gap-3`};
  ${tw`sw-items-center`};
  ${tw`sw-justify-between`};
`;

const HeaderTitle = styled.span<{
  disabled?: boolean;
}>`
  ${tw`sw-typo-semibold`};

  color: ${({ disabled }) =>
    disabled ? themeColor('facetHeaderDisabled') : themeColor('facetHeader')};

  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'inherit')};
`;
