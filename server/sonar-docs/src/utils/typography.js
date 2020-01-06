/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import Typography, { rhythm, scale } from 'typography';

const fontFamily = 'Roboto';

const typography = new Typography({
  bodyFontFamily: [fontFamily, 'serif'],
  headerFontFamily: [fontFamily, 'serif'],
  baseFontSize: '15px',
  bodyWeight: '400',
  headerWeight: '400',
  googleFonts: [{ name: fontFamily, styles: ['400,500,700'] }],
  overrideStyles: () => ({
    a: {
      color: '#439ccd'
    }
  })
});

export { rhythm, scale, typography as default };
