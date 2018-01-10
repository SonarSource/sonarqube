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
import { StickerType } from './utils';
import { translate } from '../../../helpers/l10n';

interface Props {
  onClick: (type: StickerType) => void;
  selected: boolean;
  type: StickerType;
  url: string;
}

export default class StickerButton extends React.PureComponent<Props> {
  handleClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClick(this.props.type);
  };
  render() {
    return (
      <a
        className={classNames('sticker-button', { selected: this.props.selected })}
        href=""
        onClick={this.handleClick}>
        <img src={this.props.url} alt={translate('overview.stickers', this.props.type, 'alt')} />
      </a>
    );
  }
}
