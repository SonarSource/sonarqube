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
import { getJSON, postJSON, post, RequestData } from '../helpers/request';
import throwGlobalError from '../app/utils/throwGlobalError';

/**
 * List tokens for given user login
 */
export function getTokens(login: string): Promise<any> {
  return getJSON('/api/user_tokens/search', { login }).then(r => r.userTokens);
}

/**
 * Generate a user token
 */
export function generateToken(
  tokenName: string,
  userLogin?: string
): Promise<{ name: string; token: string }> {
  const data: RequestData = { name: tokenName };
  if (userLogin) {
    data.login = userLogin;
  }
  return postJSON('/api/user_tokens/generate', data).catch(throwGlobalError);
}

/**
 * Revoke a user token
 */
export function revokeToken(tokenName: string, userLogin?: string): Promise<void | Response> {
  const data: RequestData = { name: tokenName };
  if (userLogin) {
    data.login = userLogin;
  }
  return post('/api/user_tokens/revoke', data).catch(throwGlobalError);
}
