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

import legacy from '@vitejs/plugin-legacy';
import react from '@vitejs/plugin-react';
import autoprefixer from 'autoprefixer';
import path, { resolve } from 'path';
import postCssCalc from 'postcss-calc';
import license from 'rollup-plugin-license';
import { visualizer } from 'rollup-plugin-visualizer';
import tailwind from 'tailwindcss';
import { defineConfig, loadEnv } from 'vite';
import macrosPlugin from 'vite-plugin-babel-macros';
import requireTransform from 'vite-plugin-require-transform';
import babelConfig from './babel.config';
import { ALLOWED_LICENSES, ALLOWED_LICENSE_TEXT, generateLicenseText } from './config/license';
import { viteDevServerHtmlPlugin } from './config/vite-dev-server-html-plugin.mjs';
import { viteDevServerL10nPlugin } from './config/vite-dev-server-l10n-plugin.mjs';
import packageJson from './package.json';

const DEFAULT_DEV_SERVER_PORT = 3000;
const DEFAULT_WS_PROXY_PORT = 3010;

const port = process.env.PORT || DEFAULT_DEV_SERVER_PORT;
const proxyTarget = (process.env.PROXY || 'http://localhost:9000').replace(/\/$/, '');
const isProduction = process.env.NODE_ENV === 'production';
const analyzeBundle = process.env.BUNDLE_ANALYSIS || false;

// https://vitejs.dev/config/
export default ({ mode }) => {
  process.env = { ...process.env, ...loadEnv(mode, process.cwd()) };

  return defineConfig({
    experimental: {
      // The WEB_CONTEXT string is replaced at runtime by the SQ backend web server
      // (WebPagesCache.java) with the instance configured context path
      renderBuiltUrl(filename, { hostId, hostType, type }) {
        if (hostType === 'html') {
          // All the files that are added to the (index.)html file are prefixed with WEB_CONTEXT/
          return 'WEB_CONTEXT/' + filename;
        } else if (hostType === 'js') {
          // All the files that are lazy loaded from a js chunk are prefixed with the WEB_CONTEXT
          // thanks to the __assetsPath function that's defined in index.html.
          return { runtime: `window.__assetsPath(${JSON.stringify(filename)})` };
        } else {
          // Other files (css, images, etc.) are loaded relatively to the current url,
          // automatically taking into account the WEB_CONTEXT
          return { relative: filename };
        }
      },
    },
    build: {
      outDir: 'build/webapp',
      rollupOptions: {
        // we define all the places where a user can land that requires its own bundle entry point.
        // we only have one main entry point which is the index.html for now
        input: {
          main: resolve(__dirname, 'index.html'),
        },
        output: {
          // in order to override the default `build/webapp/assets/` directory we provide our own configuration
          assetFileNames: '[ext]/[name]-[hash][extname]',
          chunkFileNames: 'js/[name]-[hash].js',
          entryFileNames: 'js/[name]-[hash].js',
          // manual chunk splitting strategy. The packages will be split to its own js package
          // We also have one more advantage with manual chunks which is caching. Unless we update
          // the version of following packages, we would have caching on these chunks as the hash
          // remains the same in successive builds as the package isn't changed
          manualChunks: {
            // vendor js chunk will contain only react dependencies
            vendor: ['react', 'react-router-dom', 'react-dom'],
            echoes: ['@sonarsource/echoes-react'],
            datefns: ['date-fns'],
            lodash: ['lodash/lodash.js'],
            highlightjs: [
              'highlight.js',
              'highlightjs-apex',
              'highlightjs-cobol',
              'highlightjs-sap-abap',
            ],
          },
        },
        plugins: [
          // a tool used to concatenate all of our 3rd party licenses together for legal reasons
          license({
            thirdParty: {
              allow: {
                test: ({ license, licenseText }) =>
                  ALLOWED_LICENSES.includes(license) ||
                  ALLOWED_LICENSE_TEXT.some((text) => (licenseText ?? '').includes(text)),
                failOnUnlicensed: false, // default is false. valid-url package has missing license
                failOnViolation: true, // Fail if a dependency specifies a license that does not match requirements
              },
              output: {
                // Output file into build/webapp directory which is included in the build output
                file: path.join(__dirname, 'build/webapp', 'vendor.LICENSE.txt'),
                template(dependencies) {
                  return dependencies
                    .map((dependency) => generateLicenseText(dependency))
                    .join('\n');
                },
              },
            },
          }),
        ],
      },
      sourcemap: isProduction, // enable source maps for production
    },
    css: {
      postcss: {
        plugins: [tailwind('./tailwind.config.js'), autoprefixer, postCssCalc],
      },
    },
    // by default vite doesn't pass along the process.env so we do it here. (for MSW and env code)
    define: {
      'process.env': {
        NODE_ENV: process.env.NODE_ENV,
      },
    },
    optimizeDeps: {
      esbuildOptions: {
        target: 'es2020',
      },
    },
    commonjsOptions: {
      // we don't want to wrap common js modules in { default: ... } when importing from an es module
      defaultIsModuleExports: false,
    },
    esbuild: {
      banner: '/*! licenses: /vendor.LICENSE.txt */',
      legalComments: 'none',
      // https://github.com/vitejs/vite/issues/8644#issuecomment-1159308803
      logOverride: { 'this-is-undefined-in-esm': 'silent' },
    },
    plugins: [
      // additional plugins to allow for the transformation of our existing code to what vite is expecting.
      requireTransform({}),
      legacy({
        modernTargets: packageJson.browserslist,
        polyfills: false,
        modernPolyfills: true,
        renderLegacyChunks: false,
      }),
      react({
        babel: babelConfig,
      }),
      // we use this to support `twin.macro` (macro is a term for a generic babel plugin used at runtime)
      // More Info: https://www.npmjs.com/package/babel-plugin-macros
      macrosPlugin(),
      viteDevServerHtmlPlugin(),
      viteDevServerL10nPlugin(),
      analyzeBundle &&
        visualizer({
          filename: './build/bundle-analyzer.html',
          template: 'treemap',
          gzipSize: true,
        }),
    ],
    // This is the public folder we have to copy to public folder after build
    publicDir: 'public',
    resolve: {
      alias: {
        // src resolution is only applicable for html files and is only needed in vite and not
        // in other configs - tsconfig and storybook
        src: path.resolve(__dirname, 'src'),
        '~sonar-aligned': path.resolve(__dirname, 'src/main/js/sonar-aligned'),
        '~design-system': path.resolve(__dirname, 'src/main/js/design-system/index.ts'),
      },
    },
    server: {
      port,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
        },
        '/static': {
          target: proxyTarget,
          changeOrigin: true,
        },
      },
    },
  });
};
