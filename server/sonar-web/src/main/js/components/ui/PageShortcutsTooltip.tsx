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
import Tooltip from '../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../helpers/l10n';

export interface PageShortcutsTooltipProps {
  className?: string;
  leftAndRightLabel?: string;
  leftLabel?: string;
  upAndDownLabel?: string;
  metaModifierLabel?: string;
}

export default function PageShortcutsTooltip(props: PageShortcutsTooltipProps) {
  const { className, leftAndRightLabel, leftLabel, upAndDownLabel, metaModifierLabel } = props;
  return (
    <Tooltip
      overlay={
        <div className="small nowrap">
          <div>
            {upAndDownLabel && (
              <span>
                <span className="shortcut-button little-spacer-right">↑</span>
                <span className="shortcut-button spacer-right">↓</span>
                {upAndDownLabel}
              </span>
            )}
            {leftAndRightLabel && (
              <span className={classNames({ 'big-spacer-left': upAndDownLabel })}>
                <span className="shortcut-button little-spacer-right">←</span>
                <span className="shortcut-button spacer-right">→</span>
                {leftAndRightLabel}
              </span>
            )}
            {leftLabel && (
              <span className={classNames({ 'big-spacer-left': upAndDownLabel })}>
                <span className="shortcut-button spacer-right">←</span>
                {leftLabel}
              </span>
            )}
          </div>
          {metaModifierLabel && (
            <div className="big-spacer-top big-padded-top bordered-top">
              <span className="shortcut-button little-spacer-right">alt</span>
              <span className="little-spacer-right">+</span>
              <span className="shortcut-button little-spacer-right">↑</span>
              <span className="shortcut-button spacer-right">↓</span>
              <span className="shortcut-button little-spacer-right">←</span>
              <span className="shortcut-button spacer-right">→</span>
              {metaModifierLabel}
            </div>
          )}
        </div>
      }
    >
      <aside
        aria-label={`
        ${translate('shortcuts.on_page.intro')}
        ${
          upAndDownLabel
            ? translateWithParameters('shortcuts.on_page.up_down_x', upAndDownLabel)
            : ''
        }
        ${
          leftAndRightLabel
            ? translateWithParameters('shortcuts.on_page.left_right_x', leftAndRightLabel)
            : ''
        }
        ${leftLabel ? translateWithParameters('shortcuts.on_page.left_x', leftLabel) : ''}
        ${
          metaModifierLabel
            ? translateWithParameters('shortcuts.on_page.meta_x', metaModifierLabel)
            : ''
        }
      `}
        className={classNames(
          className,
          'page-shortcuts-tooltip note text-center display-inline-block',
        )}
      >
        <div aria-hidden>
          <div>
            <span className="shortcut-button shortcut-button-tiny">↑</span>
          </div>
          <div>
            <span className="shortcut-button shortcut-button-tiny">←</span>
            <span className="shortcut-button shortcut-button-tiny">↓</span>
            <span className="shortcut-button shortcut-button-tiny">→</span>
          </div>
        </div>
      </aside>
    </Tooltip>
  );
}
