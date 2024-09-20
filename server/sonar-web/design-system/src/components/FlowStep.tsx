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
import tw from 'twin.macro';
import { themeColor } from '../helpers/theme';
import { BaseLink } from './Link';
import { LocationMarker, StyledMarker } from './LocationMarker';

interface Props {
  additionalMarkers?: React.ReactNode;
  className?: string;
  message?: string;
  onClick?: () => void;
  selected: boolean;
  step?: number;
}

export function FlowStep(props: Props) {
  const { additionalMarkers, className, message, selected, step } = props;

  return (
    <StyledLink className={className} onClick={props.onClick} to={{}}>
      <>
        <LocationMarker selected={selected} text={step} />
        {additionalMarkers}
      </>
      <span>{message}</span>
    </StyledLink>
  );
}

const StyledLink = styled(BaseLink)`
  ${tw`sw-p-1 sw-rounded-1/2`}
  ${tw`sw-flex sw-items-center sw-flex-wrap sw-gap-2`}
  ${tw`sw-typo-default`}
  
  color: ${themeColor('pageContent')};
  border-bottom: none;

  &.selected,
  &:hover {
    background-color: ${themeColor('codeLineLocationSelected')};
  }

  &:hover ${StyledMarker} {
    background-color: ${themeColor('codeLineLocationMarkerSelected')};
  }
`;
