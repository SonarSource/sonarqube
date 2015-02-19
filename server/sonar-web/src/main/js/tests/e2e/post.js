/* global casper:false */

var lib = require('../lib');

casper.download(lib.buildRootUrl('/coverage/download'), '../../../../../../../target/coverage.zip');
casper.test.done();
