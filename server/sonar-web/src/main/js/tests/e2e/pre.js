/* global casper:false */

var lib = require('../lib');

casper.start();
casper.open(lib.buildRootUrl('/coverage/reset'), {
  method: 'post'
});
casper.test.done();
