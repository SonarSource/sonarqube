define(function (require) {
  var bdd = require('intern!bdd');
  require('../helpers/test-page');

  bdd.describe('Maintenance Page', function () {
    bdd.it('should work', function () {
      return this.remote
          .open()
          .mockFromFile('/api/system/status', 'maintenance-spec/status-up.json')
          .startAppBrowserify('maintenance', { setup: false })
          .checkElementExist('.maintenance-title')
          .checkElementExist('.maintenance-title')
          .checkElementExist('.maintenance-text');
    });

    bdd.it('should change status', function () {
      return this.remote
          .open()
          .mockFromFile('/api/system/status', 'maintenance-spec/status-up.json')
          .startAppBrowserify('maintenance', { setup: false })
          .checkElementExist('.maintenance-title')
          .checkElementNotExist('.maintenance-title.text-danger')
          .clearMocks()
          .mockFromFile('/api/system/status', 'maintenance-spec/status-down.json')
          .checkElementExist('.maintenance-title.text-danger');
    });
  });
});
