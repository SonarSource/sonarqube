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

import { ComponentQualifier } from '~sonar-aligned/types/component';
import { mockBranch } from '../../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { getCurrentPage } from '../utils';

describe('getCurrentPage', () => {
  it('should return a portfolio page', () => {
    expect(
      getCurrentPage(
        mockComponent({ key: 'foo', qualifier: ComponentQualifier.Portfolio }),
        undefined,
      ),
    ).toEqual({
      type: 'PORTFOLIO',
      component: 'foo',
    });
  });

  it('should return a portfolio page for a subportfolio too', () => {
    expect(
      getCurrentPage(
        mockComponent({ key: 'foo', qualifier: ComponentQualifier.SubPortfolio }),
        undefined,
      ),
    ).toEqual({
      type: 'PORTFOLIO',
      component: 'foo',
    });
  });

  it('should return an application page', () => {
    expect(
      getCurrentPage(
        mockComponent({ key: 'foo', qualifier: ComponentQualifier.Application }),
        mockBranch({ name: 'develop' }),
      ),
    ).toEqual({ type: 'APPLICATION', component: 'foo', branch: 'develop' });
  });

  it('should return a project page', () => {
    expect(getCurrentPage(mockComponent(), mockBranch({ name: 'feature/foo' }))).toEqual({
      type: 'PROJECT',
      component: 'my-project',
      branch: 'feature/foo',
    });
  });
});
