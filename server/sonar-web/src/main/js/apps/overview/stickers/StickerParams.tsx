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
import Select from '../../../components/controls/Select';
import { StickerColors, StickerType, StickerOptions } from './utils';
import { translate } from '../../../helpers/l10n';

interface Props {
  className?: string;
  options: StickerOptions;
  type: StickerType;
  updateOptions: (options: Partial<StickerOptions>) => void;
}

export default class StickerParams extends React.PureComponent<Props> {
  getColorOptions = () =>
    ['white', 'black', 'orange'].map(color => ({
      label: translate('overview.stickers.options.colors', color),
      value: color
    }));

  handleColorChange = ({ value }: { value: StickerColors }) =>
    this.props.updateOptions({ color: value });

  render() {
    const { className, options, type } = this.props;
    switch (type) {
      case StickerType.marketing:
        return (
          <div className={className}>
            <label className="big-spacer-right" htmlFor="sticker-color">
              {translate('color')}
            </label>
            <Select
              className="input-medium"
              clearable={false}
              name="sticker-color"
              onChange={this.handleColorChange}
              options={this.getColorOptions()}
              searchable={false}
              value={options.color}
            />
          </div>
        );
      default:
        return null;
    }
  }
}
