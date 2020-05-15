module.exports = {
  coverageDirectory: '<rootDir>/coverage',
  collectCoverageFrom: ['src/main/js/**/*.{ts,tsx,js}'],
  coverageReporters: ['lcovonly', 'text'],
  globals: {
    'ts-jest': {
      diagnostics: {
        ignoreCodes: [151001]
      }
    }
  },
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
  moduleNameMapper: {
    '^.+\\.(md|jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
      '<rootDir>/config/jest/FileStub.js',
    '^.+\\.css$': '<rootDir>/config/jest/CSSStub.js',
    '^Docs/@types/types$': '<rootDir>/../sonar-docs/src/@types/types.d.ts',
    '^Docs/(.*)': '<rootDir>/../sonar-docs/src/$1'
  },
  setupFiles: [
    '<rootDir>/config/polyfills.js',
    '<rootDir>/config/jest/SetupEnzyme.js',
    '<rootDir>/config/jest/SetupTestEnvironment.ts'
  ],
  snapshotSerializers: ['enzyme-to-json/serializer'],
  testPathIgnorePatterns: ['<rootDir>/config', '<rootDir>/node_modules', '<rootDir>/scripts'],
  testRegex: '(/__tests__/.*|\\-test)\\.(ts|tsx|js)$',
  transform: {
    '\\.js$': 'babel-jest',
    '\\.(ts|tsx)$': 'ts-jest'
  }
};
