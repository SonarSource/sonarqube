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
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { parseIssueFromResponse } from '../../../helpers/issues';
import { mockComponent } from '../../../helpers/mocks/component';
import { mockIssue } from '../../../helpers/testMocks';
import { updateIssue } from '../actions';

jest.mock('../../../app/utils/throwGlobalError', () => jest.fn());

jest.mock('../../../helpers/issues', () => ({
  parseIssueFromResponse: jest.fn()
}));

describe('updateIssue', () => {
  const onChange = jest.fn();
  const oldIssue = mockIssue(false, { key: 'old' });
  const newIssue = mockIssue(false, { key: 'new' });
  const parsedIssue = mockIssue(false, { key: 'parsed' });
  const successPromise = jest.fn().mockResolvedValue({
    issue: mockIssue(),
    components: [mockComponent()]
  });
  const errorPromise = jest.fn().mockRejectedValue(null);
  (parseIssueFromResponse as jest.Mock).mockReturnValue(parsedIssue);

  beforeEach(jest.clearAllMocks);

  it('makes successful optimistic updates', async () => {
    updateIssue(onChange, successPromise(), oldIssue, newIssue);
    expect(onChange).toBeCalledWith(newIssue);

    await new Promise(setImmediate);

    expect(onChange).toBeCalledTimes(1);
  });

  it('makes successful non-optimistic updates', async () => {
    updateIssue(onChange, successPromise());
    expect(onChange).not.toBeCalled();

    await new Promise(setImmediate);
    expect(onChange).toBeCalledWith(parsedIssue);
    expect(onChange).toBeCalledTimes(1);
  });

  it('makes unsuccessful optimistic updates', async () => {
    updateIssue(onChange, errorPromise(), oldIssue, newIssue);
    expect(onChange).toBeCalledWith(newIssue);

    await new Promise(setImmediate);

    expect(onChange).toBeCalledWith(oldIssue);
    expect(onChange).toBeCalledTimes(2);
  });

  it('makes unsuccessful non-optimistic updates', async () => {
    updateIssue(onChange, errorPromise());
    expect(onChange).not.toBeCalled();

    await new Promise(setImmediate);
    expect(parseIssueFromResponse).not.toBeCalled();
    expect(throwGlobalError).toBeCalled();
  });
});
