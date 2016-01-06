import { expect } from 'chai';
import { collapsedDirFromPath, fileFromPath } from '../../src/main/js/helpers/path'

describe('Path', function () {
  describe('#collapsedDirFromPath()', function () {
    it('should return null when pass null', function () {
      expect(collapsedDirFromPath(null)).to.be.null;
    });

    it('should return "/" when pass "/"', function () {
      expect(collapsedDirFromPath('/')).to.equal('/');
    });

    it('should not cut short path', function () {
      expect(collapsedDirFromPath('src/main/js/components/state.js')).to.equal('src/main/js/components/');
    });

    it('should cut long path', function () {
      expect(collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js'))
          .to.equal('src/.../js/components/navigator/app/models/');
    });

    it('should cut very long path', function () {
      expect(collapsedDirFromPath('src/main/another/js/components/navigator/app/models/state.js'))
          .to.equal('src/.../js/components/navigator/app/models/');
    });
  });

  describe('#fileFromPath()', function () {
    it('should return null when pass null', function () {
      expect(fileFromPath(null)).to.be.null;
    });

    it('should return empty string when pass "/"', function () {
      expect(fileFromPath('/')).to.equal('');
    });

    it('should return file name when pass only file name', function () {
      expect(fileFromPath('file.js')).to.equal('file.js');
    });

    it('should return file name when pass file path', function () {
      expect(fileFromPath('src/main/js/file.js')).to.equal('file.js');
    });

    it('should return file name when pass file name without extension', function () {
      expect(fileFromPath('src/main/file')).to.equal('file');
    });
  });
});
