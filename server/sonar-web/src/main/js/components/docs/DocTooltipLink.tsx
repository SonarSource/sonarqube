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
import { Link } from 'react-router';
import { forEach } from 'lodash';
import DetachIcon from '../icons-components/DetachIcon';

interface OwnProps {
  customProps?: T.Dict<string>;
}

type Props = OwnProps & React.AnchorHTMLAttributes<HTMLAnchorElement>;

const SONARCLOUD_LINK = '/#sonarcloud#/';

export default function DocTooltipLink({ children, customProps, href, ...other }: Props) {
  if (customProps) {
    forEach(customProps, (value, key) => {
      if (href) {
        href = href.replace(`#${key}#`, encodeURIComponent(value));
      }
    });
  }

  if (href && href.startsWith('/')) {
    if (href.startsWith(SONARCLOUD_LINK)) {
      href = `/${href.substr(SONARCLOUD_LINK.length)}`;
    } else {
      href = `/documentation/${href.substr(1)}`;
    }

    return (
      <Link rel="noopener noreferrer" target="_blank" to={href} {...other}>
        {children}
      </Link>
    );
  }

  return (
    <>
      <a href={href} rel="noopener noreferrer" target="_blank" {...other}>
        {children}
      </a>
      <DetachIcon className="little-spacer-left little-spacer-right vertical-baseline" size={12} />
    </>
  );
}
