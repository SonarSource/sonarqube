/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import { requestTryAndRepeatUntil } from '../request';

describe('requestTryAndRepeatUntil', () => {
  jest.useFakeTimers();

  beforeEach(() => {
    jest.clearAllTimers();
  });

  it('should repeat call until stop condition is met', async () => {
    const apiCall = jest.fn().mockResolvedValue({ repeat: true });
    const stopRepeat = jest.fn().mockImplementation(({ repeat }) => !repeat);

    const promiseResult = requestTryAndRepeatUntil(
      apiCall,
      { max: -1, slowThreshold: -20 },
      stopRepeat
    );

    for (let i = 1; i < 5; i++) {
      jest.runAllTimers();
      expect(apiCall).toBeCalledTimes(i);
      await new Promise(setImmediate);
      expect(stopRepeat).toBeCalledTimes(i);
    }
    apiCall.mockResolvedValue({ repeat: false });
    jest.runAllTimers();
    expect(apiCall).toBeCalledTimes(5);
    await new Promise(setImmediate);
    expect(stopRepeat).toBeCalledTimes(5);

    return expect(promiseResult).resolves.toEqual({ repeat: false });
  });

  it('should repeat call as long as there is an error', async () => {
    const apiCall = jest.fn().mockRejectedValue({ response: { status: 504 } });
    const stopRepeat = jest.fn().mockReturnValue(true);
    const promiseResult = requestTryAndRepeatUntil(
      apiCall,
      { max: -1, slowThreshold: -20 },
      stopRepeat
    );

    for (let i = 1; i < 5; i++) {
      jest.runAllTimers();
      expect(apiCall).toBeCalledTimes(i);
      await new Promise(setImmediate);
    }
    apiCall.mockResolvedValue('Success');
    jest.runAllTimers();
    expect(apiCall).toBeCalledTimes(5);
    await new Promise(setImmediate);
    expect(stopRepeat).toBeCalledTimes(1);

    return expect(promiseResult).resolves.toBe('Success');
  });

  it('should stop after 3 calls', async () => {
    const apiCall = jest.fn().mockResolvedValue({});
    const stopRepeat = jest.fn().mockReturnValue(false);
    const promiseResult = requestTryAndRepeatUntil(
      apiCall,
      { max: 3, slowThreshold: 0 },
      stopRepeat
    );

    expect(promiseResult).rejects.toBe(undefined);

    for (let i = 1; i < 3; i++) {
      jest.runAllTimers();
      expect(apiCall).toBeCalledTimes(i);
      await new Promise(setImmediate);
    }
    apiCall.mockResolvedValue('Success');
    jest.runAllTimers();
    expect(apiCall).toBeCalledTimes(3);
  });

  it('should slow down after 2 calls', async () => {
    const apiCall = jest.fn().mockResolvedValue({});
    const stopRepeat = jest.fn().mockReturnValue(false);
    requestTryAndRepeatUntil(apiCall, { max: 5, slowThreshold: 3 }, stopRepeat);

    for (let i = 1; i < 3; i++) {
      jest.advanceTimersByTime(500);
      expect(apiCall).toBeCalledTimes(i);
      await new Promise(setImmediate);
    }

    jest.advanceTimersByTime(500);
    expect(apiCall).toBeCalledTimes(2);
    jest.advanceTimersByTime(2000);
    expect(apiCall).toBeCalledTimes(2);
    jest.advanceTimersByTime(500);
    expect(apiCall).toBeCalledTimes(3);
    await new Promise(setImmediate);

    jest.advanceTimersByTime(3000);
    expect(apiCall).toBeCalledTimes(4);
  });
});
