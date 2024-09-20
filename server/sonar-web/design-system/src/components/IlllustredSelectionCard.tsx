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
import { ReactNode } from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor } from '../helpers/theme';
import { BareButton } from '../sonar-aligned/components/buttons';

interface Props {
  className?: string;
  description: ReactNode;
  image: ReactNode;
  onClick: () => void;
  selected: boolean;
}

export function IllustratedSelectionCard(props: Props) {
  const { className, description, image, onClick, selected } = props;

  return (
    <StyledSelectionCard className={classNames(className, { selected })} onClick={onClick}>
      <ImageContainer>{image}</ImageContainer>
      <DescriptionContainer>
        <Note>{description}</Note>
      </DescriptionContainer>
    </StyledSelectionCard>
  );
}

const Note = styled.span`
  color: ${themeColor('pageContentLight')};

  ${tw`sw-typo-default`}
`;

const ImageContainer = styled.div`
  min-height: 116px;
  flex: 1;
  background: ${themeColor('backgroundPrimary')};
  ${tw`sw-flex`}
  ${tw`sw-justify-center sw-items-center`}
  ${tw`sw-rounded-t-1`}
`;

const DescriptionContainer = styled.div`
  background: ${themeColor('backgroundSecondary')};
  border-top: ${themeBorder()};
  ${tw`sw-rounded-b-1`}
  ${tw`sw-p-4`}
`;

export const StyledSelectionCard = styled(BareButton)`
  ${tw`sw-flex`}
  ${tw`sw-flex-col`}
  ${tw`sw-rounded-1`};

  min-width: 146px;
  border: ${themeBorder('default')};
  transition: border 0.3s ease;

  &:hover,
  &:focus,
  &:active {
    border: ${themeBorder('default', 'primary')};
  }

  &.selected {
    border: ${themeBorder('default', 'primary')};
  }
`;
