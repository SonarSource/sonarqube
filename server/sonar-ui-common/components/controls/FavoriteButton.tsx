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
import { translate } from '../../helpers/l10n';
import FavoriteIcon from '../icons/FavoriteIcon';
import { ButtonLink } from './buttons';
import Tooltip from './Tooltip';

export interface Props {
  className?: string;
  favorite: boolean;
  qualifier: string;
  toggleFavorite: () => void;
}

export default class FavoriteButton extends React.PureComponent<Props> {
  render() {
    const { className, favorite, qualifier, toggleFavorite } = this.props;
    const tooltip = favorite
      ? translate('favorite.current', qualifier)
      : translate('favorite.check', qualifier);
    const ariaLabel = translate('favorite.action', favorite ? 'remove' : 'add');

    return (
      <Tooltip overlay={tooltip}>
        <ButtonLink
          aria-label={ariaLabel}
          className={classNames('favorite-link', 'link-no-underline', className)}
          onClick={toggleFavorite}>
          <FavoriteIcon favorite={favorite} />
        </ButtonLink>
      </Tooltip>
    );
  }
}
