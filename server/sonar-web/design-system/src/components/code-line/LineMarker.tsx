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
import { forwardRef, Ref, useCallback, useRef } from 'react';
import tw from 'twin.macro';
import { themeColor, themeContrast } from '../../helpers/theme';
import { LocationMarker } from '../LocationMarker';

interface Props {
  hideLocationIndex?: boolean;
  index: number;
  leading?: boolean;
  message?: string;
  onLocationSelect?: (index: number) => void;
  selected: boolean;
}

function LineMarkerFunc(
  { hideLocationIndex, index, leading, message, onLocationSelect, selected }: Props,
  ref: Ref<HTMLElement>,
) {
  const element = useRef<HTMLDivElement | null>(null);
  const elementMessage = useRef<HTMLDivElement | null>(null);

  const handleClick = useCallback(() => {
    onLocationSelect?.(index);
  }, [index, onLocationSelect]);

  return (
    <Wrapper className={classNames({ leading })} ref={element}>
      <LocationMarker
        onClick={handleClick}
        ref={ref as React.RefObject<HTMLDivElement>}
        selected={selected}
        text={hideLocationIndex ? undefined : index + 1}
      />
      {message && <Message ref={elementMessage}>{message}</Message>}
    </Wrapper>
  );
}

const Message = styled.div`
  ${tw`sw-absolute`}
  ${tw`sw-typo-default`}
  ${tw`sw-rounded-1/2`}
  ${tw`sw-px-1`}
  ${tw`sw-left-0`}

  z-index: 1;
  bottom: calc(100% + 0.25rem);
  width: max-content;
  max-width: var(--max-width);
  color: ${themeContrast('codeLineIssueMessageTooltip')};
  background-color: ${themeColor('codeLineIssueMessageTooltip')};
  visibility: hidden;

  &.message-right {
    ${tw`sw-left-auto`}
    ${tw`sw-right-0`}
  }
`;

const Wrapper = styled.div`
  ${tw`sw-relative`}
  ${tw`sw-inline-block`}
  ${tw`sw-align-top`}

  &:not(:first-of-type) {
    ${tw`sw-ml-1`}
  }

  &.leading {
    margin-left: calc(var(--width) - 0.25rem);
  }

  &:hover ${Message} {
    visibility: visible;
  }
`;

export const LineMarker = forwardRef<HTMLElement, Props>(LineMarkerFunc);
