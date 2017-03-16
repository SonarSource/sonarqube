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
import { getJSON, postJSON, post } from '../helpers/request';

/**
 * List tokens for given user login
 * @param {string} login
 * @returns {Promise}
 */
export function getTokens(login) {
  const url = '/api/user_tokens/search';
  const data = { login };
  return getJSON(url, data).then(r => r.userTokens);
}

/**
 * Generate a user token
 * @param {string} userLogin
 * @param {string} tokenName
 * @returns {Promise}
 */
export function generateToken(userLogin, tokenName) {
  const url = '/api/user_tokens/generate';
  const data = { login: userLogin, name: tokenName };
  return postJSON(url, data);
}

/**
 * Revoke a user token
 * @param {string} userLogin
 * @param {string} tokenName
 * @returns {Promise}
 */
export function revokeToken(userLogin, tokenName) {
  const url = '/api/user_tokens/revoke';
  const data = { login: userLogin, name: tokenName };
  return post(url, data);
}
