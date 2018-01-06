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
// @flow
import React from 'react';
import { Link } from 'react-router';
import classNames from 'classnames';
import { scaleLinear } from 'd3-scale';
import LinkIcon from '../icons-components/LinkIcon';
import Tooltip from '../controls/Tooltip';

const SIZE_SCALE = scaleLinear()
  .domain([3, 15])
  .range([11, 18])
  .clamp(true);

/*:: type Props = {|
  x: number,
  y: number,
  width: number,
  height: number,
  fill: string,
  label: string,
  prefix: string,
  icon?: React.Element<*>,
  tooltip?: string | React.Element<*>,
  itemKey: string,
  link?: string,
  onClick?: string => void,
  placement?: string
|}; */

export default class TreeMapRect extends React.PureComponent {
  /*:: props: Props; */

  handleLinkClick = (e /*: Event */) => e.stopPropagation();

  handleRectClick = () => {
    if (this.props.onClick != null) {
      this.props.onClick(this.props.itemKey);
    }
  };

  renderLink = () => {
    const { link, height, width } = this.props;
    const hasMinSize = width >= 24 && height >= 24 && (width >= 48 || height >= 50);
    if (!hasMinSize || link == null) {
      return null;
    }
    return (
      <Link className="treemap-link" to={link} onClick={this.handleLinkClick}>
        <LinkIcon />
      </Link>
    );
  };

  renderCell = () => {
    const cellStyles = {
      left: this.props.x,
      top: this.props.y,
      width: this.props.width,
      height: this.props.height,
      backgroundColor: this.props.fill,
      fontSize: SIZE_SCALE(this.props.width / this.props.label.length),
      lineHeight: `${this.props.height}px`,
      cursor: this.props.onClick != null ? 'pointer' : 'default'
    };
    const isTextVisible = this.props.width >= 40 && this.props.height >= 45;
    const isIconVisible = this.props.width >= 24 && this.props.height >= 26;

    const label = this.props.prefix
      ? `${this.props.prefix}<br>${this.props.label.substr(this.props.prefix.length)}`
      : this.props.label;

    return (
      <div
        className="treemap-cell"
        style={cellStyles}
        onClick={this.handleRectClick}
        role="treeitem"
        tabIndex={0}>
        <div className="treemap-inner" style={{ maxWidth: this.props.width }}>
          {isIconVisible && (
            <span className={classNames('treemap-icon', { 'spacer-right': isTextVisible })}>
              {this.props.icon}
            </span>
          )}
          {isTextVisible && (
            <span className="treemap-text" dangerouslySetInnerHTML={{ __html: label }} />
          )}
        </div>
        {this.renderLink()}
      </div>
    );
  };

  render() {
    const { placement, tooltip } = this.props;
    if (tooltip != null) {
      return (
        <Tooltip overlay={tooltip} placement={placement || 'left'} mouseLeaveDelay={0}>
          {this.renderCell()}
        </Tooltip>
      );
    }
    return this.renderCell();
  }
}
