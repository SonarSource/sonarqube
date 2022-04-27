/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CodingRulesMock from '../../../api/mocks/CodingRulesMock';
import { mockLoggedInUser } from '../../../helpers/testMocks';
import { renderApp } from '../../../helpers/testReactTestingUtils';
import { CurrentUser } from '../../../types/users';
import routes from '../routes';

jest.mock('../../../api/rules');
jest.mock('../../../api/issues');
jest.mock('../../../api/quality-profiles');

let handler: CodingRulesMock;

beforeAll(() => {
  window.scrollTo = jest.fn();
  handler = new CodingRulesMock();
});

afterEach(() => handler.reset());

it('should select rules with keyboard navigation', async () => {
  const user = userEvent.setup();
  renderCodingRulesApp();
  let row = await screen.findByRole('row', { selected: true });
  expect(within(row).getByRole('link', { name: 'Awsome java rule' })).toBeInTheDocument();
  await user.keyboard('{ArrowDown}');
  row = await screen.findByRole('row', { selected: true });
  expect(within(row).getByRole('link', { name: 'Hot hotspot' })).toBeInTheDocument();
  await user.keyboard('{ArrowUp}');
  row = await screen.findByRole('row', { selected: true });
  expect(within(row).getByRole('link', { name: 'Awsome java rule' })).toBeInTheDocument();
  await user.keyboard('{ArrowRight}');
  expect(screen.getByRole('heading', { level: 3, name: 'Awsome java rule' })).toBeInTheDocument();
  await user.keyboard('{ArrowLeft}');
  row = await screen.findByRole('row', { selected: true });
  expect(within(row).getByRole('link', { name: 'Awsome java rule' })).toBeInTheDocument();
});

it('should open with permalink', async () => {
  renderCodingRulesApp(undefined, 'coding_rules?rule_key=rule1');
  expect(await screen.findByRole('link', { name: 'Awsome java rule' })).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'Hot hotspot' })).not.toBeInTheDocument();
});

it('should show open rule', async () => {
  renderCodingRulesApp(undefined, 'coding_rules?open=rule1');
  expect(
    await screen.findByRole('heading', { level: 3, name: 'Awsome java rule' })
  ).toBeInTheDocument();
});

it('should list all rules', async () => {
  renderCodingRulesApp();

  await waitFor(() => {
    handler
      .allRulesName()
      .forEach(name => expect(screen.getByRole('link', { name })).toBeInTheDocument());
  });
});

it('should have all type facet', async () => {
  renderCodingRulesApp();

  await waitFor(() => {
    [
      'issue.type.BUG',
      'issue.type.VULNERABILITY',
      'issue.type.CODE_SMELL',
      'issue.type.SECURITY_HOTSPOT'
    ].forEach(name => expect(screen.getByRole('link', { name })).toBeInTheDocument());
  });
});

it('select the correct quality profile for bulk change base on language search', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser());
  const selectQP = handler.allQualityProfile('js')[0];

  await user.click(await screen.findByRole('link', { name: 'JavaScript' }));
  await user.click(await screen.findByRole('button', { name: 'bulk_change' }));
  await user.click(await screen.findByRole('link', { name: 'coding_rules.activate_in…' }));
  const dialog = screen.getByRole('dialog', {
    name: 'coding_rules.activate_in_quality_profile (2 coding_rules._rules)'
  });

  expect(dialog).toBeInTheDocument();
  const dialogScreen = within(dialog);
  expect(dialogScreen.getByText(`${selectQP.name} - ${selectQP.languageName}`)).toBeInTheDocument();
});

it('no quality profile for bulk cahnge base on language search', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser());

  await user.click(await screen.findByRole('link', { name: 'C' }));
  await user.click(await screen.findByRole('button', { name: 'bulk_change' }));
  await user.click(await screen.findByRole('link', { name: 'coding_rules.activate_in…' }));
  const dialog = screen.getByRole('dialog', {
    name: 'coding_rules.activate_in_quality_profile (1 coding_rules._rules)'
  });

  expect(dialog).toBeInTheDocument();
  const dialogScreen = within(dialog);
  await user.click(dialogScreen.getByRole('textbox', { name: 'coding_rules.activate_in' }));
  expect(dialogScreen.getByText('coding_rules.bulk_change.no_quality_profile')).toBeInTheDocument();
});

it('should be able to bulk activate quality profile', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser());

  const selectQPSuccess = handler.allQualityProfile('java')[0];
  const selectQPWarning = handler.allQualityProfile('java')[1];

  await user.click(await screen.findByRole('button', { name: 'bulk_change' }));
  await user.click(await screen.findByRole('link', { name: 'coding_rules.activate_in…' }));

  const dialog = screen.getByRole('dialog', {
    name: 'coding_rules.activate_in_quality_profile (4 coding_rules._rules)'
  });
  expect(dialog).toBeInTheDocument();

  let dialogScreen = within(dialog);
  await user.click(dialogScreen.getByRole('textbox', { name: 'coding_rules.activate_in' }));
  await user.click(
    dialogScreen.getByText(`${selectQPSuccess.name} - ${selectQPSuccess.languageName}`)
  );
  expect(
    dialogScreen.getByText(`${selectQPSuccess.name} - ${selectQPSuccess.languageName}`)
  ).toBeInTheDocument();

  await user.click(dialogScreen.getByRole('button', { name: 'apply' }));
  expect(
    dialogScreen.getByText(
      `coding_rules.bulk_change.success.${selectQPSuccess.name}.${selectQPSuccess.languageName}.${
        handler.allRulesName().length
      }`
    )
  ).toBeInTheDocument();

  await user.click(dialogScreen.getByRole('button', { name: 'close' }));

  // Try bulk change when quality profile has warnning.
  handler.activateWithWarning();

  await user.click(await screen.findByRole('button', { name: 'bulk_change' }));
  await user.click(await screen.findByRole('link', { name: 'coding_rules.activate_in…' }));
  dialogScreen = within(
    screen.getByRole('dialog', {
      name: 'coding_rules.activate_in_quality_profile (4 coding_rules._rules)'
    })
  );
  await user.click(dialogScreen.getByRole('textbox', { name: 'coding_rules.activate_in' }));
  await user.click(
    dialogScreen.getByText(`${selectQPWarning.name} - ${selectQPWarning.languageName}`)
  );
  await user.click(dialogScreen.getByRole('button', { name: 'apply' }));
  expect(
    dialogScreen.getByText(
      `coding_rules.bulk_change.warning.${selectQPWarning.name}.${
        selectQPWarning.languageName
      }.${handler.allRulesName().length - 1}.1`
    )
  ).toBeInTheDocument();
});

it('should be able to bulk deactivate quality profile', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser());

  const selectQP = handler.allQualityProfile('java')[0];

  await user.click(await screen.findByRole('button', { name: 'bulk_change' }));
  await user.click(await screen.findByRole('link', { name: 'coding_rules.deactivate_in…' }));
  const dialogScreen = within(
    screen.getByRole('dialog', {
      name: 'coding_rules.deactivate_in_quality_profile (4 coding_rules._rules)'
    })
  );
  await user.click(dialogScreen.getByRole('textbox', { name: 'coding_rules.deactivate_in' }));

  await user.click(dialogScreen.getByText(`${selectQP.name} - ${selectQP.languageName}`));
  await user.click(dialogScreen.getByRole('button', { name: 'apply' }));
  expect(
    dialogScreen.getByText(
      `coding_rules.bulk_change.success.${selectQP.name}.${selectQP.languageName}.${
        handler.allRulesName().length
      }`
    )
  ).toBeInTheDocument();
});

function renderCodingRulesApp(currentUser?: CurrentUser, navigateTo?: string) {
  renderApp('coding_rules', routes, {
    navigateTo,
    currentUser,
    languages: {
      js: { key: 'js', name: 'JavaScript' },
      java: { key: 'java', name: 'Java' },
      c: { key: 'c', name: 'C' }
    }
  });
}
