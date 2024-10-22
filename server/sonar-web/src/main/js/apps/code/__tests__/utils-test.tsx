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
import { mockMainBranch, mockPullRequest } from '../../../helpers/mocks/branch-like';
import { getCodeMetrics, mostCommonPrefix } from '../utils';

describe('getCodeMetrics', () => {
  it('should return the right metrics for portfolios', () => {
    expect(getCodeMetrics(ComponentQualifier.Portfolio)).toMatchSnapshot();
    expect(
      getCodeMetrics(ComponentQualifier.Portfolio, undefined, { includeQGStatus: true }),
    ).toMatchSnapshot();
    expect(
      getCodeMetrics(ComponentQualifier.Portfolio, undefined, {
        includeQGStatus: true,
        newCode: true,
      }),
    ).toMatchSnapshot();
    expect(
      getCodeMetrics(ComponentQualifier.Portfolio, undefined, {
        includeQGStatus: true,
        newCode: false,
      }),
    ).toMatchSnapshot();
  });

  it('should return the right metrics for apps', () => {
    expect(getCodeMetrics(ComponentQualifier.Application)).toMatchSnapshot();
  });

  it('should return the right metrics for projects', () => {
    expect(getCodeMetrics(ComponentQualifier.Project, mockMainBranch())).toMatchSnapshot();
    expect(getCodeMetrics(ComponentQualifier.Project, mockPullRequest())).toMatchSnapshot();
  });
});

describe('#mostCommonPrefix', () => {
  it('should correctly find the common path prefix', () => {
    expect(mostCommonPrefix(['src/main/ts/tests', 'src/main/java/tests'])).toEqual('src/main/');
    expect(mostCommonPrefix(['src/main/ts/app', 'src/main/ts/app'])).toEqual('src/main/ts/');
    expect(mostCommonPrefix(['src/main/ts', 'lib/main/ts'])).toEqual('');
  });
});
