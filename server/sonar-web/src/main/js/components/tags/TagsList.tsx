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
import * as classNames from 'classnames';
import './TagsList.css';
import DropdownIcon from '../icons-components/DropdownIcon';

interface Props {
  allowUpdate?: boolean;
  className?: string;
  tags: string[];
}

export default function TagsList({ allowUpdate = false, className, tags }: Props) {
  return (
    <span className={classNames('tags-list', className)} title={tags.join(', ')}>
      <i className="icon-tags" />
      <span className="text-ellipsis">{tags.join(', ')}</span>
      {allowUpdate && <DropdownIcon />}
    </span>
  );
}
