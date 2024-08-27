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

const fs = require('fs');
const chalk = require('chalk');
const chokidar = require('chokidar');
const esbuild = require('esbuild');
const http = require('http');
const httpProxy = require('http-proxy');
const getConfig = require('../config/esbuild-config');
const { handleL10n } = require('./utils');
const paths = require('../config/paths');
const { spawn } = require('child_process');
const { buildDesignSystem } = require('./build-design-system');

const STATUS_OK = 200;
const STATUS_ERROR = 500;

const port = process.env.PORT || 3000;
const protocol = process.env.HTTPS === 'true' ? 'https' : 'http';
const host = process.env.HOST || 'localhost';
const proxyTarget = process.env.PROXY || 'http://localhost:9000';

const config = getConfig(false);

function handleStaticFileRequest(req, res) {
  fs.readFile(paths.appBuild + req.url, (err, data) => {
    if (err) {
      // Any unknown path should go to the index.html
      const htmlTemplate = require('../config/indexHtmlTemplate');

      // Replace hash placeholders as well as all the
      // tags that are usually replaced by the server
      const content = htmlTemplate('', '')
        .replace(/%WEB_CONTEXT%/g, '')
        .replace(/%SERVER_STATUS%/g, 'UP')
        .replace(/%INSTANCE%/g, 'SonarQube')
        .replace(/%OFFICIAL%/g, 'true');

      res.writeHead(STATUS_OK);
      res.end(content);
    } else {
      res.writeHead(STATUS_OK);
      res.end(data);
    }
  });
}

const forceBuildDesignSystem = process.argv.includes('--force-build-design-system');

async function run() {
  console.log('starting...');
  const esbuildContext = await esbuild.context(config);
  esbuildContext
    .serve({
      servedir: 'build/webapp',
    })
    .then((result) => {
      const { port: esbuildport } = result;

      const proxy = httpProxy.createProxyServer();
      const esbuildProxy = httpProxy.createProxyServer({
        target: `http://localhost:${esbuildport}`,
      });

      proxy.on('error', (error) => {
        console.error(chalk.blue('Backend'));
        console.error('\t', chalk.red(error.message));
        console.error('\t', error.stack);
      });

      esbuildProxy.on('error', (error) => {
        console.error(chalk.cyan('Frontend'));
        console.error('\t', chalk.red(error.message));
        console.error('\t', error.stack);
      });

      http
        .createServer((req, res) => {
          if (req.url.match(/js\/out/)) {
            esbuildProxy.web(req, res);
          } else if (req.url.match(/l10n\/index/)) {
            handleL10n(res);
          } else if (
            (req.url.includes('api/') && !req.url.includes('/web_api')) ||
            req.url.includes('images/') ||
            req.url.includes('static/')
          ) {
            proxy.web(
              req,
              res,
              {
                target: proxyTarget,
                changeOrigin: true,
              },
              (e) => console.error('req error', e),
            );
          } else {
            handleStaticFileRequest(req, res);
          }
        })
        .listen(port);

      console.log(`server started: http://localhost:${port}`);
    })
    .catch((e) => console.error(e));
}

buildDesignSystem({ callback: run, force: forceBuildDesignSystem });

chokidar
  .watch('./design-system/src', {
    ignored: /(^|[/\\])\../, // ignore dotfiles
    persistent: true,
  })
  .on('change', () => buildDesignSystem({ force: true }));
