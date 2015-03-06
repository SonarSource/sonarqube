var express = require('express'),
    path = require('path'),
    errorhandler = require('errorhandler'),
    serveStatic = require('serve-static'),
    im = require('istanbul-middleware'),
    url = require('url'),
    JS_RE = /\.js$/,
    THIRD_PARTY_RE = /\/third-party\//,
    TEMPLATES_RE = /\/templates\//;

var staticPath = path.join(__dirname, '../main/webapp');
im.hookLoader(staticPath);

var app = express();

app.set('views', __dirname + '/views');
app.set('view engine', 'jade');

app.use(errorhandler({ dumpExceptions: true, showStack: true }));

app.use(im.createClientHandler(staticPath, {
  matcher: function (req) {
    var parsed = url.parse(req.url);
    return parsed.pathname && parsed.pathname.match(JS_RE) &&
        !parsed.pathname.match(THIRD_PARTY_RE) && 
        !parsed.pathname.match(TEMPLATES_RE);
  }
}));
app.use('/coverage', im.createHandler());
app.use('/', serveStatic(staticPath));

app.get('/pages/:page', function (req, res) {
  res.render(req.param('page'));
});

// Get the port from environment variables
var port = process.env.PORT || 8000;

app.listen(port);

console.log('Server running on port %d', port);
