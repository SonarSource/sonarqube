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
const { readFileSync } = require('fs');
const path = require('path');

const postCSSPlugin = ({ plugins = [], postcss }) => ({
  name: 'plugin-postcss',
  setup(build) {
    build.onLoad({ filter: /.\.css/ }, async ({ path }) => {
      /*
       * postCssCustomProperties removes all CSS variables from files
       * We want to avoid this in some cases, typically echoes-react provides
       * CSS variable to manage the theme.
       */
      if (path.includes('echoes-react')) {
        return;
      }

      const processor = postcss(plugins);
      const content = readFileSync(path);
      const result = await processor.process(content, { from: path });

      return {
        contents: result.toString(),
        loader: 'css',
      };
    });
  },
});

module.exports = postCSSPlugin;
