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
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import FilesCounter from './FilesCounter';
import { translate } from '../../../helpers/l10n';
import type { Paging } from '../types';

type Props = {|
  current: ?number,
  loading: boolean,
  isFile: ?boolean,
  paging: ?Paging,
  view?: string
|};

export default class PageActions extends React.PureComponent {
  props: Props;

  renderShortcuts() {
    return (
      <span className="note big-spacer-right">
        <span className="big-spacer-right">
          <span className="shortcut-button little-spacer-right">↑</span>
          <span className="shortcut-button little-spacer-right">↓</span>
          {translate('component_measures.to_select_files')}
        </span>

        <span>
          <span className="shortcut-button little-spacer-right">←</span>
          <span className="shortcut-button little-spacer-right">→</span>
          {translate('component_measures.to_navigate')}
        </span>
      </span>
    );
  }

  renderFileShortcuts() {
    return (
      <span className="note big-spacer-right">
        <span>
          <span className="shortcut-button little-spacer-right">←</span>
          {translate('component_measures.to_navigate_back')}
        </span>
      </span>
    );
  }

  render() {
    const { isFile, paging, view } = this.props;
    const showShortcuts = ['list', 'tree'].includes(view);
    return (
      <div className="pull-right">
        {!isFile && showShortcuts && this.renderShortcuts()}
        {isFile && this.renderFileShortcuts()}
        <div className="measure-details-page-actions">
          <DeferredSpinner loading={this.props.loading} />
          {paging != null &&
            <FilesCounter
              className="spacer-left"
              current={this.props.current}
              total={paging.total}
            />}
        </div>
      </div>
    );
  }
}
