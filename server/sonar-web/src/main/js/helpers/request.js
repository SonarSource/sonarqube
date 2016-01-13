/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';

/**
 * Default options for any request
 * @type {{credentials: string}}
 */
const OPTIONS = {
  method: 'GET',
  credentials: 'same-origin'
};


/**
 * Default request headers
 * @type {{Accept: string}}
 */
const HEADERS = {
  'Accept': 'application/json'
};


/**
 * Create a query string from an object
 * @param {object} parameters
 * @returns {string}
 */
function queryString (parameters) {
  return Object.keys(parameters)
      .map(key => {
        return `${encodeURIComponent(key)}=${encodeURIComponent(parameters[key])}`;
      })
      .join('&');
}


/**
 * Request
 */
class Request {
  constructor (url) {
    this.url = url;
    this.options = {};
    this.headers = {};
  }

  submit () {
    let url = this.url;
    let options = _.defaults(this.options, OPTIONS);
    options.headers = _.defaults(this.headers, HEADERS);
    if (this.data) {
      if (options.method === 'GET') {
        url += '?' + queryString(this.data);
      } else {
        options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
        options.body = queryString(this.data);
      }
    }
    return window.fetch(url, options);
  }

  setMethod (method) {
    this.options.method = method;
    return this;
  }

  setData (data) {
    this.data = data;
    return this;
  }
}


/**
 * Make a request
 * @param {string} url
 * @returns {Request}
 */
export function request (url) {
  return new Request(url);
}


/**
 * Check that response status is ok
 * @param response
 * @returns {*}
 */
export function checkStatus (response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  } else {
    var error = new Error(response.status);
    error.response = response;
    throw error;
  }
}


/**
 * Parse response as JSON
 * @param response
 * @returns {object}
 */
export function parseJSON (response) {
  return response.json();
}


/**
 * Shortcut to do a GET request and return response json
 * @param url
 * @param data
 */
export function getJSON (url, data) {
  return request(url)
      .setData(data)
      .submit()
      .then(checkStatus)
      .then(parseJSON);
}


/**
 * Shortcut to do a POST request and return response json
 * @param url
 * @param data
 */
export function postJSON (url, data) {
  return request(url)
      .setMethod('POST')
      .setData(data)
      .submit()
      .then(checkStatus)
      .then(parseJSON);
}


/**
 * Shortcut to do a POST request and return response json
 * @param url
 * @param data
 */
export function post (url, data) {
  return request(url)
      .setMethod('POST')
      .setData(data)
      .submit()
      .then(checkStatus);
}


/**
 * Delay promise for testing purposes
 * @param response
 * @returns {Promise}
 */
export function delay (response) {
  return new Promise(resolve => setTimeout(() => resolve(response), 3000));
}
