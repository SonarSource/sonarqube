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
import { BaseLink, LocationMarker, StyledMarker, themeColor } from 'design-system';
import React, { useCallback, useEffect, useMemo, useRef } from 'react';
import { translate } from '../../../helpers/l10n';

interface Props {
  concealed?: boolean;
  index: number;
  message: string | undefined;
  onClick: (index: number) => void;
  selected: boolean;
}

export default function IssueLocation(props: Props) {
  const { index, message, selected, concealed, onClick } = props;
  const node = useRef<HTMLElement | null>(null);
  const locationType = useMemo(() => getLocationType(message), [message]);
  const normalizedMessage = useMemo(() => message?.replace(/^(source|sink): /i, ''), [message]);

  useEffect(() => {
    if (selected && node.current) {
      node.current.scrollIntoView({
        block: 'nearest',
        behavior: 'smooth',
      });
    }
  }, [selected]);

  const handleClick = useCallback(
    (event: React.MouseEvent<HTMLAnchorElement>) => {
      event.preventDefault();
      onClick(index);
    },
    [index, onClick],
  );

  return (
    <StyledLink
      aria-label={normalizedMessage}
      aria-current={selected}
      onClick={handleClick}
      to={{}}
    >
      <StyledLocation
        className={classNames('sw-p-1 sw-rounded-1/2 sw-flex sw-gap-2 sw-body-sm', {
          selected,
        })}
        ref={(n) => (node.current = n)}
      >
        <LocationMarker selected={selected} text={concealed ? undefined : index + 1} />
        <span>
          {locationType && (
            <LocationMarker
              className="sw-inline sw-mr-2"
              selected={selected}
              text={locationType.toUpperCase()}
            />
          )}
          <StyledLocationName>
            {normalizedMessage ?? translate('issue.unnamed_location')}
          </StyledLocationName>
        </span>
      </StyledLocation>
    </StyledLink>
  );
}

const StyledLocation = styled.div`
  &.selected,
  &:hover {
    background-color: ${themeColor('codeLineLocationSelected')};
  }

  &:hover ${StyledMarker} {
    background-color: ${themeColor('codeLineLocationMarkerSelected')};
  }
`;

const StyledLink = styled(BaseLink)`
  color: ${themeColor('pageContent')};
  border: none;
`;

const StyledLocationName = styled.span`
  word-break: break-word;
`;

function getLocationType(message?: string) {
  if (message?.toLowerCase().startsWith('source')) {
    return 'source';
  } else if (message?.toLowerCase().startsWith('sink')) {
    return 'sink';
  }
  return null;
}
