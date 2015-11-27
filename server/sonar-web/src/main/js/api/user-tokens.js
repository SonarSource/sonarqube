import { getJSON, postJSON, post } from '../helpers/request.js';


/**
 * List tokens for given user login
 * @param {string} login
 * @returns {Promise}
 */
export function getTokens (login) {
  let url = baseUrl + '/api/user_tokens/search';
  let data = { login };
  return getJSON(url, data).then(r => r.userTokens);
}


/**
 * Generate a user token
 * @param {string} userLogin
 * @param {string} tokenName
 * @returns {Promise}
 */
export function generateToken(userLogin, tokenName) {
  let url = baseUrl + '/api/user_tokens/generate';
  let data = { login: userLogin, name: tokenName };
  return postJSON(url, data);
}


/**
 * Revoke a user token
 * @param {string} userLogin
 * @param {string} tokenName
 * @returns {Promise}
 */
export function revokeToken(userLogin, tokenName) {
  let url = baseUrl + '/api/user_tokens/revoke';
  let data = { login: userLogin, name: tokenName };
  return post(url, data);
}
