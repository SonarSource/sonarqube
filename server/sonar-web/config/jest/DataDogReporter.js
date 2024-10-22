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

/* eslint-disable no-console */
const fs = require('fs');

const ES_ITEM_CATEGORY = 'Validate-UT-Frontend';

module.exports = class ElasticSearchReporter {
  constructor(globalConfig, options) {
    this.rootDir = globalConfig.rootDir;
    this.outputFilepath = options.outputFilepath;
  }

  stripFilePath(path) {
    return path.replace(this.rootDir, '');
  }

  writeToFile(data) {
    try {
      fs.writeFileSync(this.outputFilepath, JSON.stringify(data));
    } catch (e) {
      console.error(e);
    }
  }

  collectTestData(testClassResults) {
    const commit = process.env.GIT_SHA1;
    const branchName = process.env.GITHUB_BRANCH;
    const build = process.env.BUILD_NUMBER;

    const data = testClassResults.reduce((flattenedTestResults, testClassResult) => {
      const formattedTestResults = this.formatTestResults(
        testClassResult,
        commit,
        build,
        branchName,
      );

      return flattenedTestResults.concat(formattedTestResults);
    }, []);

    this.writeToFile(data);
  }

  formatTestResults(testClassResult, commit, build, branchName) {
    const timestamp = new Date(testClassResult.perfStats.start).toISOString();
    const testClass = this.stripFilePath(testClassResult.testFilePath);

    return testClassResult.testResults
      .filter((test) => test.status === 'failed')
      .map((testResult) => ({
        commit,
        branchName,
        build,
        category: ES_ITEM_CATEGORY,
        timestamp,
        testClass,
        testMethod: testResult.fullName,
        exceptionClass: '',
        exceptionMessage: testResult.failureMessages[0],
        exceptionLogs: testResult.failureMessages[0],
      }));
  }

  onRunComplete(contexts, { testResults }) {
    if (!this.outputFilepath) {
      throw new Error('option `outputFilepath` is undefined');
    }

    this.collectTestData(testResults);
  }
};
