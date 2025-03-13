module.exports = {
    testEnvironment: 'node',
    collectCoverage: true,
    collectCoverageFrom: [
      '*.{js,jsx}',
      '!*.config.js'
    ],
    coverageDirectory: 'coverage',
  };