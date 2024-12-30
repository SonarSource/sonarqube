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

import * as ThemeHelper from '../../helpers/theme';
import { lightTheme } from '../../theme';

const props = {
  color: 'rgb(0,0,0)',
};

describe('getProp', () => {
  it('should work', () => {
    expect(ThemeHelper.getProp('color')(props)).toEqual('rgb(0,0,0)');
  });
});

describe('themeColor', () => {
  it('should work for light theme', () => {
    expect(ThemeHelper.themeColor('backgroundPrimary')({ theme: lightTheme })).toEqual(
      'rgb(252,252,253)',
    );
  });

  it('should work with a theme-defined opacity', () => {
    expect(ThemeHelper.themeColor('bannerIconHover')({ theme: lightTheme })).toEqual(
      'rgba(217,45,32,0.2)',
    );
  });

  it('should work for all kind of color parameters', () => {
    expect(ThemeHelper.themeColor('transparent')({ theme: lightTheme })).toEqual('transparent');
    expect(ThemeHelper.themeColor('currentColor')({ theme: lightTheme })).toEqual('currentColor');
    expect(ThemeHelper.themeColor('var(--test)')({ theme: lightTheme })).toEqual('var(--test)');
    expect(ThemeHelper.themeColor('rgb(0,0,0)')({ theme: lightTheme })).toEqual('rgb(0,0,0)');
    expect(ThemeHelper.themeColor('rgba(0,0,0,1)')({ theme: lightTheme })).toEqual('rgba(0,0,0,1)');
    expect(
      ThemeHelper.themeColor(ThemeHelper.themeContrast('backgroundPrimary')({ theme: lightTheme }))(
        {
          theme: lightTheme,
        },
      ),
    ).toEqual('rgb(8,9,12)');
    expect(
      ThemeHelper.themeColor(ThemeHelper.themeAvatarColor('luke')({ theme: lightTheme }))({
        theme: lightTheme,
      }),
    ).toEqual('rgb(209,215,254)');
  });
});

describe('themeContrast', () => {
  it('should work for light theme', () => {
    expect(ThemeHelper.themeContrast('backgroundPrimary')({ theme: lightTheme })).toEqual(
      'rgb(8,9,12)',
    );
  });

  it('should work for all kind of color parameters', () => {
    expect(ThemeHelper.themeContrast('var(--test)')({ theme: lightTheme })).toEqual('var(--test)');
    expect(ThemeHelper.themeContrast('rgb(0,0,0)')({ theme: lightTheme })).toEqual('rgb(0,0,0)');
    expect(ThemeHelper.themeContrast('rgba(0,0,0,1)')({ theme: lightTheme })).toEqual(
      'rgba(0,0,0,1)',
    );
    expect(
      ThemeHelper.themeContrast(ThemeHelper.themeColor('backgroundPrimary')({ theme: lightTheme }))(
        {
          theme: lightTheme,
        },
      ),
    ).toEqual('rgb(252,252,253)');
    expect(
      ThemeHelper.themeContrast(ThemeHelper.themeAvatarColor('luke')({ theme: lightTheme }))({
        theme: lightTheme,
      }),
    ).toEqual('rgb(209,215,254)');
    expect(
      ThemeHelper.themeContrast('backgroundPrimary')({
        theme: {
          ...lightTheme,
          contrasts: { ...lightTheme.contrasts, backgroundPrimary: 'inherit' },
        },
      }),
    ).toEqual('inherit');
  });
});

describe('themeBorder', () => {
  it('should work for light theme', () => {
    expect(ThemeHelper.themeBorder()({ theme: lightTheme })).toEqual('1px solid rgb(235,235,235)');
  });
  it('should allow to override the color of the border', () => {
    expect(ThemeHelper.themeBorder('focus', 'primaryLight')({ theme: lightTheme })).toEqual(
      '4px solid rgba(123,135,217,0.2)',
    );
  });
  it('should allow to override the opacity of the border', () => {
    expect(ThemeHelper.themeBorder('focus', undefined, 0.5)({ theme: lightTheme })).toEqual(
      '4px solid rgba(197,205,223,0.5)',
    );
  });
  it('should allow to pass a CSS prop as color name', () => {
    expect(
      ThemeHelper.themeBorder('focus', 'var(--outlineColor)', 0.5)({ theme: lightTheme }),
    ).toEqual('4px solid var(--outlineColor)');
  });
});

describe('themeShadow', () => {
  it('should work for light theme', () => {
    expect(ThemeHelper.themeShadow('xs')({ theme: lightTheme })).toEqual(
      '0px 1px 2px 0px rgba(29,33,47,0.05)',
    );
  });
  it('should allow to override the color of the shadow', () => {
    expect(ThemeHelper.themeShadow('xs', 'backgroundPrimary')({ theme: lightTheme })).toEqual(
      '0px 1px 2px 0px rgba(252,252,253,0.05)',
    );
    expect(ThemeHelper.themeShadow('xs', 'transparent')({ theme: lightTheme })).toEqual(
      '0px 1px 2px 0px transparent',
    );
  });
  it('should allow to override the opacity of the shadow', () => {
    expect(ThemeHelper.themeShadow('xs', 'backgroundPrimary', 0.8)({ theme: lightTheme })).toEqual(
      '0px 1px 2px 0px rgba(252,252,253,0.8)',
    );
  });
  it('should allow to pass a CSS prop as color name', () => {
    expect(ThemeHelper.themeShadow('xs', 'var(--shadowColor)')({ theme: lightTheme })).toEqual(
      '0px 1px 2px 0px var(--shadowColor)',
    );
  });
});
