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
import { Children, ElementType, Fragment, HtmlHTMLAttributes, ReactNode } from 'react';
import tw from 'twin.macro';
import { themeBorder, themeColor } from '../../helpers/theme';
import { isDefined } from '../../helpers/types';

interface Props extends HtmlHTMLAttributes<HTMLDivElement> {
  as?: ElementType;
  children: ReactNode;
  className?: string;
}

export function SubnavigationGroup({ as, className, children, ...htmlProps }: Readonly<Props>) {
  const childrenArray = Children.toArray(children).filter(isDefined);
  return (
    <Group as={as} className={className} {...htmlProps}>
      {childrenArray.map((child, index) => (
        <Fragment key={index}>
          {child}
          {index < childrenArray.length - 1 && <Separator />}
        </Fragment>
      ))}
    </Group>
  );
}

const Group = styled.div`
  ${tw`sw-relative`}
  ${tw`sw-flex sw-flex-col`}
  ${tw`sw-w-full`}

  background-color: ${themeColor('subnavigation')};
  border: ${themeBorder('default', 'subnavigationBorder')};
`;

const Separator = styled.div`
  ${tw`sw-w-full`}

  height: 1px;
  background-color: ${themeColor('subnavigationSeparator')};
`;
