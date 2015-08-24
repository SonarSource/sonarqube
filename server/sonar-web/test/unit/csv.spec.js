define(function (require) {
  var bdd = require('intern!bdd');
  var assert = require('intern/chai!assert');

  var csvEscape = require('../../build/js/libs/csv');

  bdd.describe('#csvEscape', function () {
    bdd.it('should escape', function () {
      assert.equal(csvEscape('Hello, "World"!'), '"Hello, \\"World\\"!"');
    });

    bdd.it('should not escape', function () {
      assert.equal(csvEscape('HelloWorld'), '"HelloWorld"');
    });
  });
});
