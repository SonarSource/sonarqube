/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
const autoprefixer = require('autoprefixer');
const postCssPlugin = require('./esbuild-postcss-plugin');
const postcss = require('postcss');
const postCssCalc = require('postcss-calc');
const postCssCustomProperties = require('postcss-custom-properties');
const tailwindcss = require('tailwindcss');
const htmlPlugin = require('./esbuild-html-plugin');
const htmlTemplate = require('./indexHtmlTemplate');
const {
  getCustomProperties,
  ESBUILD_TARGET_BROWSERS,
  AUTOPREFIXER_BROWSER_LIST,
} = require('./utils');

module.exports = (release) => {
  const plugins = [
    postCssPlugin({
      plugins: [
        autoprefixer({ overrideBrowserslist: AUTOPREFIXER_BROWSER_LIST }),
        postCssCustomProperties({
          importFrom: { customProperties: getCustomProperties() },
          preserve: false,
        }),
        postCssCalc,
        tailwindcss('./tailwind.config.js'),
      ],
      postcss,
    }),
  ];

  if (release) {
    // Only create index.html from template when releasing
    // The devserver will generate its own index file from the template
    plugins.push(htmlPlugin());
  }

  return {
    entryPoints: ['src/main/js/app/index.ts'],
    tsconfig: './tsconfig.json',
    external: ['/images/*', '../fonts/*'],
    loader: {
      '.png': 'dataurl',
      '.md': 'text',
    },
    define: {
      'process.cwd': 'dummy_process_cwd',
    },
    inject: ['config/process-shim.js'],
    bundle: true,
    minify: release,
    metafile: true,
    sourcemap: true,
    target: ESBUILD_TARGET_BROWSERS,
    outdir: 'build/webapp/js',
    entryNames: release ? 'out[hash]' : 'out',
    plugins,
  };
};
