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

import {
  collapsedDirFromPath,
  collapsePath,
  cutLongWords,
  fileFromPath,
  limitComponentName,
  splitPath,
} from '../path';

describe('#collapsedDirFromPath()', () => {
  it('should return null when pass null', () => {
    expect(collapsedDirFromPath(null)).toBeNull();
  });

  it('should return "/" when pass "/"', () => {
    expect(collapsedDirFromPath('/')).toBe('/');
  });

  it('should not cut short path', () => {
    expect(collapsedDirFromPath('src/main/js/components/state.js')).toBe('src/main/js/components/');
  });

  it('should cut long path', () => {
    expect(collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js')).toBe(
      'src/.../components/navigator/app/models/',
    );
  });

  it('should cut very long path', () => {
    expect(
      collapsedDirFromPath('src/main/another/js/components/navigator/app/models/state.js'),
    ).toBe('src/.../components/navigator/app/models/');
  });
});

describe('#fileFromPath()', () => {
  it('should return null when pass null', () => {
    expect(fileFromPath(null)).toBeNull();
  });

  it('should return empty string when pass "/"', () => {
    expect(fileFromPath('/')).toBe('');
  });

  it('should return file name when pass only file name', () => {
    expect(fileFromPath('file.js')).toBe('file.js');
  });

  it('should return file name when pass file path', () => {
    expect(fileFromPath('src/main/js/file.js')).toBe('file.js');
  });

  it('should return file name when pass file name without extension', () => {
    expect(fileFromPath('src/main/file')).toBe('file');
  });
});

describe('#cutLongWords', () => {
  it('should cut the long work in the middle', () => {
    expect(cutLongWords('This is a reallylongwordthatdontexistforthe test')).toBe(
      'This is a reallylongwordthatdontexistfor... test',
    );
  });

  it('should not cut anything', () => {
    expect(cutLongWords('This is a test')).toBe('This is a test');
  });
});

describe('collapsePath', () => {
  it('should fail fast if path is not a string', () => {
    expect(collapsePath({} as string)).toBe('');
  });

  it('should not collapse short path', () => {
    const path = 'my/path';
    expect(collapsePath(path)).toBe(path);
  });

  it('should collapse path longer than the limit', () => {
    const path = 'my/long/path/very/long/path';
    expect(collapsePath(path, 10)).toBe('my/.../very/long/path');
    expect(collapsePath(path, 5)).toBe('my/.../long/path');
    expect(collapsePath(path, 2)).toBe('my/.../path');
  });
});

describe('splitPath', () => {
  it('should split path properly', () => {
    expect(splitPath('my/super/path')).toEqual({ head: 'my/super', tail: 'path' });
    expect(splitPath('my/super/very/long/path')).toEqual({
      head: 'my/super/very/long',
      tail: 'path',
    });
  });
});

describe('limitComponentName', () => {
  const name = 'my/super/name';

  it('should fail fast if component name is not a string', () => {
    expect(limitComponentName({} as string)).toBe('');
  });

  it('should limiit component name longer than the limit', () => {
    expect(limitComponentName(name)).toBe(name);
    expect(limitComponentName(name, 10)).toBe('my/super/n...');
  });
});
