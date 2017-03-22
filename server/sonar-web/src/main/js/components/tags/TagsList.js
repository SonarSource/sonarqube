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
import './TagsList.css';

type Props = {
  tags: Array<string>,
  allowUpdate: boolean,
  allowMultiLine: boolean,
  customClass?: string
};

export default class TagsList extends React.PureComponent {
  props: Props;

  static defaultProps = {
    allowUpdate: false,
    allowMultiLine: false
  };

  render() {
    const { tags, allowUpdate } = this.props;
    const spanClass = classNames({
      note: !allowUpdate,
      'text-ellipsis': !this.props.allowMultiLine
    });
    const tagListClass = classNames('tags-list', this.props.customClass);

    return (
      <span className={tagListClass} title={tags.join(', ')}>
        <i className="icon-tags icon-half-transparent" />
        <span className={spanClass}>{tags.join(', ')}</span>
        {allowUpdate && <i className="icon-dropdown icon-half-transparent" />}
      </span>
    );
  }
}
