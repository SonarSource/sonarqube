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
import react from '@vitejs/plugin-react';
import autoprefixer from 'autoprefixer';
import { resolve } from 'node:path';
import postCssCalc from 'postcss-calc';
import tailwind from 'tailwindcss';
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';
import babelConfig from './babel.config';
import * as packageJson from './package.json';

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    lib: {
      entry: resolve('src', 'index.ts'),
      name: 'MIUI',
      formats: ['es'],
      fileName: (_format) => `index.js`,
    },
    outDir: 'lib',
    rollupOptions: {
      external: [...Object.keys(packageJson.peerDependencies)],
    },
  },
  css: {
    postcss: {
      plugins: [tailwind('../tailwind.config.js'), autoprefixer, postCssCalc],
    },
  },
  esbuild: {
    // https://github.com/vitejs/vite/issues/8644#issuecomment-1159308803
    logOverride: { 'this-is-undefined-in-esm': 'silent' },
  },
  optimizeDeps: {
    esbuildOptions: {
      target: 'es2022',
    },
  },
  plugins: [
    react({
      babel: babelConfig,
    }),
    dts({
      entryRoot: 'src',
    }),
  ],
});
