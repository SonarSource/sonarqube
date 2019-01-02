/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as classNames from 'classnames';
import { isDarkColor } from '../../helpers/colors';
import { getBaseUrl } from '../../helpers/urls';
import './IdentityProviderLink.css';

interface Props {
  children: React.ReactNode;
  className?: string;
  identityProvider: T.IdentityProvider;
  onClick?: () => void;
  small?: boolean;
  url: string | undefined;
}

export default function IdentityProviderLink({
  children,
  className,
  identityProvider,
  onClick,
  small,
  url
}: Props) {
  const size = small ? 14 : 20;

  return (
    <a
      className={classNames(
        'identity-provider-link',
        { 'dark-text': !isDarkColor(identityProvider.backgroundColor), small },
        className
      )}
      href={url}
      onClick={onClick}
      style={{ backgroundColor: identityProvider.backgroundColor }}>
      <img
        alt={identityProvider.name}
        height={size}
        src={getBaseUrl() + identityProvider.iconPath}
        width={size}
      />
      {children}
    </a>
  );
}
