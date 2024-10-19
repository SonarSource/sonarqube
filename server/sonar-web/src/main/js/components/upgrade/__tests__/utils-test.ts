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
import { SystemUpgrade } from '../../../types/system';
import * as u from '../utils';

describe('sortUpgrades', () => {
  it('should sort correctly versions', () => {
    expect(
      u.sortUpgrades([
        { version: '5.4.2' },
        { version: '5.10' },
        { version: '5.1' },
        { version: '5.4' },
      ] as SystemUpgrade[]),
    ).toEqual([{ version: '5.10' }, { version: '5.4.2' }, { version: '5.4' }, { version: '5.1' }]);
    expect(
      u.sortUpgrades([
        { version: '5.10' },
        { version: '5.1.2' },
        { version: '6.0' },
        { version: '6.9' },
      ] as SystemUpgrade[]),
    ).toEqual([{ version: '6.9' }, { version: '6.0' }, { version: '5.10' }, { version: '5.1.2' }]);
  });
});

describe('groupUpgrades', () => {
  it('should group correctly', () => {
    expect(
      u.groupUpgrades([
        { version: '5.10' },
        { version: '5.4.2' },
        { version: '5.4' },
        { version: '5.1' },
      ] as SystemUpgrade[]),
    ).toEqual([
      [{ version: '5.10' }, { version: '5.4.2' }, { version: '5.4' }, { version: '5.1' }],
    ]);
    expect(
      u.groupUpgrades([
        { version: '6.9' },
        { version: '6.7' },
        { version: '6.0' },
        { version: '5.10' },
        { version: '5.4.2' },
      ] as SystemUpgrade[]),
    ).toEqual([
      [{ version: '6.9' }, { version: '6.7' }, { version: '6.0' }],
      [{ version: '5.10' }, { version: '5.4.2' }],
    ]);
  });
});
