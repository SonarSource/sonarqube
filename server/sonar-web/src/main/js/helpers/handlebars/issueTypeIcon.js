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
import Handlebars from 'handlebars/runtime';

/* eslint-disable max-len */
const bug = new Handlebars.default.SafeString(
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 14" width="14" height="14" style="position: relative; top: -1px; vertical-align: middle">
      <g transform="matrix(1,0,0,1,0.495158,0.453789)">
        <path style="fill: currentColor" d="M10.3 8l1.4 1.2.7-.8L10.7 7H9v-.3l2-2.3V2h-1v2L9 5.1V4h-.2c-.1-.8-.6-1.5-1.3-1.8L8.9.8 8.1.1 6.5 1.7 4.9.1l-.7.7 1.4 1.4c-.8.3-1.3 1-1.4 1.8H4v1.1L3 4V2H2v2.3l2 2.3V7H2.3L.7 8.4l.7.8L2.7 8H4v.3l-2 1.9V13h1v-2.4l1-1C4 11 5.1 12 6.4 12h.8c.7 0 1.4-.3 1.8-.9.3-.4.3-.9.2-1.4l.9.9V13h1v-2.8L9 8.3V8h1.3zM6 10V4.3h1V10H6z"/>
      </g>
    </svg>`
);
const vulnerability = new Handlebars.default.SafeString(
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 14" width="14" height="14" style="position: relative; top: -1px; vertical-align: middle">
      <g transform="matrix(1,0,0,1,2.49176,-0.686483)">
        <path style="fill: currentColor" d="M7.5 6H3V3.6c0-1 .8-1.9 1.9-1.9s1.9.8 1.9 1.9c0 .3.2.5.5.5s.5-.2.5-.5C7.8 2 6.5.7 4.9.7S2 2.1 2 3.6V6h-.5C.7 6 0 6.7 0 7.5v5c0 .8.7 1.5 1.5 1.5h6c.8 0 1.5-.7 1.5-1.5v-5C9 6.7 8.3 6 7.5 6zM3 9.5C3 8.7 3.7 8 4.5 8S6 8.7 6 9.5c0 .7-.4 1.2-1 1.4V12H4v-1.1c-.6-.2-1-.7-1-1.4zM10.9 2.9L9.5 3c.1 0 .2 0 .3-.1l1-1c.2-.2.2-.5 0-.7s-.5-.2-.7 0l-1 1c-.1.1-.1.5 0 .7.1 0 .3.1.4.1-.3 0-.5.3-.5.5 0 .3.2.5.5.5l1.4-.1c.3 0 .5-.3.5-.5 0-.3-.3-.5-.5-.5z"/>
        <path style="fill: currentColor" d="M9.8 4.1c-.2-.2-.5-.1-.7.1-.2.2-.1.5.1.7l1.1.9c.1.1.2.1.3.1.1 0 .3-.1.4-.2.2-.2.1-.5-.1-.7l-1.1-.9z"/>
      </g>
    </svg>`
);
const codeSmell = new Handlebars.default.SafeString(
    `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 14" width="14" height="14" style="position: relative; top: -1px; vertical-align: middle">
      <g transform="matrix(1,0,0,1,0.5,0.5)">
        <path style="fill: currentColor" d="M6.5 0C2.9 0 0 2.9 0 6.5S2.9 13 6.5 13 13 10.1 13 6.5 10.1 0 6.5 0zM6 6h1v1H6V6zm-4.1.2c-.1 0-.2-.1-.2-.2 0-.4.2-1.3.7-2.1.5-1 1.3-1.5 1.6-1.7.1-.1.2 0 .3.1l1.4 2.5c0 .1 0 .2-.1.3-.2.1-.3.3-.4.4-.1.2-.2.4-.2.6 0 .1-.1.2-.2.2l-2.9-.1zm6.7 4.7c-.3.2-1.2.5-2.2.5-1 0-1.8-.4-2.2-.5-.1-.1-.1-.2-.1-.3l1.4-2.5c.1-.1.2-.1.3-.1.2.1.4.1.6.1.2 0 .4 0 .6-.1.1 0 .2 0 .3.1l1.4 2.5c0 .1 0 .2-.1.3zm2.6-4.5l-2.8.1c-.1 0-.2-.1-.2-.2 0-.2-.1-.4-.2-.6l-.4-.4c-.1 0-.2-.2-.1-.2l1.4-2.5c.1-.1.2-.1.3-.1.3.2 1 .7 1.6 1.6.5.9.6 1.8.7 2.1-.1.1-.1.2-.3.2z"/>
      </g>
    </svg>`
);

module.exports = function (type) {
  switch (type) {
    case 'BUG':
      return bug;
    case 'VULNERABILITY':
      return vulnerability;
    case 'CODE_SMELL':
      return codeSmell;
    default:
      return '';
  }
};
