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
const babelConfig = require('./babel.config');

babelConfig.presets = [
  ['@babel/preset-env', { targets: { node: 'current' } }],
  '@babel/preset-typescript',
];

module.exports = {
  coverageDirectory: '<rootDir>/coverage',
  collectCoverageFrom: [
    'src/components/**/*.{ts,tsx,js}',
    'src/helpers/**/*.{ts,tsx,js}',
    'src/hooks/**/*.{ts,tsx,js}',
    'src/sonar-aligned/**/*.{ts,tsx,js}',
    '!src/helpers/{keycodes,testUtils}.{ts,tsx}',
  ],
  coverageReporters: ['lcovonly', 'text'],
  globals: {
    'ts-jest': {
      diagnostics: false,
    },
  },
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
  moduleNameMapper: {
    '^.+\\.(md|jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
      '<rootDir>/config/jest/FileStub.js',
    '^.+\\.css$': '<rootDir>/config/jest/CSSStub.js',
  },
  setupFiles: [
    '<rootDir>/config/jest/SetupTestEnvironment.js',
    '<rootDir>/config/jest/SetupTheme.js',
  ],
  setupFilesAfterEnv: [
    '<rootDir>/config/jest/SetupReactTestingLibrary.ts',
    '<rootDir>/../config/jest/SetupFailOnConsole.ts',
  ],
  snapshotSerializers: ['@emotion/jest/serializer'],
  testEnvironment: 'jsdom',
  testPathIgnorePatterns: [
    '<rootDir>/config/jest',
    '<rootDir>/node_modules',
    '<rootDir>/scripts',
    '<rootDir>/lib',
  ],
  testRegex: '(/__tests__/.*|\\-test)\\.(ts|tsx|js)$',
  transform: {
    '^.+\\.(t|j)sx?$': ['babel-jest', babelConfig],
  },
  transformIgnorePatterns: ['/node_modules/(?!(d3-.+))/'],
  reporters: [
    'default'
  ],
  testTimeout: 60000,
};
