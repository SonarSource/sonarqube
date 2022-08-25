/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { forEach } from 'lodash';
import * as React from 'react';
import { Dict } from '../../types/types';
import Link from '../common/Link';

interface OwnProps {
  customProps?: Dict<string>;
}

type Props = OwnProps & React.AnchorHTMLAttributes<HTMLAnchorElement>;

export default function DocTooltipLink({ children, customProps, href, ...other }: Props) {
  if (customProps) {
    forEach(customProps, (value, key) => {
      if (href) {
        href = href.replace(`#${key}#`, encodeURIComponent(value));
      }
    });
  }

  if (href && href.startsWith('/')) {
    href = `/documentation/${href.substr(1)}`;

    return (
      <Link target="_blank" to={href} {...other}>
        {children}
      </Link>
    );
  }

  return href ? (
    <Link size={12} to={href} target="_blank" {...other}>
      {children}
    </Link>
  ) : null;
}
