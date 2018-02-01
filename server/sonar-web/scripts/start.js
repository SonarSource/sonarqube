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
/* eslint-disable no-console */
process.env.NODE_ENV = 'development';

const chalk = require('chalk');
const webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');
const clearConsole = require('react-dev-utils/clearConsole');
const formatWebpackMessages = require('react-dev-utils/formatWebpackMessages');
const errorOverlayMiddleware = require('react-error-overlay/middleware');
const getConfig = require('../config/webpack.config');
const paths = require('../config/paths');
const getMessages = require('./utils/getMessages');

const config = getConfig({ production: false });

const port = process.env.PORT || 3000;
const protocol = process.env.HTTPS === 'true' ? 'https' : 'http';
const host = process.env.HOST || 'localhost';
const proxy = process.env.PROXY || 'http://localhost:9000';

const compiler = setupCompiler(host, port, protocol);

runDevServer(compiler, host, port, protocol);

function setupCompiler(host, port, protocol) {
  const compiler = webpack(config);

  compiler.plugin('invalid', () => {
    clearConsole();
    console.log('Compiling...');
  });

  compiler.plugin('done', stats => {
    clearConsole();

    const jsonStats = stats.toJson({}, true);
    const messages = formatWebpackMessages(jsonStats);
    const seconds = jsonStats.time / 1000;
    if (!messages.errors.length && !messages.warnings.length) {
      console.log(chalk.green('Compiled successfully!'));
      console.log('Duration: ' + seconds.toFixed(2) + 's');
      console.log();
      console.log('The app is running at:');
      console.log();
      console.log('  ' + chalk.cyan(protocol + '://' + host + ':' + port + '/'));
      console.log();
    }

    if (messages.errors.length) {
      console.log(chalk.red('Failed to compile.'));
      console.log();
      messages.errors.forEach(message => {
        console.log(message);
        console.log();
      });
    }
  });

  return compiler;
}

function runDevServer(compiler, host, port, protocol) {
  const devServer = new WebpackDevServer(compiler, {
    before(app) {
      app.use(errorOverlayMiddleware());
      app.get('/api/l10n/index', (req, res) => {
        getMessages()
          .then(messages => res.json({ effectiveLocale: 'en', messages }))
          .catch(() => res.status(500));
      });
    },
    compress: true,
    clientLogLevel: 'none',
    contentBase: paths.appPublic,
    disableHostCheck: true,
    hot: true,
    publicPath: config.output.publicPath,
    quiet: true,
    watchOptions: {
      ignored: /node_modules/
    },
    https: protocol === 'https',
    host,
    overlay: false,
    historyApiFallback: {
      disableDotRule: true
    },
    proxy: {
      '/api': proxy,
      '/fonts': proxy,
      '/images': proxy,
      '/static': proxy
    }
  });

  devServer.listen(port, err => {
    if (err) {
      console.log(err);
      return;
    }

    clearConsole();
    console.log(chalk.cyan('Starting the development server...'));
    console.log();
  });
}
