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
import { isNil, omitBy } from 'lodash';
import { stringify } from 'querystring';
import { getCookie } from './cookies';
import { getUrlContext } from './init';
import { translate } from './l10n';

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
  method: 'GET',
};

/**
 * Default request headers
 */
const DEFAULT_HEADERS = {
  Accept: 'application/json',
};

/**
 * Request
 */
class Request {
  private data?: RequestData;
  private isJSON = false;

  constructor(private readonly url: string, private readonly options: { method?: string } = {}) {}

  getSubmitData(customHeaders: any = {}): { url: string; options: RequestInit } {
    let { url } = this;
    const options: RequestInit = { ...DEFAULT_OPTIONS, ...this.options };

    if (this.data) {
      if (this.data instanceof FormData) {
        options.body = this.data;
      } else if (this.isJSON) {
        customHeaders['Content-Type'] = 'application/json';
        options.body = JSON.stringify(this.data);
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
      ...customHeaders,
    };
    return { url, options };
  }

  submit(): Promise<Response> {
    const { url, options } = this.getSubmitData({ ...getCSRFToken() });
    return window.fetch(getUrlContext() + url, options);
  }

  setMethod(method: string): Request {
    this.options.method = method;
    return this;
  }

  setData(data?: RequestData, isJSON = false): Request {
    if (data) {
      this.data = data;
      this.isJSON = isJSON;
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
  request.submit = function () {
    const { url, options } = this.getSubmitData();
    return window.fetch(url, options);
  };
  return request;
}

/**
 * Check that response status is ok
 */
export function checkStatus(response: Response, bypassRedirect?: boolean): Promise<Response> {
  return new Promise((resolve, reject) => {
    if (response.status === HttpStatus.Unauthorized && !bypassRedirect) {
      import('./handleRequiredAuthentication')
        .then((i) => i.default())
        .then(reject, reject);
    } else if (isSuccessStatus(response.status)) {
      resolve(response);
    } else {
      reject(response);
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
 * Parse response as Text
 */
export function parseText(response: Response): Promise<string> {
  return response.text();
}

/**
 * Parse response of failed request
 */
export function parseError(response: Response): Promise<string> {
  const DEFAULT_MESSAGE = translate('default_error_message');
  return parseJSON(response)
    .then(({ errors }) => errors.map((error: any) => error.msg).join('. '))
    .catch(() => DEFAULT_MESSAGE);
}

/**
 * Shortcut to do a GET request and return a Response
 */
export function get(url: string, data?: RequestData, bypassRedirect?: boolean): Promise<Response> {
  return request(url)
    .setData(data)
    .submit()
    .then((response) => checkStatus(response, bypassRedirect));
}

/**
 * Shortcut to do a GET request and return response json
 */
export function getJSON(url: string, data?: RequestData, bypassRedirect?: boolean): Promise<any> {
  return get(url, data, bypassRedirect).then(parseJSON);
}

/**
 * Shortcut to do a GET request and return response text
 */
export function getText(
  url: string,
  data?: RequestData,
  bypassRedirect?: boolean
): Promise<string> {
  return get(url, data, bypassRedirect).then(parseText);
}

/**
 * Shortcut to do a CORS GET request and return response json
 */
export function getCorsJSON(url: string, data?: RequestData): Promise<any> {
  return corsRequest(url)
    .setData(data)
    .submit()
    .then((response) => {
      if (isSuccessStatus(response.status)) {
        return parseJSON(response);
      } else {
        return Promise.reject(response);
      }
    });
}

/**
 * Shortcut to do a POST request and return response json
 */
export function postJSON(url: string, data?: RequestData, bypassRedirect?: boolean): Promise<any> {
  return request(url)
    .setMethod('POST')
    .setData(data)
    .submit()
    .then((response) => checkStatus(response, bypassRedirect))
    .then(parseJSON);
}

/**
 * Shortcut to do a POST request with a json body and return response json
 */
export function postJSONBody(
  url: string,
  data?: RequestData,
  bypassRedirect?: boolean
): Promise<any> {
  return request(url)
    .setMethod('POST')
    .setData(data, true)
    .submit()
    .then((response) => checkStatus(response, bypassRedirect))
    .then(parseJSON);
}

/**
 * Shortcut to do a POST request
 */
export function post(url: string, data?: RequestData, bypassRedirect?: boolean): Promise<void> {
  return new Promise((resolve, reject) => {
    request(url)
      .setMethod('POST')
      .setData(data)
      .submit()
      .then((response) => checkStatus(response, bypassRedirect))
      .then(() => resolve(), reject);
  });
}

function tryRequestAgain<T>(
  repeatAPICall: () => Promise<T>,
  tries: { max: number; slowThreshold: number },
  stopRepeat: (response: T) => boolean,
  repeatErrors: number[] = [],
  lastError?: Response
) {
  tries.max--;
  if (tries.max !== 0) {
    return new Promise<T>((resolve) => {
      setTimeout(
        () => resolve(requestTryAndRepeatUntil(repeatAPICall, tries, stopRepeat, repeatErrors)),
        tries.max > tries.slowThreshold ? 500 : 3000
      );
    });
  }
  return Promise.reject(lastError);
}

export function requestTryAndRepeatUntil<T>(
  repeatAPICall: () => Promise<T>,
  tries: { max: number; slowThreshold: number },
  stopRepeat: (response: T) => boolean,
  repeatErrors: number[] = []
) {
  return repeatAPICall().then(
    (r) => {
      if (stopRepeat(r)) {
        return r;
      }
      return tryRequestAgain(repeatAPICall, tries, stopRepeat, repeatErrors);
    },
    (error: Response) => {
      if (repeatErrors.length === 0 || repeatErrors.includes(error.status)) {
        return tryRequestAgain(repeatAPICall, tries, stopRepeat, repeatErrors, error);
      }
      return Promise.reject(error);
    }
  );
}

export function isSuccessStatus(status: number) {
  return status >= 200 && status < 300;
}

// Adapted from https://nodejs.org/api/http.html#http_http_HTTP_STATUS
export enum HttpStatus {
  Ok = 200,
  Created = 201,
  MultipleChoices = 300,
  MovedPermanently = 301,
  Found = 302,
  BadRequest = 400,
  Unauthorized = 401,
  Forbidden = 403,
  NotFound = 404,
  InternalServerError = 500,
  NotImplemented = 501,
  BadGateway = 502,
  ServiceUnavailable = 503,
  GatewayTimeout = 504,
}
