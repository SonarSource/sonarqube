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
import { stringify } from 'querystring';
import { omitBy, isNil } from 'lodash';
import { getCookie } from './cookies';
import { translate } from './l10n';

/** Current application version. Can be changed if a newer version is deployed. */
let currentApplicationVersion: string | undefined;

export function getCSRFTokenName(): string {
  return 'X-XSRF-TOKEN';
}

export function getCSRFTokenValue(): string {
  const cookieName = 'XSRF-TOKEN';
  const cookieValue = getCookie(cookieName);
  if (!cookieValue) {
    return '';
  }
  return cookieValue;
}

/**
 * Return an object containing a special http request header used to prevent CSRF attacks.
 */
export function getCSRFToken(): T.Dict<string> {
  // Fetch API in Edge doesn't work with empty header,
  // so we ensure non-empty value
  const value = getCSRFTokenValue();
  return value ? { [getCSRFTokenName()]: value } : {};
}

export type RequestData = T.Dict<any>;

export function omitNil(obj: RequestData): RequestData {
  return omitBy(obj, isNil);
}

/**
 * Default options for any request
 */
const DEFAULT_OPTIONS: {
  credentials: RequestCredentials;
  method: string;
} = {
  credentials: 'same-origin',
  method: 'GET'
};

/**
 * Default request headers
 */
const DEFAULT_HEADERS = {
  Accept: 'application/json'
};

/**
 * Request
 */
class Request {
  private data?: RequestData;

  constructor(private url: string, private options: { method?: string } = {}) {}

  getSubmitData(customHeaders: any = {}): { url: string; options: RequestInit } {
    let { url } = this;
    const options: RequestInit = { ...DEFAULT_OPTIONS, ...this.options };

    if (this.data) {
      if (this.data instanceof FormData) {
        options.body = this.data;
      } else {
        const strData = stringify(omitNil(this.data));
        if (options.method === 'GET') {
          url += '?' + strData;
        } else {
          customHeaders['Content-Type'] = 'application/x-www-form-urlencoded';
          options.body = strData;
        }
      }
    }

    options.headers = {
      ...DEFAULT_HEADERS,
      ...customHeaders
    };
    return { url, options };
  }

  submit(): Promise<Response> {
    const { url, options } = this.getSubmitData({ ...getCSRFToken() });
    return window.fetch((window as any).baseUrl + url, options);
  }

  setMethod(method: string): Request {
    this.options.method = method;
    return this;
  }

  setData(data?: RequestData): Request {
    if (data) {
      this.data = data;
    }
    return this;
  }
}

/**
 * Make a request
 */
export function request(url: string): Request {
  return new Request(url);
}

/**
 * Make a cors request
 */
export function corsRequest(url: string, mode: RequestMode = 'cors'): Request {
  const options: RequestInit = { mode };
  const request = new Request(url, options);
  request.submit = function() {
    const { url, options } = this.getSubmitData();
    return window.fetch(url, options);
  };
  return request;
}

function checkApplicationVersion(response: Response): boolean {
  const version = response.headers.get('Sonar-Version');
  if (version) {
    if (currentApplicationVersion && currentApplicationVersion !== version) {
      window.location.reload();
      return false;
    } else {
      currentApplicationVersion = version;
    }
  }
  return true;
}

/**
 * Check that response status is ok
 */
export function checkStatus(response: Response): Promise<Response> {
  return new Promise((resolve, reject) => {
    if (checkApplicationVersion(response)) {
      if (response.status === 401) {
        import('../app/utils/handleRequiredAuthentication')
          .then(i => i.default())
          .then(reject, reject);
      } else if (response.status >= 200 && response.status < 300) {
        resolve(response);
      } else {
        reject({ response });
      }
    }
  });
}

/**
 * Parse response as JSON
 */
export function parseJSON(response: Response): Promise<any> {
  return response.json();
}

/**
 * Parse response of failed request
 */
export function parseError(error: { response: Response }): Promise<string> {
  const DEFAULT_MESSAGE = translate('default_error_message');

  try {
    return error.response
      .json()
      .then(r => r.errors.map((error: any) => error.msg).join('. '))
      .catch(() => DEFAULT_MESSAGE);
  } catch (ex) {
    return Promise.resolve(DEFAULT_MESSAGE);
  }
}

/**
 * Shortcut to do a GET request and return response json
 */
export function getJSON(url: string, data?: RequestData): Promise<any> {
  return request(url)
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

/**
 * Shortcut to do a CORS GET request and return responsejson
 */
export function getCorsJSON(url: string, data?: RequestData): Promise<any> {
  return corsRequest(url)
    .setData(data)
    .submit()
    .then(response => {
      if (response.status >= 200 && response.status < 300) {
        return Promise.resolve(response);
      } else {
        return Promise.reject({ response });
      }
    })
    .then(parseJSON);
}

/**
 * Shortcut to do a POST request and return response json
 */
export function postJSON(url: string, data?: RequestData): Promise<any> {
  return request(url)
    .setMethod('POST')
    .setData(data)
    .submit()
    .then(checkStatus)
    .then(parseJSON);
}

/**
 * Shortcut to do a POST request
 */
export function post(url: string, data?: RequestData): Promise<void> {
  return new Promise((resolve, reject) => {
    request(url)
      .setMethod('POST')
      .setData(data)
      .submit()
      .then(checkStatus)
      .then(() => {
        resolve();
      }, reject);
  });
}

/**
 * Shortcut to do a DELETE request and return response json
 */
export function requestDelete(url: string, data?: RequestData): Promise<any> {
  return request(url)
    .setMethod('DELETE')
    .setData(data)
    .submit()
    .then(checkStatus);
}

/**
 * Delay promise for testing purposes
 */
export function delay(response: any): Promise<any> {
  return new Promise(resolve => setTimeout(() => resolve(response), 1200));
}

export function requestTryAndRepeat<T>(
  repeatAPICall: () => Promise<T>,
  tries: number,
  slowTriesThreshold: number,
  repeatErrors = [404]
) {
  return repeatAPICall().catch((error: { response: Response }) => {
    if (repeatErrors.includes(error.response.status)) {
      tries--;
      if (tries > 0) {
        return new Promise(resolve => {
          setTimeout(
            () =>
              resolve(requestTryAndRepeat(repeatAPICall, tries, slowTriesThreshold, repeatErrors)),
            tries > slowTriesThreshold ? 500 : 3000
          );
        });
      }
      return Promise.reject();
    }
    return Promise.reject(error);
  });
}
