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
module.exports = {
  coverageDirectory: '<rootDir>/coverage',
  collectCoverageFrom: ['src/main/js/**/*.{ts,tsx,js}', '!helpers/{keycodes,testUtils}.{ts,tsx}'],
  coverageReporters: ['lcovonly', 'text'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
  moduleNameMapper: {
    '^.+\\.(md|jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
      '<rootDir>/config/jest/FileStub.js',
    '^.+\\.css$': '<rootDir>/config/jest/CSSStub.js',
  },
  setupFiles: [
    '<rootDir>/config/polyfills.ts',
    '<rootDir>/config/jest/SetupTestEnvironment.ts',
    '<rootDir>/config/jest/SetupTheme.js',
  ],
  setupFilesAfterEnv: [
    '<rootDir>/config/jest/SetupReactTestingLibrary.ts',
    '<rootDir>/config/jest/SetupJestAxe.ts',
  ],
  snapshotSerializers: ['@emotion/jest/serializer'],
  testEnvironment: 'jsdom',
  testPathIgnorePatterns: [
    '<rootDir>/config',
    '<rootDir>/design-system',
    '<rootDir>/node_modules',
    '<rootDir>/scripts',
  ],
  testRegex: '(/__tests__/.*|\\-test)\\.(ts|tsx|js)$',
  transform: {
    '^.+\\.(t|j)sx?$': [
      '@swc/jest',
      {
        jsc: {
          target: 'es2018',
        },
      },
    ],
  },
  transformIgnorePatterns: ['/node_modules/(?!(d3-.+))/'],
  reporters: [
    'default',
    [
      'jest-junit',
      {
        outputDirectory: 'build/test-results/test-jest',
        outputName: 'junit.xml',
        ancestorSeparator: ' > ',
        suiteNameTemplate: '{filename}',
        classNameTemplate: '{classname}',
        titleTemplate: '{title}',
      },
    ],
    [
      './config/jest/DataDogReporter.js',
      {
        outputFilepath: '/tmp/ut-ts-web-test-monitoring.log',
      },
    ],
    ['jest-slow-test-reporter', { numTests: 5, warnOnSlowerThan: 10000, color: true }],
  ],
  testTimeout: 60000,
};
