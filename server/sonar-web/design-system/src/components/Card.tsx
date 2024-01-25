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
import * as React from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor } from '../helpers/theme';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  children: React.ReactNode;
}

export function Card(props: Readonly<CardProps>) {
  const { children, ...rest } = props;

  return <CardStyled {...rest}>{children}</CardStyled>;
}

export function GreyCard(props: Readonly<CardProps>) {
  const { children, ...rest } = props;

  return <GreyCardStyled {...rest}>{children}</GreyCardStyled>;
}

export function LightGreyCard(props: Readonly<CardProps>) {
  const { children, ...rest } = props;

  return <LightGreyCardStyled {...rest}>{children}</LightGreyCardStyled>;
}

export const CardWithPrimaryBackground = styled(Card)`
  background-color: ${themeColor('backgroundPrimary')};
`;

const CardStyled = styled.div`
  background-color: ${themeColor('backgroundSecondary')};
  border: ${themeBorder('default', 'projectCardBorder')};

  ${tw`sw-p-6`};
  ${tw`sw-rounded-1`};
`;

const LightGreyCardStyled = styled(CardStyled)`
  border: ${themeBorder('default')};
`;

const GreyCardStyled = styled(CardStyled)`
  border: ${themeBorder('default', 'almCardBorder')};
`;
