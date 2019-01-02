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
import IssuesCounter from './IssuesCounter';
import TotalEffort from './TotalEffort';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import ReloadButton from '../../../components/controls/ReloadButton';
import { translate } from '../../../helpers/l10n';
import { isSonarCloud } from '../../../helpers/system';

interface Props {
  canSetHome: boolean;
  effortTotal: number | undefined;
  onReload: () => void;
  paging: T.Paging | undefined;
  selectedIndex: number | undefined;
}

export default class PageActions extends React.PureComponent<Props> {
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
    const { effortTotal, paging, selectedIndex } = this.props;

    return (
      <div className="pull-right">
        {this.renderShortcuts()}

        <div className="issues-page-actions">
          <ReloadButton onClick={this.props.onReload} />
          {paging != null && (
            <IssuesCounter className="spacer-left" current={selectedIndex} total={paging.total} />
          )}
          {effortTotal !== undefined && <TotalEffort effort={effortTotal} />}
        </div>

        {this.props.canSetHome && (
          <HomePageSelect
            className="huge-spacer-left"
            currentPage={isSonarCloud() ? { type: 'MY_ISSUES' } : { type: 'ISSUES' }}
          />
        )}
      </div>
    );
  }
}
