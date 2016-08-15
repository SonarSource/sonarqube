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
process.env.NODE_ENV = 'production';

var chalk = require('chalk');
var fs = require('fs');
var path = require('path');
var rimrafSync = require('rimraf').sync;
var webpack = require('webpack');
var paths = require('../config/paths');

var isFastBuild = process.argv.some(arg => arg.indexOf('--fast') > -1);

var config = isFastBuild ?
    require('../config/webpack/webpack.config.fast') :
    require('../config/webpack/webpack.config.prod');

// Remove all content but keep the directory so that
// if you're in it, you don't end up in Trash
console.log(chalk.cyan.bold('Cleaning output directory...'));
console.log(paths.jsBuild + '/*');
console.log();
rimrafSync(paths.jsBuild + '/*');

if (isFastBuild) {
  console.log(chalk.magenta.bold('Running fast build...'));
} else {
  console.log(chalk.cyan.bold('Creating optimized production build...'));
}
console.log();

webpack(config).run(function (err, stats) {
  if (err) {
    console.log(chalk.red.bold('Failed to create a production build!'));
    console.log(chalk.red(err.message || err));
    process.exit(1);
  }

  console.log(chalk.green.bold('Compiled successfully!'));

  var jsonStats = stats.toJson();
  var seconds = jsonStats.time / 1000;
  console.log('Duration: ' + seconds.toFixed(2) + 's');
  console.log();
});
