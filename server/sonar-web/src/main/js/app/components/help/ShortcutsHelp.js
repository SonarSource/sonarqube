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
import { translate } from '../../../helpers/l10n';

export default function ShortcutsHelp() {
  return (
    <div>
      <h2 className="spacer-top spacer-bottom">{translate('help.section.shortcuts')}</h2>

      <div className="columns">
        <div className="column-half">
          <div className="spacer-bottom">
            <h3 className="shortcuts-section-title">{translate('shortcuts.section.global')}</h3>
            <ul className="shortcuts-list">
              <li>
                <span className="shortcut-button spacer-right">s</span>
                {translate('shortcuts.section.global.search')}
              </li>
              <li>
                <span className="shortcut-button spacer-right">?</span>
                {translate('shortcuts.section.global.shortcuts')}
              </li>
            </ul>
          </div>

          <h3 className="shortcuts-section-title">{translate('shortcuts.section.rules')}</h3>
          <ul className="shortcuts-list">
            <li>
              <span className="shortcut-button little-spacer-right">↑</span>
              <span className="shortcut-button spacer-right">↓</span>
              {translate('shortcuts.section.rules.navigate_between_rules')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">→</span>
              {translate('shortcuts.section.rules.open_details')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">←</span>
              {translate('shortcuts.section.rules.return_to_list')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">a</span>
              {translate('shortcuts.section.rules.activate')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">d</span>
              {translate('shortcuts.section.rules.deactivate')}
            </li>
          </ul>
        </div>

        <div className="column-half">
          <h3 className="shortcuts-section-title">{translate('shortcuts.section.issues')}</h3>
          <ul className="shortcuts-list">
            <li>
              <span className="shortcut-button little-spacer-right">↑</span>
              <span className="shortcut-button spacer-right">↓</span>
              {translate('shortcuts.section.issues.navigate_between_issues')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">→</span>
              {translate('shortcuts.section.issues.open_details')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">←</span>
              {translate('shortcuts.section.issues.return_to_list')}
            </li>
            <li>
              <span className="shortcut-button little-spacer-right">alt</span>
              <span className="little-spacer-right">+</span>
              <span className="shortcut-button little-spacer-right">↑</span>
              <span className="shortcut-button spacer-right">↓</span>
              {translate('issues.to_navigate_issue_locations')}
            </li>
            <li>
              <span className="shortcut-button little-spacer-right">alt</span>
              <span className="little-spacer-right">+</span>
              <span className="shortcut-button little-spacer-right">←</span>
              <span className="shortcut-button spacer-right">→</span>
              {translate('issues.to_switch_flows')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">f</span>
              {translate('shortcuts.section.issue.do_transition')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">a</span>
              {translate('shortcuts.section.issue.assign')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">m</span>
              {translate('shortcuts.section.issue.assign_to_me')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">i</span>
              {translate('shortcuts.section.issue.change_severity')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">c</span>
              {translate('shortcuts.section.issue.comment')}
            </li>
            <li>
              <span className="shortcut-button little-spacer-right">ctrl</span>
              <span className="shortcut-button spacer-right">enter</span>
              {translate('shortcuts.section.issue.submit_comment')}
            </li>
            <li>
              <span className="shortcut-button spacer-right">t</span>
              {translate('shortcuts.section.issue.change_tags')}
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
