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
/* eslint-disable no-console*/
process.env.NODE_ENV = 'production';

const chalk = require('chalk');
const webpack = require('webpack');
const sortBy = require('lodash/sortBy');
const formatSize = require('./utils/formatSize');
const getConfigs = require('../config/webpack.config');

const configs = getConfigs({ production: true });

function build() {
  console.log(chalk.cyan.bold('Creating optimized production build...'));
  console.log();

  webpack(configs, (err, stats) => {
    if (err) {
      console.log(chalk.red.bold('Failed to create a production build!'));
      console.log(chalk.red(err.message || err));
      process.exit(1);
    }

    const report = (stats, bundleName, filesLimit = 10) => {
      if (stats.compilation.errors && stats.compilation.errors.length) {
        console.log(chalk.red.bold('Failed to create a production build!'));
        stats.compilation.errors.forEach(err => console.log(chalk.red(err.message || err)));
        process.exit(1);
      }
      const jsonStats = stats.toJson();
      const onlyJS = jsonStats.assets.filter(asset => asset.name.endsWith('.js'));
      console.log(`Biggest js chunks (${onlyJS.length} total) [${bundleName}]:`);
      sortBy(onlyJS, asset => -asset.size)
        .slice(0, filesLimit)
        .forEach(asset => {
          let sizeLabel = formatSize(asset.size);
          const leftPadding = ' '.repeat(Math.max(0, 8 - sizeLabel.length));
          sizeLabel = leftPadding + sizeLabel;
          console.log('', chalk.yellow(sizeLabel), asset.name);
        });
      console.log();
    };

    report(stats.stats[0], 'modern');
    console.log();
    report(stats.stats[1], 'legacy');

    console.log(chalk.green.bold('Compiled successfully!'));
  });
}

build();
