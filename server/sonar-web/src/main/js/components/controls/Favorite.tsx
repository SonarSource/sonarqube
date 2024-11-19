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
import { FavoriteButton } from '~design-system';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { useToggleFavoriteMutation } from '../../queries/favorites';
import Tooltip from './Tooltip';

interface Props {
  className?: string;
  component: string;
  componentName?: string;
  favorite: boolean;
  handleFavorite?: (component: string, isFavorite: boolean) => void;
  qualifier: string;
}

export default function Favorite(props: Readonly<Props>) {
  const {
    className,
    componentName,
    qualifier,
    favorite: favoriteP,
    component,
    handleFavorite,
  } = props;
  const buttonRef = React.useRef<HTMLButtonElement>(null);
  // local state of favorite is only needed in case of portfolios, as they are not migrated to query yet
  const [favorite, setFavorite] = React.useState(favoriteP);
  const { mutate } = useToggleFavoriteMutation();

  const toggleFavorite = () => {
    const newFavorite = !favorite;

    return mutate(
      { component, addToFavorites: newFavorite },
      {
        onSuccess: () => {
          setFavorite(newFavorite);
          handleFavorite?.(component, newFavorite);
          buttonRef.current?.focus();
        },
      },
    );
  };

  const actionName = favorite ? 'remove' : 'add';
  const overlay = componentName
    ? translateWithParameters(`favorite.action.${qualifier}.${actionName}_x`, componentName)
    : translate('favorite.action', qualifier, actionName);

  React.useEffect(() => {
    setFavorite(favoriteP);
  }, [favoriteP]);

  return (
    <FavoriteButton
      className={className}
      overlay={overlay}
      toggleFavorite={toggleFavorite}
      tooltip={Tooltip}
      favorite={favorite}
      innerRef={buttonRef}
    />
  );
}
