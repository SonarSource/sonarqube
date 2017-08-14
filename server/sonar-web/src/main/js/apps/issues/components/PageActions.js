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
import IssuesCounter from './IssuesCounter';
import ReloadButton from './ReloadButton';
import type { Paging } from '../utils';
import { translate } from '../../../helpers/l10n';

type Props = {|
  loading: boolean,
  onReload: () => void,
  paging: ?Paging,
  selectedIndex: ?number
|};

export default class PageActions extends React.PureComponent {
  props: Props;

  renderShortcuts() {
    return (
      <span className="note big-spacer-right">
        <span className="big-spacer-right">
          <span className="shortcut-button little-spacer-right">↑</span>
          <span className="shortcut-button little-spacer-right">↓</span>
          {translate('issues.to_select_issues')}
        </span>

        <span>
          <span className="shortcut-button little-spacer-right">←</span>
          <span className="shortcut-button little-spacer-right">→</span>
          {translate('issues.to_navigate')}
        </span>
      </span>
    );
  }

  render() {
    const { paging, selectedIndex } = this.props;

    return (
      <div className="pull-right">
        {this.renderShortcuts()}

        <div className="issues-page-actions">
          {this.props.loading
            ? <i className="issues-main-header-spinner spinner spacer-right" />
            : <ReloadButton className="spacer-right" onClick={this.props.onReload} />}
          {paging != null && <IssuesCounter current={selectedIndex} total={paging.total} />}
        </div>
      </div>
    );
  }
}
