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
/* eslint-disable no-console*/
process.env.NODE_ENV = 'production';

const fs = require('fs-extra');
const esbuild = require('esbuild');
const chalk = require('chalk');
const { performance } = require('perf_hooks');
const paths = require('../config/paths');

const getConfig = require('../config/esbuild-config');

const release = process.argv.findIndex(val => val === 'release') >= 0;

function clean() {
  fs.emptyDirSync(paths.appBuild);
}

async function build() {
  const start = performance.now();
  console.log(chalk.cyan.bold(`Creating ${release ? 'optimized' : 'fast'} production build...`));
  console.log();

  await esbuild.build(getConfig(release)).catch(() => process.exit(1));

  console.log(chalk.green.bold('Compiled successfully!'));
  console.log(chalk.cyan(Math.round(performance.now() - start), 'ms'));
  console.log();
}

function copyAssets() {
  fs.copySync(paths.appPublic, paths.appBuild);
}

(async () => {
  clean();
  await build();
  copyAssets();
})();
