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
import { byRole, byTestId } from '../../../helpers/testSelector';

import Link from '../../../../components/common/Link';
import { DocLink } from '../../../../helpers/doc-links';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import DocHelpTooltip, { DocHelpTooltipProps } from '../DocHelpTooltip';

const ui = {
  body: byRole('body'),
  beforeLink: byRole('link', { name: 'Interactive element before' }),
  helpIcon: byTestId('help-tooltip-activator'),
  helpLink: byRole('link', { name: 'Icon' }),
  linkInTooltip: byRole('link', { name: 'Label' }),
  linkInTooltip2: byRole('link', { name: /^Label2\b/ }),
  afterLink: byRole('link', { name: 'Interactive element after' }),
};

it('should correctly navigate through TAB', async () => {
  const user = userEvent.setup();
  renderDocHelpTooltip();

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
  expect(ui.helpIcon.get()).toHaveFocus();
  await user.tab({ shift: true });
  await user.tab({ shift: true });
  await user.tab();
  await user.tab();
  await user.tab({ shift: true });
  expect(await ui.beforeLink.find()).toHaveFocus();
});

function renderDocHelpTooltip(props: Partial<DocHelpTooltipProps> = {}) {
  return renderComponent(
    <>
      <Link to="/" target="_blank">
        Interactive element before
      </Link>
      <DocHelpTooltip
        title="Tooltip title"
        content="Tooltip content"
        links={[
          {
            doc: false,
            href: '/user-guide/clean-as-you-code2/',
            label: 'Label',
          },
          {
            doc: true,
            href: DocLink.CaYC,
            inPlace: true,
            label: 'Label2',
          },
        ]}
        {...props}
      >
        <Link to="/" target="_blank">
          Icon
        </Link>
      </DocHelpTooltip>
      <Link to="/" target="_blank">
        Interactive element after
      </Link>
    </>,
  );
}
