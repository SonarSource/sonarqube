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
import { parseIssueFromResponse } from '../../../helpers/issues';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockIssue } from '../../../helpers/testMocks';
import { throwGlobalError } from '../../../sonar-aligned/helpers/error';
import { updateIssue } from '../actions';

jest.mock('../../../sonar-aligned/helpers/error', () => ({ throwGlobalError: jest.fn() }));

jest.mock('../../../helpers/issues', () => ({
  parseIssueFromResponse: jest.fn(),
}));

describe('updateIssue', () => {
  const onChange = jest.fn();
  const oldIssue = mockIssue(false, { key: 'old' });
  const newIssue = mockIssue(false, { key: 'new' });
  const parsedIssue = mockIssue(false, { key: 'parsed' });
  const successPromise = jest.fn().mockResolvedValue({
    issue: mockIssue(),
    components: [mockComponent()],
  });
  const errorPromise = jest.fn().mockRejectedValue(null);
  (parseIssueFromResponse as jest.Mock).mockReturnValue(parsedIssue);

  beforeEach(jest.clearAllMocks);

  it('makes successful optimistic updates', async () => {
    updateIssue(onChange, successPromise(), oldIssue, newIssue);
    expect(onChange).toHaveBeenCalledWith(newIssue);

    await new Promise(setImmediate);

    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it('makes successful non-optimistic updates', async () => {
    updateIssue(onChange, successPromise());
    expect(onChange).not.toHaveBeenCalled();

    await new Promise(setImmediate);
    expect(onChange).toHaveBeenCalledWith(parsedIssue);
    expect(onChange).toHaveBeenCalledTimes(1);
  });

  it('makes unsuccessful optimistic updates', async () => {
    updateIssue(onChange, errorPromise(), oldIssue, newIssue);
    expect(onChange).toHaveBeenCalledWith(newIssue);

    await new Promise(setImmediate);

    expect(onChange).toHaveBeenCalledWith(oldIssue);
    expect(onChange).toHaveBeenCalledTimes(2);
  });

  it('makes unsuccessful non-optimistic updates', async () => {
    updateIssue(onChange, errorPromise());
    expect(onChange).not.toHaveBeenCalled();

    await new Promise(setImmediate);
    expect(parseIssueFromResponse).not.toHaveBeenCalled();
    expect(throwGlobalError).toHaveBeenCalled();
  });
});
