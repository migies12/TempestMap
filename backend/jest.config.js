module.exports = {
    testEnvironment: 'node',
    collectCoverage: true,
    collectCoverageFrom: [
      'backend/*.{js,jsx}',
    ],
    coverageDirectory: 'coverage',
  };