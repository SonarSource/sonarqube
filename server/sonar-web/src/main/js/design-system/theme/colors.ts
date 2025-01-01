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

export default {
  white: [255, 255, 255],
  black: [0, 0, 0],
  sonarcloud: [243, 112, 42],
  grey: { 50: [235, 235, 235], 100: [221, 221, 221] },
  blueGrey: {
    25: [252, 252, 253],
    35: [247, 249, 252],
    50: [239, 242, 249],
    100: [225, 230, 243],
    200: [197, 205, 223],
    300: [166, 173, 194],
    400: [106, 117, 144],
    500: [62, 67, 87],
    600: [42, 47, 64],
    700: [29, 33, 47],
    800: [18, 20, 29],
    900: [8, 9, 12],
  },
  indigo: {
    25: [244, 246, 255],
    50: [232, 235, 255],
    100: [209, 215, 254],
    200: [189, 198, 255],
    300: [159, 169, 237],
    400: [123, 135, 217],
    500: [93, 108, 208],
    600: [75, 86, 187],
    700: [71, 81, 143],
    800: [43, 51, 104],
    900: [27, 34, 80],
  },
  tangerine: {
    25: [255, 248, 244],
    50: [250, 230, 220],
    100: [246, 206, 187],
    200: [243, 185, 157],
    300: [240, 166, 130],
    400: [237, 148, 106],
    500: [235, 131, 82],
    600: [233, 116, 63],
    700: [231, 102, 49],
    800: [181, 68, 25],
    900: [130, 43, 10],
  },
  green: {
    50: [246, 254, 249],
    100: [236, 253, 243],
    200: [209, 250, 223],
    300: [166, 244, 197],
    400: [50, 213, 131],
    500: [18, 183, 106],
    600: [3, 152, 85],
    700: [2, 122, 72],
    800: [5, 96, 58],
    900: [5, 79, 49],
  },
  yellowGreen: {
    50: [247, 251, 230],
    100: [241, 250, 210],
    200: [225, 245, 168],
    300: [197, 230, 124],
    400: [166, 208, 91],
    500: [110, 183, 18],
    600: [104, 154, 48],
    700: [83, 128, 39],
    800: [63, 104, 29],
    900: [49, 85, 22],
  },
  yellow: {
    50: [252, 245, 228],
    100: [254, 245, 208],
    200: [252, 233, 163],
    300: [250, 220, 121],
    400: [248, 205, 92],
    500: [245, 184, 64],
    600: [209, 152, 52],
    700: [174, 122, 41],
    800: [140, 94, 30],
    900: [102, 64, 15],
  },
  orange: {
    50: [255, 240, 235],
    100: [254, 219, 199],
    200: [255, 214, 175],
    300: [254, 150, 75],
    400: [253, 113, 34],
    500: [247, 95, 9],
    600: [220, 94, 3],
    700: [181, 71, 8],
    800: [147, 55, 13],
    900: [122, 46, 14],
  },
  red: {
    50: [254, 243, 242],
    100: [254, 228, 226],
    200: [254, 205, 202],
    300: [253, 162, 155],
    400: [249, 112, 102],
    500: [240, 68, 56],
    600: [217, 45, 32],
    700: [180, 35, 24],
    800: [128, 27, 20],
    900: [93, 29, 19],
  },
  blue: {
    50: [245, 251, 255],
    100: [233, 244, 251],
    200: [184, 222, 241],
    300: [143, 202, 234],
    400: [110, 185, 228],
    500: [85, 170, 223],
    600: [69, 149, 203],
    700: [58, 127, 173],
    800: [49, 108, 146],
    900: [23, 67, 97],
  },
  codeSnippetLight: {
    body: [51, 53, 60],
    annotations: [34, 84, 192],
    constants: [126, 83, 5],
    comments: [109, 111, 119],
    keyword: [152, 29, 150],
    string: [32, 105, 31],
    'keyword-light': [28, 28, 163], // Not used currently in code snippet
    'preprocessing-directive': [47, 103, 48],
  },
  codeSnippetDark: {
    body: [241, 245, 253],
    annotations: [137, 214, 255],
    constants: [237, 182, 130],
    comments: [156, 164, 175],
    keyword: [251, 173, 255],
    string: [177, 220, 146],
    'keyword-light': [185, 185, 255], // Not used currently in code snippet
    'preprocessing-directive': [133, 228, 134],
  },
  codeSyntaxLight: {
    body: [56, 58, 66],
    annotations: [35, 91, 213],
    constants: [135, 87, 2],
    comments: [95, 96, 102],
    keyword: [162, 34, 160],
    string: [36, 117, 35],
    'keyword-light': [30, 30, 173],
    'preprocessing-directive': [52, 114, 53],
  },
  codeSyntaxDark: {
    body: [226, 231, 241],
    annotations: [97, 174, 238],
    constants: [209, 154, 102],
    comments: [167, 172, 180],
    keyword: [223, 145, 246],
    string: [152, 195, 121],
    'keyword-light': [171, 171, 255],
    'preprocessing-directive': [120, 215, 121],
  },
};
