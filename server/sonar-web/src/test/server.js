/* jshint node: true */
var express = require('express');

var api = require('./server-lib/api'),
    assets = require('./server-lib/assets'),
    mocks = require('./server-lib/mocks'),
    pages = require('./server-lib/pages');

var app = express(),
    port = process.env.PORT || 3000;

app.use(api());
app.use(assets());
app.use(pages());
app.use(mocks());

app.listen(port);

console.log('Server running on port %d', port);
