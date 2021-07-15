const fs = require('fs');
const diff = require('diff');
const globby = require('globby');

const GLOBS = ['**/*.ts', '**/*.tsx', '!node_modules/**/*', '!build/**/*'];

let header;

return readFile('./HEADER')
  .then(h => {
    header = h;
  })
  .then(() => globby(GLOBS))
  .then(readFilesFromPaths)
  .then(checkFiles)
  .then(errors => {
    if (errors) {
      console.error(errors, 'files have an invalid license header');
      process.exit(1);
    } else {
      console.log('✓ All files have valid license headers');
    }
  })
  .catch(e => {
    console.error(e);
    process.exit(1);
  });

function checkFiles(files) {
  return files.reduce((errors, { path, text }) => {
    if (text.slice(0, header.length) !== header) {
      console.error('❌ ', path);
      console.error(
        diff.createPatch(path, header + '\n', text.slice(0, header.length) + '\n', '', '')
      );
      return errors + 1;
    }
    return errors;
  }, 0);
}

function readFilesFromPaths(paths) {
  return Promise.all(paths.map(path => readFile(path).then(text => ({ path, text }))));
}

function readFile(path) {
  return new Promise((resolve, reject) => {
    fs.readFile(path, { encoding: 'utf-8' }, (err, text) => {
      if (err) {
        reject(err);
      } else {
        resolve(text);
      }
    });
  });
}
