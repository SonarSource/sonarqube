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
import classNames from 'classnames';
import { Link } from 'gatsby';
import * as React from 'react';
import { MarkdownRemark } from '../@types/graphql-types';
import { getMarkdownRemarkTitle, getMarkdownRemarkUrl } from './utils';

interface Props {
  className?: string;
  location: Location;
  node?: MarkdownRemark;
}

const PREFIX = process.env.GATSBY_DOCS_VERSION ? '/' + process.env.GATSBY_DOCS_VERSION : '';

export default function PageLink({ className, location, node }: Props) {
  const title = getMarkdownRemarkTitle(node);
  const url = getMarkdownRemarkUrl(node);

  if (!url || !title) {
    return null;
  }

  return (
    <div>
      <Link
        className={classNames(className, { active: location.pathname === PREFIX + url })}
        to={url}>
        {title}
      </Link>
    </div>
  );
}
