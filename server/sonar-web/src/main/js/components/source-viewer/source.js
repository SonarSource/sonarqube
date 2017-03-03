/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'uuid',

  defaults () {
    return {
      exist: true,

      hasSource: false,
      hasCoverage: false,
      hasDuplications: false,
      hasSCM: false,

      canSeeCode: true
    };
  },

  key () {
    return this.get('key');
  },

  addMeta (meta) {
    const source = this.get('source');
    let metaIdx = 0;
    let metaLine = meta[metaIdx];
    source.forEach(line => {
      while (metaLine != null && line.line > metaLine.line) {
        metaLine = meta[++metaIdx];
      }
      if (metaLine != null && line.line === metaLine.line) {
        Object.assign(line, metaLine);
        metaLine = meta[++metaIdx];
      }
    });
    this.set({ source });
  },

  addDuplications (duplications) {
    const source = this.get('source');
    if (source != null) {
      source.forEach(line => {
        const lineDuplications = [];
        duplications.forEach((d, i) => {
          let duplicated = false;
          d.blocks.forEach(b => {
            if (b._ref === '1') {
              const lineFrom = b.from;
              const lineTo = b.from + b.size - 1;
              if (line.line >= lineFrom && line.line <= lineTo) {
                duplicated = true;
              }
            }
          });
          lineDuplications.push(duplicated ? i + 1 : false);
        });
        line.duplications = lineDuplications;
      });
    }
    this.set({ source });
  },

  checkIfHasDuplications () {
    const source = this.get('source');
    let hasDuplications = false;
    if (source != null) {
      source.forEach(line => {
        if (line.duplicated) {
          hasDuplications = true;
        }
      });
    }
    this.set({ hasDuplications });
  },

  hasCoverage (source) {
    return source.some(line => line.coverageStatus != null);
  }
});

