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
import { BadgeType } from './utils';
import { Button } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  onClick: (type: BadgeType) => void;
  selected: boolean;
  type: BadgeType;
  url: string;
}

export default class BadgeButton extends React.PureComponent<Props> {
  handleClick = () => {
    this.props.onClick(this.props.type);
  };

  render() {
    const { selected, type, url } = this.props;
    const width = type !== BadgeType.measure ? '128px' : undefined;
    return (
      <Button className={classNames('badge-button', { selected })} onClick={this.handleClick}>
        <img alt={translate('overview.badges', type, 'alt')} src={url} width={width} />
      </Button>
    );
  }
}
