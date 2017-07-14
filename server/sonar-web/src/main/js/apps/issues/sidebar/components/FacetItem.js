/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import classNames from 'classnames';
import { formatMeasure } from '../../../../helpers/measures';

type Props = {|
  active: boolean,
  disabled: boolean,
  facetMode: string,
  halfWidth: boolean,
  name: string | React.Element<*>,
  onClick: string => void,
  stat: ?number,
  value: string
|};

export default class FacetItem extends React.PureComponent {
  props: Props;

  static defaultProps = {
    disabled: false,
    halfWidth: false
  };

  handleClick = (event: Event & { currentTarget: HTMLElement }) => {
    event.preventDefault();
    const value = event.currentTarget.dataset.value;
    this.props.onClick(value);
  };

  render() {
    const { stat } = this.props;

    const className = classNames('facet', 'search-navigator-facet', {
      active: this.props.active,
      'search-navigator-facet-half': this.props.halfWidth
    });

    const formattedStat =
      stat &&
      formatMeasure(stat, this.props.facetMode === 'effort' ? 'SHORT_WORK_DUR' : 'SHORT_INT');

    return this.props.disabled
      ? <span className={className}>
          <span className="facet-name">
            {this.props.name}
          </span>
          <span className="facet-stat">
            {formattedStat}
          </span>
        </span>
      : <a className={className} data-value={this.props.value} href="#" onClick={this.handleClick}>
          <span className="facet-name">
            {this.props.name}
          </span>
          <span className="facet-stat">
            {formattedStat}
          </span>
        </a>;
  }
}
