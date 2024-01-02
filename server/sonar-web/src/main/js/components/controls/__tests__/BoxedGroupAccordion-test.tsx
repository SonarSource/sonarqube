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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import BoxedGroupAccordion from '../BoxedGroupAccordion';

it('should behave correctly', async () => {
  const user = userEvent.setup();
  renderDeliveryAccordion();
  expect(screen.queryByText('children')).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { expanded: false }));
  expect(screen.getByText('children')).toBeInTheDocument();
});

it('should render header correctly', () => {
  renderDeliveryAccordion(() => <div>header</div>);
  expect(screen.getByText('header')).toBeInTheDocument();
});

function renderDeliveryAccordion(renderHeader?: () => React.ReactNode) {
  function AccordionTest() {
    const [open, setOpen] = React.useState(false);

    return (
      <BoxedGroupAccordion
        onClick={() => setOpen(!open)}
        open={open}
        title="test"
        renderHeader={renderHeader}
      >
        <div>children</div>
      </BoxedGroupAccordion>
    );
  }

  return renderComponent(<AccordionTest />);
}
