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


export function collapsePath (path, limit = 30) {
  if (typeof path !== 'string') {
    return '';
  }

  var tokens = path.split('/');

  if (tokens.length <= 2) {
    return path;
  }

  var head = _.first(tokens),
      tail = _.last(tokens),
      middle = _.initial(_.rest(tokens)),
      cut = false;

  while (middle.join().length > limit && middle.length > 0) {
    middle.shift();
    cut = true;
  }

  var body = [].concat(head, cut ? ['...'] : [], middle, tail);
  return body.join('/');
}


/**
 * Return a collapsed path without a file name
 * @example
 * // returns 'src/.../js/components/navigator/app/models/'
 * collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js')
 * @param {string} path
 * @returns {string|null}
 */
export function collapsedDirFromPath (path) {
  var limit = 30;
  if (typeof path === 'string') {
    var tokens = _.initial(path.split('/'));
    if (tokens.length > 2) {
      var head = _.first(tokens),
          tail = _.last(tokens),
          middle = _.initial(_.rest(tokens)),
          cut = false;
      while (middle.join().length > limit && middle.length > 0) {
        middle.shift();
        cut = true;
      }
      var body = [].concat(head, cut ? ['...'] : [], middle, tail);
      return body.join('/') + '/';
    } else {
      return tokens.join('/') + '/';
    }
  } else {
    return null;
  }
}


/**
 * Return a file name for a given file path
 * * @example
 * // returns 'state.js'
 * collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js')
 * @param {string} path
 * @returns {string|null}
 */
export function fileFromPath (path) {
  if (typeof path === 'string') {
    var tokens = path.split('/');
    return _.last(tokens);
  } else {
    return null;
  }
}
