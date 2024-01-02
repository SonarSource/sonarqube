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
import * as React from 'react';
import BackButton from '../../../components/controls/BackButton';
import PageShortcutsTooltip from '../../../components/ui/PageShortcutsTooltip';
import { translate } from '../../../helpers/l10n';

export interface ConciseIssuesListHeaderProps {
  displayBackButton: boolean;
  loading: boolean;
  onBackClick: () => void;
}

export default function ConciseIssuesListHeader(props: ConciseIssuesListHeaderProps) {
  const { displayBackButton, loading } = props;

  return (
    <header className="layout-page-header-panel concise-issues-list-header">
      <div className="layout-page-header-panel-inner concise-issues-list-header-inner display-flex-center display-flex-space-between">
        {displayBackButton && <BackButton disabled={loading} onClick={props.onBackClick} />}
        <PageShortcutsTooltip
          leftLabel={translate('issues.to_navigate_back')}
          upAndDownLabel={translate('issues.to_select_issues')}
          metaModifierLabel={translate('issues.to_navigate_issue_locations')}
        />
        {loading && <i className="spinner" />}
      </div>
    </header>
  );
}
