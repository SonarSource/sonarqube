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

import '@testing-library/jest-dom';
import { configure, screen, waitFor } from '@testing-library/react';
import userEvent, { PointerEventsCheckLevel } from '@testing-library/user-event';

configure({
  asyncUtilTimeout: 6000,
});

expect.extend({
  async toHaveATooltipWithContent(received: any, content: string) {
    const user = userEvent.setup({ pointerEventsCheck: PointerEventsCheckLevel.Never });

    if (!(received instanceof Element)) {
      return {
        pass: false,
        message: () => `Received object is not an HTMLElement, and cannot have a tooltip`,
      };
    }

    await user.hover(received);

    const tooltip = await screen.findByRole('tooltip');

    const result = tooltip.textContent?.includes(content)
      ? {
          pass: true,
          message: () => `Tooltip content "${tooltip.textContent}" contains expected "${content}"`,
        }
      : {
          pass: false,
          message: () =>
            `Tooltip content "${tooltip.textContent}" does not contain expected "${content}"`,
        };

    await user.keyboard('{Escape}');
    await user.unhover(received);

    await waitFor(() => {
      expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    return result;
  },
  async toHaveAPopoverWithContent(received: any, content: string) {
    const user = userEvent.setup({ pointerEventsCheck: PointerEventsCheckLevel.Never });

    if (!(received instanceof Element)) {
      return {
        pass: false,
        message: () => `Received object is not an HTMLElement, and cannot have a tooltip`,
      };
    }

    await user.click(received);

    const popover = await screen.findByRole('dialog');

    const result = popover.textContent?.includes(content)
      ? {
          pass: true,
          message: () => `Tooltip content "${popover.textContent}" contains expected "${content}"`,
        }
      : {
          pass: false,
          message: () =>
            `Tooltip content "${popover.textContent}" does not contain expected "${content}"`,
        };

    await user.keyboard('{Escape}');

    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    return result;
  },
});
