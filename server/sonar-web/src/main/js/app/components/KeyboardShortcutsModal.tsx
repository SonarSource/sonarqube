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
import Link from '../../components/common/Link';
import { Button } from '../../components/controls/buttons';
import Modal from '../../components/controls/Modal';
import { isInput } from '../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../helpers/keycodes';
import { translate } from '../../helpers/l10n';
import { getKeyboardShortcutEnabled } from '../../helpers/preferences';

type Shortcuts = Array<{
  category: string;
  shortcuts: Array<{
    keys: string[];
    action: string;
  }>;
}>;

const CATEGORIES: { left: Shortcuts; right: Shortcuts } = {
  left: [
    {
      category: 'global',
      shortcuts: [
        { keys: ['s'], action: 'search' },
        { keys: ['?'], action: 'open_shortcuts' },
      ],
    },
    {
      category: 'issues_page',
      shortcuts: [
        { keys: ['↑', '↓'], action: 'navigate' },
        { keys: ['→'], action: 'source_code' },
        { keys: ['←'], action: 'back' },
        { keys: ['alt', '+', '↑', '↓'], action: 'navigate_locations' },
        { keys: ['alt', '+', '←', '→'], action: 'switch_flows' },
        { keys: ['f'], action: 'transition' },
        { keys: ['a'], action: 'assign' },
        { keys: ['m'], action: 'assign_to_me' },
        { keys: ['i'], action: 'severity' },
        { keys: ['c'], action: 'comment' },
        { keys: ['ctrl', '+', 'enter'], action: 'submit_comment' },
        { keys: ['t'], action: 'tags' },
      ],
    },
  ],
  right: [
    {
      category: 'code_page',
      shortcuts: [
        { keys: ['↑', '↓'], action: 'select_files' },
        { keys: ['→'], action: 'open_file' },
        { keys: ['←'], action: 'back' },
      ],
    },
    {
      category: 'measures_page',
      shortcuts: [
        { keys: ['↑', '↓'], action: 'select_files' },
        { keys: ['→'], action: 'open_file' },
        { keys: ['←'], action: 'back' },
      ],
    },
    {
      category: 'rules_page',
      shortcuts: [
        { keys: ['↑', '↓'], action: 'navigate' },
        { keys: ['→'], action: 'rule_details' },
        { keys: ['←'], action: 'back' },
      ],
    },
  ],
};

function renderShortcuts(list: Shortcuts) {
  return (
    <>
      {list.map(({ category, shortcuts }) => (
        <div key={category} className="spacer-bottom">
          <h3 className="null-spacer-top">{translate('keyboard_shortcuts', category, 'title')}</h3>
          <table>
            <thead>
              <tr>
                <th>{translate('keyboard_shortcuts.shortcut')}</th>
                <th>{translate('keyboard_shortcuts.action')}</th>
              </tr>
            </thead>
            <tbody>
              {shortcuts.map(({ action, keys }) => (
                <tr key={action}>
                  <td>
                    {keys.map((k) =>
                      k === '+' ? (
                        <span key={k} className="little-spacer-right">
                          {k}
                        </span>
                      ) : (
                        <code key={k} className="little-spacer-right">
                          {k}
                        </code>
                      ),
                    )}
                  </td>
                  <td>{translate('keyboard_shortcuts', category, action)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
    </>
  );
}

export default function KeyboardShortcutsModal() {
  const [display, setDisplay] = React.useState(false);

  React.useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (!getKeyboardShortcutEnabled()) {
        return true;
      }

      if (isInput(event)) {
        return true;
      }

      if (event.key === KeyboardKeys.KeyQuestionMark) {
        setDisplay((d) => !d);
      }
    };

    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [setDisplay]);

  if (!display) {
    return null;
  }

  const title = translate('keyboard_shortcuts.title');

  return (
    <Modal contentLabel={title} onRequestClose={() => setDisplay(false)} size="medium">
      <div className="modal-head display-flex-space-between">
        <h2>{title}</h2>
        <Link
          to="/account"
          onClick={() => {
            setDisplay(false);
            return true;
          }}
        >
          {translate('keyboard_shortcuts.disable_link')}
        </Link>
      </div>

      <div className="modal-body modal-container markdown display-flex-start shortcuts-modal">
        <div className="flex-1">{renderShortcuts(CATEGORIES.left)}</div>
        <div className="flex-1 huge-spacer-left">{renderShortcuts(CATEGORIES.right)}</div>
      </div>

      <div className="modal-foot">
        <Button onClick={() => setDisplay(false)}>{translate('close')}</Button>
      </div>
    </Modal>
  );
}
