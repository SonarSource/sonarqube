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
/* eslint-disable no-console*/
process.env.NODE_ENV = 'production';

const chalk = require('chalk');
const fs = require('fs-extra');
const rimrafSync = require('rimraf').sync;
const webpack = require('webpack');
const paths = require('../config/paths');
const formatSize = require('./utils/formatSize');
const getConfig = require('../config/webpack.config');

const fast = process.argv.some(arg => arg.indexOf('--fast') > -1);

const config = getConfig({ fast, production: true });

function clean() {
  // Remove all content but keep the directory so that
  // if you're in it, you don't end up in Trash
  console.log(chalk.cyan.bold('Cleaning output directories and files...'));

  console.log(paths.jsBuild + '/*');
  rimrafSync(paths.jsBuild + '/*');

  console.log(paths.cssBuild + '/*');
  rimrafSync(paths.cssBuild + '/*');

  console.log(paths.htmlBuild);
  rimrafSync(paths.htmlBuild);

  console.log();
}

function build() {
  if (fast) {
    console.log(chalk.magenta.bold('Running fast build...'));
  } else {
    console.log(chalk.cyan.bold('Creating optimized production build...'));
  }
  console.log();

  webpack(config, (err, stats) => {
    if (err) {
      console.log(chalk.red.bold('Failed to create a production build!'));
      console.log(chalk.red(err.message || err));
      process.exit(1);
    }

    if (stats.compilation.errors && stats.compilation.errors.length) {
      console.log(chalk.red.bold('Failed to create a production build!'));
      stats.compilation.errors.forEach(err => console.log(chalk.red(err.message || err)));
      process.exit(1);
    }

    const jsonStats = stats.toJson();

    console.log('Assets:');
    const assets = jsonStats.assets.slice();
    assets.sort((a, b) => b.size - a.size);
    assets.forEach(asset => {
      let sizeLabel = formatSize(asset.size);
      const leftPadding = ' '.repeat(Math.max(0, 8 - sizeLabel.length));
      sizeLabel = leftPadding + sizeLabel;
      console.log('', chalk.yellow(sizeLabel), asset.name);
    });
    console.log();

    const seconds = jsonStats.time / 1000;
    console.log('Duration: ' + seconds.toFixed(2) + 's');
    console.log();

    console.log(chalk.green.bold('Compiled successfully!'));
  });
}

function copyPublicFolder() {
  fs.copySync(paths.appPublic, paths.appBuild, {
    dereference: true,
    filter: file => file !== paths.appHtml
  });
}

clean();
build();
copyPublicFolder();
