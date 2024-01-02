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
/* eslint-disable no-console */
process.env.NODE_ENV = 'development';

const chalk = require('chalk');
const { spawn } = require('child_process');

function buildDesignSystem({ callback, force } = {}) {
  process.chdir(`${__dirname}/..`);

  const build = spawn('npx', [
    'turbo',
    'run',
    ...(force ? ['--force=true'] : []),
    'design-system#build:no-checks',
  ]);

  build.stdout.on('data', (data) => {
    console.log(chalk.green.bold(data.toString()));
  });

  build.stderr.on('data', (data) => {
    console.log(chalk.red.bold(data.toString()));
  });

  build.on('exit', function (code) {
    if (code === 0 && callback) {
      callback();
    }
  });
}

module.exports = { buildDesignSystem };
