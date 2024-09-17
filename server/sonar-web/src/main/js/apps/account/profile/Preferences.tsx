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
import { SubHeading, Switch } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
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
    <>
      <SubHeading as="h2">{translate('my_account.preferences')}</SubHeading>
      <ul>
        <li>
          <div>{translate('my_account.preferences.keyboard_shortcuts')}</div>
          <div className="sw-flex sw-flex-row">
            <div className="sw-max-w-3/4">
              <FormattedMessage
                id="my_account.preferences.keyboard_shortcuts.description"
                defaultMessage={translate('my_account.preferences.keyboard_shortcuts.description')}
                values={{
                  questionMark: (
                    <span className="markdown">
                      <code>?</code>
                    </span>
                  ),
                }}
              />
            </div>
            <Switch
              name={translate('my_account.preferences.keyboard_shortcuts')}
              onChange={handleToggleKeyboardShortcut}
              value={shortcutsPreferenceValue}
            />
          </div>
        </li>
      </ul>
    </>
  );
}
