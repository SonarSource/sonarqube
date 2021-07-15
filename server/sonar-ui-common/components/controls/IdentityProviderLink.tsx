/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { isDarkColor } from '../../helpers/colors';
import { getBaseUrl } from '../../helpers/urls';
import './IdentityProviderLink.css';

interface Props {
  backgroundColor: string;
  children: React.ReactNode;
  className?: string;
  iconPath: string;
  name: string;
  onClick?: () => void;
  small?: boolean;
  url: string | undefined;
}

export default function IdentityProviderLink({
  backgroundColor,
  children,
  className,
  iconPath,
  name,
  onClick,
  small,
  url,
}: Props) {
  const size = small ? 14 : 20;

  return (
    <a
      className={classNames(
        'identity-provider-link',
        { 'dark-text': !isDarkColor(backgroundColor), small },
        className
      )}
      href={url}
      onClick={onClick}
      style={{ backgroundColor }}>
      <img alt={name} height={size} src={getBaseUrl() + iconPath} width={size} />
      {children}
    </a>
  );
}
