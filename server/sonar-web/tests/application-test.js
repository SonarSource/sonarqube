describe.skip('Application', function () {
  describe('#collapsedDirFromPath()', function () {
    it('should return null when pass null', function () {
      assert.isNull(window.collapsedDirFromPath(null));
    });

    it('should return "/" when pass "/"', function () {
      assert.equal(window.collapsedDirFromPath('/'), '/');
    });

    it('should not cut short path', function () {
      assert.equal(window.collapsedDirFromPath('src/main/js/components/state.js'), 'src/main/js/components/');
    });

    it('should cut long path', function () {
      assert.equal(window.collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js'),
          'src/.../js/components/navigator/app/models/');
    });

    it('should cut very long path', function () {
      assert.equal(window.collapsedDirFromPath('src/main/another/js/components/navigator/app/models/state.js'),
          'src/.../js/components/navigator/app/models/');
    });
  });

  describe('#fileFromPath()', function () {
    it('should return null when pass null', function () {
      assert.isNull(window.fileFromPath(null));
    });

    it('should return empty string when pass "/"', function () {
      assert.equal(window.fileFromPath('/'), '');
    });

    it('should return file name when pass only file name', function () {
      assert.equal(window.fileFromPath('file.js'), 'file.js');
    });

    it('should return file name when pass file path', function () {
      assert.equal(window.fileFromPath('src/main/js/file.js'), 'file.js');
    });

    it('should return file name when pass file name without extension', function () {
      assert.equal(window.fileFromPath('src/main/file'), 'file');
    });
  });

  describe('Severity Comparators', function () {
    describe('#severityComparator', function () {
      it('should have correct order', function () {
        assert.equal(window.severityComparator('BLOCKER'), 0);
        assert.equal(window.severityComparator('CRITICAL'), 1);
        assert.equal(window.severityComparator('MAJOR'), 2);
        assert.equal(window.severityComparator('MINOR'), 3);
        assert.equal(window.severityComparator('INFO'), 4);
      });
    });

    describe('#severityColumnsComparator', function () {
      it('should have correct order', function () {
        assert.equal(window.severityColumnsComparator('BLOCKER'), 0);
        assert.equal(window.severityColumnsComparator('CRITICAL'), 2);
        assert.equal(window.severityColumnsComparator('MAJOR'), 4);
        assert.equal(window.severityColumnsComparator('MINOR'), 1);
        assert.equal(window.severityColumnsComparator('INFO'), 3);
      });
    });
  });
});
