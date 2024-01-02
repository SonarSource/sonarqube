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
import { FormattedMessage } from 'react-intl';
import Toggle from '../../../components/controls/Toggle';
import { translate } from '../../../helpers/l10n';
import {
  getKeyboardShortcutEnabled,
  setKeyboardShortcutEnabled,
} from '../../../helpers/preferences';

export function Preferences() {
  const [shortcutsPreferenceValue, setShortcutsPreferenceValue] = React.useState(
    getKeyboardShortcutEnabled(),
  );

  const handleToggleKeyboardShortcut = React.useCallback(
    (value: boolean) => {
      setKeyboardShortcutEnabled(value);
      setShortcutsPreferenceValue(value);
    },
    [setShortcutsPreferenceValue],
  );

  return (
    <div className="boxed-group">
      <div className="boxed-group-inner">
        <h2 className="big-spacer-bottom">{translate('my_account.preferences')}</h2>
        <ul>
          <li>
            <div className="text-bold spacer-bottom">
              {translate('my_account.preferences.keyboard_shortcuts')}
            </div>
            <div className="display-flex-row">
              <div className="width-50 big-padded-right">
                <FormattedMessage
                  id="my_account.preferences.keyboard_shortcuts.description"
                  defaultMessage={translate(
                    'my_account.preferences.keyboard_shortcuts.description',
                  )}
                  values={{
                    questionMark: (
                      <span className="markdown">
                        <code>?</code>
                      </span>
                    ),
                  }}
                />
              </div>
              <Toggle
                ariaLabel={
                  shortcutsPreferenceValue
                    ? translate('my_account.preferences.keyboard_shortcuts.enabled')
                    : translate('my_account.preferences.keyboard_shortcuts.disabled')
                }
                onChange={handleToggleKeyboardShortcut}
                value={shortcutsPreferenceValue}
              />
            </div>
          </li>
        </ul>
      </div>
    </div>
  );
}
