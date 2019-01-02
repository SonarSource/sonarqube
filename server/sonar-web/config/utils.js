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
const cssMinimizeOptions = {
  discardComments: { removeAll: true }
};

const cssLoader = ({ production }) => ({
  loader: 'css-loader',
  options: {
    importLoaders: 1,
    minimize: production && cssMinimizeOptions,
    url: false
  }
});

const theme = require('../src/main/js/app/theme');

const customProperties = {};
Object.keys(theme).forEach(key => {
  if (typeof theme[key] === 'string') {
    customProperties[`--${key}`] = theme[key];
  }
});

const postcssLoader = () => ({
  loader: 'postcss-loader',
  options: {
    ident: 'postcss',
    plugins: () => [
      require('autoprefixer'),
      require('postcss-custom-properties')({
        importFrom: { customProperties },
        preserve: false
      }),
      require('postcss-calc')
    ]
  }
});

const minifyParams = ({ production }) =>
  production && {
    removeComments: true,
    collapseWhitespace: true,
    removeRedundantAttributes: true,
    useShortDoctype: true,
    removeEmptyAttributes: true,
    removeStyleLinkTypeAttributes: true,
    keepClosingSlash: true,
    minifyJS: true,
    minifyCSS: true,
    minifyURLs: true
  };

module.exports = {
  cssLoader,
  postcssLoader,
  minifyParams
};
