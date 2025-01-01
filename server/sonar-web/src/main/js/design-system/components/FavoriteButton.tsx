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

import classNames from 'classnames';
import * as React from 'react';
import { InteractiveIcon } from './InteractiveIcon';
import { StarFillIcon, StarIcon } from './icons';

interface Props {
  className?: string;
  favorite: boolean;
  innerRef?: React.Ref<HTMLButtonElement>;
  overlay: string;
  toggleFavorite: VoidFunction;
  tooltip?: React.ComponentType<React.PropsWithChildren<{ content: React.ReactNode }>>;
}

export function FavoriteButton(props: Props) {
  const { className, favorite, overlay, toggleFavorite, tooltip, innerRef } = props;
  const Tooltip = tooltip ?? React.Fragment;

  return (
    <Tooltip content={overlay}>
      <InteractiveIcon
        Icon={favorite ? StarFillIcon : StarIcon}
        aria-label={overlay}
        className={classNames('it__favorite-link', className, {
          'it__is-filled': favorite,
        })}
        innerRef={innerRef}
        onClick={toggleFavorite}
        size="small"
      />
    </Tooltip>
  );
}
