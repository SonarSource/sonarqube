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
import { translate } from '../../helpers/l10n';
import './TagsList.css';

type Props = {
  tags?: Array<string>,
  allowUpdate?: boolean,
  hideWhenNoTags?: boolean,
  allowMultiLine?: boolean
};

export default class SearchableFilterFooter extends React.PureComponent {
  props: Props;

  render () {
    const { tags, hideWhenNoTags, allowUpdate } = this.props;
    const spanClass = classNames('note', {
      'single-line': !this.props.allowMultiLine
    });
    let tagsText;

    if (tags && tags.length) {
      tagsText = tags.join(', ');
    } else if (hideWhenNoTags) {
      return null;
    } else {
      tagsText = translate('no_tags');
    }

    return (
      <span className="tags-list" title={tagsText}>
        <i className="icon-tags icon-half-transparent"/>
        <span className={spanClass}>{tagsText}</span>
        {allowUpdate && <i className="icon-dropdown icon-half-transparent"/>}
      </span>
    );
  }
}
