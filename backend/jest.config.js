module.exports = {
    testEnvironment: 'node',
    collectCoverage: true,
    collectCoverageFrom: [
      '*.{js,jsx}',
      '!jest.config.js'
    ],
    coverageDirectory: 'coverage',
  };