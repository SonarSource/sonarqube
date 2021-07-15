/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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

import handleRequiredAuthentication from '../handleRequiredAuthentication';
import {
  checkStatus,
  getJSON,
  getText,
  HttpStatus,
  isSuccessStatus,
  parseError,
  parseJSON,
  parseText,
  post,
  postJSON,
  postJSONBody,
  requestTryAndRepeatUntil,
} from '../request';

jest.mock('../handleRequiredAuthentication', () => ({ default: jest.fn() }));

jest.mock('../init', () => {
  const module = jest.requireActual('../init');
  return {
    ...module,
    getRequestOptions: jest.fn().mockReturnValue({}),
  };
});
const url = '/my-url';

beforeEach(() => {
  jest.clearAllMocks();
  window.fetch = jest.fn().mockResolvedValue(mockResponse({}, HttpStatus.Ok, {}));
});

describe('getJSON', () => {
  it('should get json without parameters', async () => {
    const response = mockResponse({}, HttpStatus.Ok, {});
    window.fetch = jest.fn().mockResolvedValue(response);
    getJSON(url);
    await new Promise(setImmediate);

    expect(window.fetch).toBeCalledWith(url, expect.objectContaining({ method: 'GET' }));
    expect(response.json).toBeCalled();
  });

  it('should get json with parameters', () => {
    getJSON(url, { data: 'test' });
    expect(window.fetch).toBeCalledWith(
      url + '?data=test',
      expect.objectContaining({ method: 'GET' })
    );
  });
});

describe('getText', () => {
  it('should get text without parameters', async () => {
    const response = mockResponse({}, HttpStatus.Ok, '');
    window.fetch = jest.fn().mockResolvedValue(response);
    getText(url);
    await new Promise(setImmediate);

    expect(window.fetch).toBeCalledWith(url, expect.objectContaining({ method: 'GET' }));
    expect(response.text).toBeCalled();
  });

  it('should get text with parameters', () => {
    getText(url, { data: 'test' });
    expect(window.fetch).toBeCalledWith(
      url + '?data=test',
      expect.objectContaining({ method: 'GET' })
    );
  });
});

describe('parseError', () => {
  it('should parse error and return the message', async () => {
    const response = new Response(JSON.stringify({ errors: [{ msg: 'Error1' }] }), {
      status: HttpStatus.BadRequest,
    });
    await expect(parseError(response)).resolves.toBe('Error1');
  });

  it('should parse error and return concatenated messages', async () => {
    const response = new Response(
      JSON.stringify({ errors: [{ msg: 'Error1' }, { msg: 'Error2' }] }),
      { status: HttpStatus.BadRequest }
    );
    await expect(parseError(response)).resolves.toBe('Error1. Error2');
  });

  it('should parse error and return default message', async () => {
    const response = new Response('{}', { status: HttpStatus.BadRequest });
    await expect(parseError(response)).resolves.toBe('default_error_message');
    const responseUndefined = new Response('', { status: HttpStatus.BadRequest });
    await expect(parseError(responseUndefined)).resolves.toBe('default_error_message');
  });
});

describe('parseJSON', () => {
  it('should return a json response', () => {
    const body = { test: 2 };
    const response = mockResponse({}, HttpStatus.Ok, body);
    const jsonResponse = parseJSON(response);
    expect(response.json).toBeCalled();
    return expect(jsonResponse).resolves.toEqual(body);
  });
});

describe('parseText', () => {
  it('should return a text response', () => {
    const body = 'test';
    const response = mockResponse({}, HttpStatus.Ok, body);
    const textResponse = parseText(response);
    expect(response.text).toBeCalled();
    return expect(textResponse).resolves.toBe(body);
  });
});

describe('postJSON', () => {
  it('should post without parameters and get json', async () => {
    const response = mockResponse();
    window.fetch = jest.fn().mockResolvedValue(response);
    postJSON(url);
    await new Promise(setImmediate);

    expect(window.fetch).toBeCalledWith(url, expect.objectContaining({ method: 'POST' }));
    expect(response.json).toBeCalled();
  });

  it('should post with a body and get json', () => {
    postJSON(url, { data: 'test' });
    expect(window.fetch).toBeCalledWith(
      url,
      expect.objectContaining({ body: 'data=test', method: 'POST' })
    );
  });
});

describe('postJSONBody', () => {
  it('should post without parameters and get json', async () => {
    const response = mockResponse();
    window.fetch = jest.fn().mockResolvedValue(response);
    postJSONBody(url);
    await new Promise(setImmediate);

    expect(window.fetch).toBeCalledWith(url, expect.objectContaining({ method: 'POST' }));
    expect(response.json).toBeCalled();
  });

  it('should post with a body and get json', () => {
    postJSONBody(url, { nested: { data: 'test', withArray: [1, 2] } });
    expect(window.fetch).toBeCalledWith(
      url,
      expect.objectContaining({
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: '{"nested":{"data":"test","withArray":[1,2]}}',
        method: 'POST',
      })
    );
  });
});

describe('post', () => {
  it('should post without parameters and return nothing', async () => {
    const response = mockResponse();
    window.fetch = jest.fn().mockResolvedValue(response);
    post(url, { data: 'test' });
    await new Promise(setImmediate);

    expect(window.fetch).toBeCalledWith(
      url,
      expect.objectContaining({ body: 'data=test', method: 'POST' })
    );
    expect(response.json).not.toBeCalled();
    expect(response.text).not.toBeCalled();
  });
});

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

    await expect(promiseResult).resolves.toEqual({ repeat: false });
  });

  it('should repeat call as long as there is an error', async () => {
    const apiCall = jest.fn().mockRejectedValue({ status: HttpStatus.GatewayTimeout });
    const stopRepeat = jest.fn().mockReturnValue(true);
    const promiseResult = requestTryAndRepeatUntil(
      apiCall,
      { max: -1, slowThreshold: -20 },
      stopRepeat,
      [HttpStatus.GatewayTimeout]
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

    await expect(promiseResult).resolves.toBe('Success');
  });

  it('should stop after 3 calls', async () => {
    const apiCall = jest.fn().mockResolvedValue({});
    const stopRepeat = jest.fn().mockReturnValue(false);
    const promiseResult = requestTryAndRepeatUntil(
      apiCall,
      { max: 3, slowThreshold: 0 },
      stopRepeat
    );

    for (let i = 1; i < 3; i++) {
      expect(apiCall).toBeCalledTimes(i);
      await new Promise(setImmediate);
      jest.runAllTimers();
    }
    expect(apiCall).toBeCalledTimes(3);
    await expect(promiseResult).rejects.toBeUndefined();

    // It should not call anymore after 3 times
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

describe('checkStatus', () => {
  it('should resolve with the response', async () => {
    const response = mockResponse();
    await expect(checkStatus(response)).resolves.toBe(response);
  });

  it('should reject with the response', async () => {
    const response = mockResponse({}, HttpStatus.InternalServerError);
    await expect(checkStatus(response)).rejects.toEqual(response);
  });

  it('should handle required authentication', async () => {
    await checkStatus(mockResponse({}, HttpStatus.Unauthorized)).catch(() => {
      expect(handleRequiredAuthentication).toBeCalled();
    });
  });

  it('should bybass the redirect with a 401 error', async () => {
    const mockedResponse = mockResponse({}, HttpStatus.Unauthorized);
    await checkStatus(mockedResponse, true).catch((response) => {
      expect(handleRequiredAuthentication).not.toBeCalled();
      expect(response).toEqual(mockedResponse);
    });
  });
});
it('should export status codes', () => {
  expect(HttpStatus.NotFound).toEqual(404);
});

describe('isSuccessStatus', () => {
  it('should work for a successful response status', () => {
    expect(isSuccessStatus(HttpStatus.Ok)).toBe(true);
    expect(isSuccessStatus(HttpStatus.Created)).toBe(true);
  });

  it('should work for an unsuccessful response status', () => {
    expect(isSuccessStatus(HttpStatus.MultipleChoices)).toBe(false);
    expect(isSuccessStatus(HttpStatus.NotFound)).toBe(false);
    expect(isSuccessStatus(HttpStatus.InternalServerError)).toBe(false);
  });
});

function mockResponse(headers: T.Dict<string> = {}, status = HttpStatus.Ok, value?: any): Response {
  const body = value && value instanceof Object ? JSON.stringify(value) : value;
  const response = new Response(body, { headers, status });
  response.json = jest.fn().mockResolvedValue(value);
  response.text = jest.fn().mockResolvedValue(value);
  return response;
}
