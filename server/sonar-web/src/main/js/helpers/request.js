/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import { stringify } from 'querystring';
import { getCookie } from './cookies';

type Response = {
  json: () => Promise<Object>,
  status: number
};

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
 * @returns {Object}
 */
export function getCSRFToken(): Object {
  // Fetch API in Edge doesn't work with empty header,
  // so we ensure non-empty value
  const value = getCSRFTokenValue();
  return value ? { [getCSRFTokenName()]: value } : {};
}

/**
 * Default options for any request
 */
const DEFAULT_OPTIONS: {
  credentials: string,
  method: string
} = {
  method: 'GET',
  credentials: 'same-origin'
};

/**
 * Default request headers
 */
const DEFAULT_HEADERS: {
  'Accept': string
} = {
  Accept: 'application/json'
};

/**
 * Request
 */
class Request {
  url: string;
  options: {
    method?: string
  };
  headers: Object;
  data: ?Object;

  constructor(url: string): void {
    this.url = url;
    this.options = {};
    this.headers = {};
  }

  submit() {
    let url: string = this.url;

    const options = { ...DEFAULT_OPTIONS, ...this.options };
    const customHeaders = {};

    if (this.data) {
      if (this.data instanceof FormData) {
        options.body = this.data;
      } else if (options.method === 'GET') {
        url += '?' + stringify(this.data);
      } else {
        customHeaders['Content-Type'] = 'application/x-www-form-urlencoded';
        // $FlowFixMe complains that `data` is nullable
        options.body = stringify(this.data);
      }
    }

    options.headers = {
      ...DEFAULT_HEADERS,
      ...customHeaders,
      ...this.headers,
      ...getCSRFToken()
    };

    return window.fetch(window.baseUrl + url, options);
  }

  setMethod(method: string): Request {
    this.options.method = method;
    return this;
  }

  setData(data?: Object): Request {
    this.data = data;
    return this;
  }

  setHeader(name: string, value: string): Request {
    this.headers[name] = value;
    return this;
  }
}

/**
 * Make a request
 * @param {string} url
 * @returns {Request}
 */
export function request(url: string): Request {
  return new Request(url);
}

/**
 * Check that response status is ok
 * @param response
 * @returns {*}
 */
export function checkStatus(response: Response): Promise<Object> {
  return new Promise((resolve, reject) => {
    if (response.status === 401) {
      // workaround cyclic dependencies
      const requireAuthentication = require('../app/utils/handleRequiredAuthentication').default;
      requireAuthentication();
      reject();
    } else if (response.status >= 200 && response.status < 300) {
      resolve(response);
    } else {
      reject({ response });
    }
  });
}

/**
 * Parse response as JSON
 * @param response
 * @returns {object}
 */
export function parseJSON(response: Response): Promise<Object> {
  return response.json();
}

/**
 * Shortcut to do a GET request and return response json
 * @param url
 * @param data
 */
export function getJSON(url: string, data?: Object): Promise<Object> {
  return request(url).setData(data).submit().then(checkStatus).then(parseJSON);
}

/**
 * Shortcut to do a POST request and return response json
 * @param url
 * @param data
 */
export function postJSON(url: string, data?: Object): Promise<Object> {
  return request(url).setMethod('POST').setData(data).submit().then(checkStatus).then(parseJSON);
}

/**
 * Shortcut to do a POST request
 * @param url
 * @param data
 */
export function post(url: string, data?: Object): Promise<void> {
  return request(url).setMethod('POST').setData(data).submit().then(checkStatus);
}

/**
 * Shortcut to do a POST request and return response json
 * @param url
 * @param data
 */
export function requestDelete(url: string, data?: Object): Promise<Object> {
  return request(url).setMethod('DELETE').setData(data).submit().then(checkStatus);
}

/**
 * Delay promise for testing purposes
 * @param response
 * @returns {Promise}
 */
export function delay(response: *): Promise<*> {
  return new Promise(resolve => setTimeout(() => resolve(response), 1200));
}
