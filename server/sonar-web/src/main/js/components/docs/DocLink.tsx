/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import DetachIcon from '../../components/icons-components/DetachIcon';

const SONARCLOUD_LINK = '/#sonarcloud#/';

export default function DocLink(props: React.AnchorHTMLAttributes<HTMLAnchorElement>) {
  const { children, href, ...other } = props;
  if (href && href.startsWith('/')) {
    let url = `/documentation/${href.substr(1)}`;
    if (href.startsWith(SONARCLOUD_LINK)) {
      url = `/${href.substr(SONARCLOUD_LINK.length)}`;
    }
    return (
      <Link to={url} {...other}>
        {children}
      </Link>
    );
  }

  return (
    <>
      <a href={href} rel="noopener noreferrer" target="_blank" {...other}>
        {children}
      </a>
      <DetachIcon
        className="text-muted little-spacer-left little-spacer-right vertical-baseline"
        size={12}
      />
    </>
  );
}
