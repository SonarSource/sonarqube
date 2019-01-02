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
import { Link } from 'react-router';
import { DocumentationEntry } from '../utils';

interface Props {
  indent: boolean;
  node: DocumentationEntry | undefined;
  splat: string;
}

export function MenuItem({ indent, node, splat }: Props) {
  if (!node) {
    return null;
  }

  const active = node.url === '/' + splat;
  return (
    <Link
      className={classNames('list-group-item', { active })}
      key={node.url}
      style={{ paddingLeft: indent ? 31 : 10 }}
      to={'/documentation' + node.url}>
      <h3 className="list-group-item-heading">{node.navTitle || node.title}</h3>
    </Link>
  );
}
