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
import { ClockIcon, ItemLink, StarFillIcon, TextBold, TextMuted } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { getComponentOverviewUrl } from '../../../helpers/urls';
import { ComponentResult } from './utils';

interface Props {
  component: ComponentResult;
  innerRef: (componentKey: string, node: HTMLElement | null) => void;
  onClose: () => void;
  onSelect: (componentKey: string) => void;
  selected: boolean;
}
export default class GlobalSearchResult extends React.PureComponent<Props> {
  doSelect = () => {
    this.props.onSelect(this.props.component.key);
  };

  render() {
    const { component, selected } = this.props;
    const to = getComponentOverviewUrl(component.key, component.qualifier);
    return (
      <ItemLink
        className={classNames('sw-flex sw-flex-col sw-items-start sw-space-y-1', {
          active: selected,
        })}
        innerRef={(node: HTMLAnchorElement | null) => {
          this.props.innerRef(component.key, node);
        }}
        key={component.key}
        onClick={this.props.onClose}
        onPointerEnter={this.doSelect}
        to={to}
      >
        <div className="sw-flex sw-justify-between sw-items-center sw-w-full">
          <TextBold match={component.match} name={component.name} />
          <div className="sw-ml-2">
            {component.isFavorite && <StarFillIcon />}
            {!component.isFavorite && component.isRecentlyBrowsed && (
              <ClockIcon aria-label={translate('recently_browsed')} />
            )}
          </div>
        </div>
        <TextMuted text={component.key} />
      </ItemLink>
    );
  }
}
