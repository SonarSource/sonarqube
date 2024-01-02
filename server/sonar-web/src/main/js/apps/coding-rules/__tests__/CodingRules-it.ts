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
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CodingRulesMock from '../../../api/mocks/CodingRulesMock';
import { mockCurrentUser, mockLoggedInUser } from '../../../helpers/testMocks';
import { renderAppRoutes } from '../../../helpers/testReactTestingUtils';
import { CurrentUser } from '../../../types/users';
import routes from '../routes';

jest.mock('../../../api/rules');
jest.mock('../../../api/issues');
jest.mock('../../../api/users');
jest.mock('../../../api/quality-profiles');

let handler: CodingRulesMock;

beforeAll(() => {
  window.scrollTo = jest.fn();
  window.HTMLElement.prototype.scrollIntoView = jest.fn();
  handler = new CodingRulesMock();
});

afterEach(() => handler.reset());

it('should select rules with keyboard navigation', async () => {
  const user = userEvent.setup();
  renderCodingRulesApp();
  let listitem = await screen.findByRole('listitem', { current: true });
  expect(within(listitem).getByRole('link', { name: 'Awsome java rule' })).toBeInTheDocument();
  await user.keyboard('{ArrowDown}');
  listitem = await screen.findByRole('listitem', { current: true });
  expect(within(listitem).getByRole('link', { name: 'Hot hotspot' })).toBeInTheDocument();
  await user.keyboard('{ArrowUp}');
  listitem = await screen.findByRole('listitem', { current: true });
  expect(within(listitem).getByRole('link', { name: 'Awsome java rule' })).toBeInTheDocument();
  await user.keyboard('{ArrowRight}');
  expect(screen.getByRole('heading', { level: 3, name: 'Awsome java rule' })).toBeInTheDocument();
  await user.keyboard('{ArrowLeft}');
  listitem = await screen.findByRole('listitem', { current: true });
  expect(within(listitem).getByRole('link', { name: 'Awsome java rule' })).toBeInTheDocument();
});

it('should open with permalink', async () => {
  renderCodingRulesApp(undefined, 'coding_rules?rule_key=rule1');
  expect(await screen.findByRole('link', { name: 'Awsome java rule' })).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'Hot hotspot' })).not.toBeInTheDocument();
});

it('should show open rule with default description section', async () => {
  renderCodingRulesApp(undefined, 'coding_rules?open=rule1');
  expect(
    await screen.findByRole('heading', { level: 3, name: 'Awsome java rule' })
  ).toBeInTheDocument();
  expect(screen.getByText('Why')).toBeInTheDocument();
  expect(screen.getByText('Because')).toBeInTheDocument();
});

it('should show open rule with no description', async () => {
  renderCodingRulesApp(undefined, 'coding_rules?open=rule6');
  expect(
    await screen.findByRole('heading', { level: 3, name: 'Bad Python rule' })
  ).toBeInTheDocument();
  expect(screen.getByText('issue.external_issue_description.Bad Python rule')).toBeInTheDocument();
});

it('should show hotspot rule section', async () => {
  const user = userEvent.setup();
  renderCodingRulesApp(undefined, 'coding_rules?open=rule2');
  expect(await screen.findByRole('heading', { level: 3, name: 'Hot hotspot' })).toBeInTheDocument();
  expect(screen.getByText('Introduction to this rule')).toBeInTheDocument();
  expect(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.root_cause.SECURITY_HOTSPOT',
    })
  ).toBeInTheDocument();
  expect(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.assess_the_problem',
    })
  ).toBeInTheDocument();
  expect(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  ).toBeInTheDocument();
  // Check that we render plain html
  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  );
  expect(screen.getByRole('link', { name: 'Awsome Reading' })).toBeInTheDocument();
});

it('should show rule advanced section', async () => {
  const user = userEvent.setup();
  renderCodingRulesApp(undefined, 'coding_rules?open=rule5');
  expect(
    await screen.findByRole('heading', { level: 3, name: 'Awsome Python rule' })
  ).toBeInTheDocument();
  expect(screen.getByText('Introduction to this rule')).toBeInTheDocument();
  expect(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.how_to_fix',
    })
  ).toBeInTheDocument();
  expect(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  ).toBeInTheDocument();
  // Check that we render plain html
  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  );
  expect(screen.getByRole('link', { name: 'Awsome Reading' })).toBeInTheDocument();
});

it('should show rule advanced section with context', async () => {
  const user = userEvent.setup();
  renderCodingRulesApp(undefined, 'coding_rules?open=rule7');
  expect(
    await screen.findByRole('heading', { level: 3, name: 'Python rule with context' })
  ).toBeInTheDocument();
  expect(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.how_to_fix',
    })
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.how_to_fix',
    })
  );
  expect(screen.getByRole('button', { name: 'Spring' })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Spring boot' })).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'coding_rules.description_context.other' })
  ).toBeInTheDocument();
  expect(screen.getByText('coding_rules.description_context.sub_title.Spring')).toBeInTheDocument();
  expect(screen.getByText('This is how to fix for spring')).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'Spring boot' }));
  expect(
    screen.getByText('coding_rules.description_context.sub_title.Spring boot')
  ).toBeInTheDocument();
  expect(screen.getByText('This is how to fix for spring boot')).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'coding_rules.description_context.other' }));
  expect(
    screen.queryByText(
      'coding_rules.description_context.sub_title.coding_rules.description_context.other'
    )
  ).not.toBeInTheDocument();
  expect(screen.getByText('coding_rules.context.others.title')).toBeInTheDocument();
  expect(screen.getByText('coding_rules.context.others.description.first')).toBeInTheDocument();

  const productBoardLink = screen.getByRole('link', {
    name: 'opens_in_new_window coding_rules.context.others.feedback_description.link',
  });
  expect(productBoardLink).toBeInTheDocument();
  expect(productBoardLink).toHaveAttribute('target', '_blank');
});

it('should be able to extend the rule description', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin();
  renderCodingRulesApp(undefined, 'coding_rules?open=rule5');
  expect(
    await screen.findByRole('heading', { level: 3, name: 'Awsome Python rule' })
  ).toBeInTheDocument();

  // Add
  await user.click(screen.getByRole('button', { name: 'coding_rules.extend_description' }));
  expect(screen.getByRole('textbox')).toBeInTheDocument();
  await user.click(screen.getByRole('textbox'));
  await user.keyboard('TEST DESC');
  await user.click(screen.getByRole('button', { name: 'save' }));
  expect(await screen.findByText('TEST DESC')).toBeInTheDocument();

  // Edit
  await user.click(screen.getByRole('button', { name: 'coding_rules.extend_description' }));
  await user.click(screen.getByRole('textbox'));
  await user.keyboard('{Control>}A{/Control}NEW DESC');
  await user.click(screen.getByRole('button', { name: 'save' }));
  expect(await screen.findByText('NEW DESC')).toBeInTheDocument();

  //Cancel
  await user.click(screen.getByRole('button', { name: 'coding_rules.extend_description' }));
  await user.dblClick(screen.getByRole('textbox'));
  await user.keyboard('DIFFERENCE');
  await user.click(screen.getByRole('button', { name: 'cancel' }));
  expect(await screen.findByText('NEW DESC')).toBeInTheDocument();

  //Remove
  await user.click(screen.getByRole('button', { name: 'coding_rules.extend_description' }));
  await user.click(screen.getByRole('button', { name: 'remove' }));
  await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'remove' }));
  await waitFor(() => expect(screen.queryByText('NEW DESC')).not.toBeInTheDocument());
});

it('should list all rules', async () => {
  renderCodingRulesApp();

  await waitFor(() => {
    handler
      .allRulesName()
      .forEach((name) => expect(screen.getByRole('link', { name })).toBeInTheDocument());
  });
});

it('should have all type facet', async () => {
  renderCodingRulesApp();

  await waitFor(() => {
    [
      'issue.type.BUG',
      'issue.type.VULNERABILITY',
      'issue.type.CODE_SMELL',
      'issue.type.SECURITY_HOTSPOT',
    ].forEach((name) => expect(screen.getByRole('checkbox', { name })).toBeInTheDocument());
  });
});

it('select the correct quality profile for bulk change base on language search', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser());
  const selectQP = handler.allQualityProfile('js')[0];

  await user.click(await screen.findByRole('checkbox', { name: 'JavaScript' }));
  await user.click(await screen.findByRole('button', { name: 'bulk_change' }));
  await user.click(await screen.findByRole('link', { name: 'coding_rules.activate_in…' }));
  const dialog = screen.getByRole('dialog', {
    name: 'coding_rules.activate_in_quality_profile (2 coding_rules._rules)',
  });

  expect(dialog).toBeInTheDocument();
  const dialogScreen = within(dialog);
  expect(dialogScreen.getByText(`${selectQP.name} - ${selectQP.languageName}`)).toBeInTheDocument();
});

it('no quality profile for bulk cahnge base on language search', async () => {
  const user = userEvent.setup();
  handler.setIsAdmin();
  renderCodingRulesApp(mockLoggedInUser());

  await user.click(await screen.findByRole('checkbox', { name: 'C' }));
  await user.click(await screen.findByRole('button', { name: 'bulk_change' }));
  await user.click(await screen.findByRole('link', { name: 'coding_rules.activate_in…' }));
  const dialog = screen.getByRole('dialog', {
    name: 'coding_rules.activate_in_quality_profile (1 coding_rules._rules)',
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
    name: `coding_rules.activate_in_quality_profile (${handler.allRulesCount()} coding_rules._rules)`,
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
      name: `coding_rules.activate_in_quality_profile (${handler.allRulesCount()} coding_rules._rules)`,
    })
  );
  await user.click(dialogScreen.getByRole('textbox', { name: 'coding_rules.activate_in' }));
  await user.click(
    dialogScreen.getByText(`${selectQPWarning.name} - ${selectQPWarning.languageName}`)
  );
  await user.click(dialogScreen.getByRole('button', { name: 'apply' }));
  expect(
    dialogScreen.getByText(
      `coding_rules.bulk_change.warning.${selectQPWarning.name}.${selectQPWarning.languageName}.${
        handler.allRulesName().length - 1
      }.1`
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
      name: `coding_rules.deactivate_in_quality_profile (${handler.allRulesCount()} coding_rules._rules)`,
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

it('should handle hash parameters', async () => {
  renderCodingRulesApp(mockLoggedInUser(), 'coding_rules#languages=c,js|types=BUG');

  // 2 languages
  expect(await screen.findByText('x_selected.2')).toBeInTheDocument();
  expect(screen.getAllByTitle('issue.type.BUG')).toHaveLength(2);
  // Only 3 rules shown
  expect(screen.getByText('x_of_y_shown.3.3')).toBeInTheDocument();
});

it('should show notification for rule advanced section and remove it after user visits', async () => {
  const user = userEvent.setup();
  renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule8');
  await screen.findByRole('heading', {
    level: 3,
    name: 'Awesome Python rule with education principles',
  });
  expect(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  );

  expect(screen.getByText('coding_rules.more_info.notification_message')).toBeInTheDocument();
  expect(
    screen.getByRole('button', {
      name: 'coding_rules.more_info.scroll_message',
    })
  ).toBeInTheDocument();
  await user.click(
    screen.getByRole('button', {
      name: 'coding_rules.more_info.scroll_message',
    })
  );
  // navigate away and come back
  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.how_to_fix',
    })
  );
  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  );
  expect(screen.queryByText('coding_rules.more_info.notification_message')).not.toBeInTheDocument();
});

it('should show notification for rule advanced section and removes it when user scroll to the principles', async () => {
  const user = userEvent.setup();
  renderCodingRulesApp(mockLoggedInUser(), 'coding_rules?open=rule8');

  await screen.findByRole('heading', {
    level: 3,
    name: 'Awesome Python rule with education principles',
  });
  expect(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  ).toBeInTheDocument();

  // navigate away and come back
  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.how_to_fix',
    })
  );
  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  );

  expect(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  ).toBeInTheDocument();

  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  );

  expect(screen.getByText('coding_rules.more_info.notification_message')).toBeInTheDocument();
  expect(
    screen.getByRole('button', {
      name: 'coding_rules.more_info.scroll_message',
    })
  ).toBeInTheDocument();

  fireEvent.scroll(screen.getByText('coding_rules.more_info.education_principles.title'));

  // navigate away and come back
  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.how_to_fix',
    })
  );
  await user.click(
    screen.getByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  );
  expect(screen.queryByText('coding_rules.more_info.notification_message')).not.toBeInTheDocument();
});

it('should not show notification for anonymous users', async () => {
  const user = userEvent.setup();
  renderCodingRulesApp(mockCurrentUser(), 'coding_rules?open=rule8');

  await user.click(
    await screen.findByRole('tab', {
      name: 'coding_rules.description_section.title.more_info',
    })
  );

  expect(screen.queryByText('coding_rules.more_info.notification_message')).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', {
      name: 'coding_rules.more_info.scroll_message',
    })
  ).not.toBeInTheDocument();
});

function renderCodingRulesApp(currentUser?: CurrentUser, navigateTo?: string) {
  renderAppRoutes('coding_rules', routes, {
    navigateTo,
    currentUser,
    languages: {
      js: { key: 'js', name: 'JavaScript' },
      java: { key: 'java', name: 'Java' },
      c: { key: 'c', name: 'C' },
    },
  });
}
