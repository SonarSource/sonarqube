/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { latinize, slugify } from '../strings';

describe('#latinize', () => {
  it('should remove diacritics and replace them with normal letters', () => {
    expect(latinize('âêîôûŵŷäëïöüẅÿàèìòùẁỳáéíóúẃýøāēīūčģķļņšž')).toBe(
      'aeiouwyaeiouwyaeiouwyaeiouwyoaeiucgklnsz'
    );
    expect(latinize('ASDFGhjklQWERTz')).toBe('ASDFGhjklQWERTz');
  });
});

describe('#slugify', () => {
  it('should transform text into a slug', () => {
    expect(slugify('Luke Sky&Walker')).toBe('luke-sky-and-walker');
    expect(slugify('tèst_:-ng><@')).toBe('test-ng');
    expect(slugify('my-valid-slug-1')).toBe('my-valid-slug-1');
  });
});
