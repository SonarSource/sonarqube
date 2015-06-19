define(function (require) {
  var bdd = require('intern!bdd');
  var assert = require('intern/chai!assert');

  //require('intern/order!build/js/libs/translate.js');
  require('intern/order!build/js/libs/third-party/jquery.js');
  //require('intern/order!build/js/libs/third-party/jquery-ui.js');
  //require('intern/order!build/js/libs/third-party/d3.js');
  //require('intern/order!build/js/libs/third-party/latinize.js');
  require('intern/order!build/js/libs/third-party/underscore.js');
  //require('intern/order!build/js/libs/third-party/backbone.js');
  //require('intern/order!build/js/libs/third-party/handlebars.js');
  //require('intern/order!build/js/libs/third-party/select2.js');
  require('intern/order!build/js/libs/third-party/keymaster.js');
  //require('intern/order!build/js/libs/third-party/moment.js');
  //require('intern/order!build/js/libs/third-party/numeral.js');
  //require('intern/order!build/js/libs/third-party/numeral-languages.js');
  //require('intern/order!build/js/libs/third-party/bootstrap/tooltip.js');
  //require('intern/order!build/js/libs/third-party/bootstrap/dropdown.js');
  //require('intern/order!build/js/libs/third-party/md5.js');
  //require('intern/order!build/js/libs/select2-jquery-ui-fix.js');
  require('intern/order!build/js/libs/application.js');

  bdd.describe('Application', function () {

    bdd.describe('#collapsedDirFromPath', function () {

      bdd.it('should return null when pass null', function () {
        assert.isNull(window.collapsedDirFromPath(null));
      });

      bdd.it('should return "/" when pass "/"', function () {
        assert.equal(window.collapsedDirFromPath('/'), '/');
      });

      bdd.it('should not cut short path', function () {
        assert.equal(window.collapsedDirFromPath('src/main/js/components/state.js'), 'src/main/js/components/');
      });

      bdd.it('should cut long path', function () {
        assert.equal(window.collapsedDirFromPath('src/main/js/components/navigator/app/models/state.js'),
            'src/.../js/components/navigator/app/models/');
      });

      bdd.it('should cut very long path', function () {
        assert.equal(window.collapsedDirFromPath('src/main/another/js/components/navigator/app/models/state.js'),
            'src/.../js/components/navigator/app/models/');
      });

    });

  });
});
