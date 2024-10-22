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

import { LinkStandalone } from '@sonarsource/echoes-react';
import * as React from 'react';
import { ContentCell, Key, KeyboardHint, Modal, SubTitle, Table, TableRow } from '~design-system';
import { isInput } from '../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../helpers/keycodes';
import { translate } from '../../helpers/l10n';
import { getKeyboardShortcutEnabled } from '../../helpers/preferences';

type Section = {
  rows: Array<{ command: string; description: string }>;
  subTitle: string;
};

const FILE_ROWS = [
  {
    command: `${Key.ArrowUp} ${Key.ArrowDown}`,
    description: 'keyboard_shortcuts_modal.code_page.select_files',
  },
  {
    command: `${Key.ArrowRight}`,
    description: 'keyboard_shortcuts_modal.code_page.open_file',
  },
  {
    command: `${Key.ArrowLeft}`,
    description: 'keyboard_shortcuts_modal.return_back_to_the_list',
  },
];

export const SECTIONS: Array<Section> = [
  {
    rows: [
      {
        command: 's',
        description: 'keyboard_shortcuts_modal.global.open_search_bar',
      },
      {
        command: '?',
        description: 'keyboard_shortcuts_modal.global.open_keyboard_shortcuts_modal',
      },
    ],
    subTitle: 'keyboard_shortcuts_modal.global',
  },

  {
    rows: [
      {
        command: `${Key.ArrowUp} ${Key.ArrowDown}`,
        description: 'keyboard_shortcuts_modal.navigate_between_issues',
      },
      {
        command: `${Key.ArrowRight}`,
        description: 'keyboard_shortcuts_modal.open_issue',
      },
      {
        command: `${Key.ArrowLeft}`,
        description: 'keyboard_shortcuts_modal.return_back_to_the_list',
      },
      {
        command: `${Key.Alt} + ${Key.ArrowUp} ${Key.ArrowDown}`,
        description: 'keyboard_shortcuts_modal.issue_details_page.navigate_issue_locations',
      },
      {
        command: `${Key.Alt} + ${Key.ArrowLeft} ${Key.ArrowRight}`,
        description: 'keyboard_shortcuts_modal.issue_details_page.switch_flows',
      },
      {
        command: 'f',
        description: 'keyboard_shortcuts_modal.do_issue_transition',
      },
      {
        command: 'a',
        description: 'keyboard_shortcuts_modal.assign_issue',
      },
      {
        command: 'm',
        description: 'keyboard_shortcuts_modal.assign_issue_to_me',
      },
      {
        command: 't',
        description: 'keyboard_shortcuts_modal.change_tags_of_issue',
      },
      {
        command: `${Key.Control} + ${Key.Enter}`,
        description: 'keyboard_shortcuts_modal.issue_details_page.submit_comment',
      },
    ],
    subTitle: 'keyboard_shortcuts_modal.issues_page',
  },

  {
    rows: FILE_ROWS,
    subTitle: 'keyboard_shortcuts_modal.code_page',
  },

  {
    rows: FILE_ROWS,
    subTitle: 'keyboard_shortcuts_modal.measures_page',
  },

  {
    rows: [
      {
        command: `${Key.ArrowUp} ${Key.ArrowDown}`,
        description: 'keyboard_shortcuts_modal.rules_page.navigate_between_rule',
      },
      {
        command: `${Key.ArrowRight}`,
        description: 'keyboard_shortcuts_modal.rules_page.open_rule',
      },
      {
        command: `${Key.ArrowLeft}`,
        description: 'keyboard_shortcuts_modal.return_back_to_the_list',
      },
    ],
    subTitle: 'keyboard_shortcuts_modal.rules_page',
  },
];

function renderSection() {
  return SECTIONS.map((section) => (
    <div key={section.subTitle} className="sw-mb-4">
      <SubTitle>{translate(section.subTitle)}</SubTitle>

      <Table columnCount={2} columnWidths={['30%', '70%']}>
        {section.rows.map((row) => (
          <TableRow key={row.command}>
            <ContentCell className="sw-justify-center">
              <KeyboardHint command={row.command} title="" />
            </ContentCell>

            <ContentCell>{translate(row.description)}</ContentCell>
          </TableRow>
        ))}
      </Table>
    </div>
  ));
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

  const title = translate('keyboard_shortcuts_modal.title');

  const body = (
    <>
      <LinkStandalone
        onClick={() => {
          setDisplay(false);
          return true;
        }}
        to="/account"
      >
        {translate('keyboard_shortcuts_modal.disable_link')}
      </LinkStandalone>

      <div className="sw-mt-4">{renderSection()}</div>
    </>
  );

  return (
    <Modal
      body={body}
      headerTitle={title}
      onClose={() => setDisplay(false)}
      secondaryButtonLabel={translate('close')}
    />
  );
}
