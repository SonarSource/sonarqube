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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { byRole, byTestId } from 'testing-library-selector';

import { renderComponent } from '../../../helpers/testReactTestingUtils';
import DocumentationTooltip, { DocumentationTooltipProps } from '../DocumentationTooltip';
import Link from '../Link';

const ui = {
  body: byRole('body'),
  beforeLink: byRole('link', { name: 'Interactive element before' }),
  helpIcon: byTestId('help-tooltip-activator'),
  linkInTooltip: byRole('link', { name: 'Label' }),
  linkInTooltip2: byRole('link', { name: 'opens_in_new_window Label2' }),
  afterLink: byRole('link', { name: 'Interactive element after' }),
};

it('should correctly navigate through TAB', async () => {
  const user = userEvent.setup();
  renderDocumentationTooltip();

  await user.tab();
  expect(await ui.beforeLink.find()).toHaveFocus();
  await user.tab();
  expect(ui.helpIcon.get()).toHaveFocus();
  await user.tab();
  expect(ui.linkInTooltip.get()).toHaveFocus();
  await user.tab();
  expect(ui.linkInTooltip2.get()).toHaveFocus();
  // Looks like RTL tab event ignores any custom focuses during the events phase,
  // unless preventDefault is specified
  await user.tab();
  await user.tab({ shift: true });
  expect(await ui.afterLink.find()).toHaveFocus();
  await user.tab({ shift: true });
  await user.tab();
  await user.tab();
  await user.tab({ shift: true });
  expect(await ui.afterLink.find()).toHaveFocus();
});

function renderDocumentationTooltip(props: Partial<DocumentationTooltipProps> = {}) {
  return renderComponent(
    <>
      <Link to="/" target="_blank">
        Interactive element before
      </Link>
      <DocumentationTooltip
        title="Tooltip title"
        content="Tooltip content"
        links={[
          {
            href: '/user-guide/clean-as-you-code/',
            label: 'Label',
            doc: false,
          },
          {
            href: '/user-guide/clean-as-you-code2/',
            label: 'Label2',
            doc: true,
            inPlace: true,
          },
        ]}
        {...props}
      />
      <Link to="/" target="_blank">
        Interactive element after
      </Link>
    </>
  );
}
