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

import { Spinner } from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { ItemButton } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';

interface Props {
  allowMore: boolean;
  loadingMore?: string;
  onMoreClick: (qualifier: string) => void;
  onSelect: (qualifier: string) => void;
  qualifier: string;
  selected: boolean;
}

export default class GlobalSearchShowMore extends React.PureComponent<Props> {
  handleMoreClick = (event: React.MouseEvent<HTMLButtonElement>, qualifier: string) => {
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.blur();

    if (qualifier !== '') {
      this.props.onMoreClick(qualifier);
    }
  };

  handleMouseEnter = (qualifier: string) => {
    if (qualifier !== '') {
      this.props.onSelect(`qualifier###${qualifier}`);
    }
  };

  render() {
    const { loadingMore, qualifier, selected, allowMore } = this.props;

    return (
      <ItemButton
        className={classNames({ active: selected })}
        disabled={!allowMore}
        onClick={(e: React.MouseEvent<HTMLButtonElement>) => {
          this.handleMoreClick(e, qualifier);
        }}
        onPointerEnter={() => {
          this.handleMouseEnter(qualifier);
        }}
      >
        <Spinner isLoading={loadingMore === qualifier}>{translate('show_more')}</Spinner>
      </ItemButton>
    );
  }
}
