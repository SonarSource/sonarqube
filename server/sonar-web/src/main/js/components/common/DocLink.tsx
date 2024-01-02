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
import * as React from 'react';
import withAppStateContext, {
  WithAppStateContextProps,
} from '../../app/components/app-state/withAppStateContext';
import { getUrlForDoc } from '../../helpers/docs';
import Link, { LinkProps } from './Link';

type Props = WithAppStateContextProps &
  Omit<LinkProps, 'to'> & { to: string; innerRef?: React.Ref<HTMLAnchorElement> };

export function DocLink({ appState, to, innerRef, ...props }: Props) {
  const toStatic = getUrlForDoc(appState.version, to);
  return <Link ref={innerRef} to={toStatic} target="_blank" {...props} />;
}

export default withAppStateContext(DocLink);
