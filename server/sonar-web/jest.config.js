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
  testTimeout: 40000,
};
