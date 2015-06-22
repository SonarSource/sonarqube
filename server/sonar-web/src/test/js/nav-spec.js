/* globals describe:false, it:false */
var lib = require('../lib');

describe('Navigation App', function () {

  it('should show global spaces', 7, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/navigation/global', 'global.json');
        })

        .then(function () {
          casper.evaluate(function () {
            window.SS.isUserAdmin = false;
            require(['apps/nav/app'], function (App) {
              App.start();
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.navbar-global .nav');
        })

        .then(function () {
          // check global spaces
          test.assertExists('.navbar-global a[href*="/issues"]');
          test.assertExists('.navbar-global a[href*="/measures"]');
          test.assertExists('.navbar-global a[href*="/coding_rules"]');
          test.assertExists('.navbar-global a[href*="/profiles"]');
          test.assertExists('.navbar-global a[href*="/quality_gates"]');

          // should not see settings
          test.assertDoesntExist('.navbar-global a[href*="/settings"]');

          // check "more"
          test.assertExists('.navbar-global a[href*="/comparison"]');
        });
  });

  it('should show global dashboards', 12, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/navigation/global', 'global.json');
        })

        .then(function () {
          casper.evaluate(function () {
            window.SS.isUserAdmin = false;
            require(['apps/nav/app'], function (App) {
              App.start();
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.navbar-global a[href*="/dashboard/index?did=1"]');
        })

        .then(function () {
          // check links existence
          test.assertExists('.navbar-global a[href*="/dashboard/index?did=1"]');
          test.assertSelectorContains('.navbar-global a[href*="/dashboard/index?did=1"]', 'First Global Dashboard');
          test.assertExists('.navbar-global a[href*="/dashboard/index?did=2"]');
          test.assertSelectorContains('.navbar-global a[href*="/dashboard/index?did=2"]', 'Second Global Dashboard');
          test.assertExists('.navbar-global a[href*="/dashboard/index?did=3"]');
          test.assertSelectorContains('.navbar-global a[href*="/dashboard/index?did=3"]', 'Third Global Dashboard');
        })

        .then(function () {
          // check that dashboards are not visible by default
          test.assertNotVisible('.navbar-global a[href*="/dashboard/index?did=1"]');
          test.assertNotVisible('.navbar-global a[href*="/dashboard/index?did=2"]');
          test.assertNotVisible('.navbar-global a[href*="/dashboard/index?did=3"]');

          // check dropdown
          casper.click('.navbar-global .t-dashboards [data-toggle="dropdown"]');
          test.assertVisible('.navbar-global a[href*="/dashboard/index?did=1"]');
          test.assertVisible('.navbar-global a[href*="/dashboard/index?did=2"]');
          test.assertVisible('.navbar-global a[href*="/dashboard/index?did=3"]');
        });
  });

  it('should show global plugin pages', 12, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/navigation/global', 'global.json');
        })

        .then(function () {
          casper.evaluate(function () {
            window.SS.isUserAdmin = false;
            require(['apps/nav/app'], function (App) {
              App.start();
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.navbar-global a[href*="/page/1"]');
        })

        .then(function () {
          // check links existence
          test.assertExists('.navbar-global a[href*="/page/1"]');
          test.assertSelectorContains('.navbar-global a[href*="/page/1"]', 'First Global Page');
          test.assertExists('.navbar-global a[href*="/page/2"]');
          test.assertSelectorContains('.navbar-global a[href*="/page/2"]', 'Second Global Page');
          test.assertExists('.navbar-global a[href*="/page/3"]');
          test.assertSelectorContains('.navbar-global a[href*="/page/3"]', 'Third Global Page');
        })

        .then(function () {
          // check that pages are not visible by default
          test.assertNotVisible('.navbar-global a[href*="/page/1"]');
          test.assertNotVisible('.navbar-global a[href*="/page/2"]');
          test.assertNotVisible('.navbar-global a[href*="/page/3"]');

          // check dropdown
          casper.click('.navbar-global .t-more [data-toggle="dropdown"]');
          test.assertVisible('.navbar-global a[href*="/page/1"]');
          test.assertVisible('.navbar-global a[href*="/page/2"]');
          test.assertVisible('.navbar-global a[href*="/page/3"]');
        });
  });

  it('should show login form', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base#anchor'), function () {
          lib.setDefaultViewport();
        })

        .then(function () {
          casper.evaluate(function () {
            window.SS.isUserAdmin = false;
            require(['apps/nav/app'], function (App) {
              App.start();
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.navbar-global .nav');
        })

        .then(function () {
          test.assertExists('.navbar-global .js-login');
          casper.click('.navbar-global .js-login');
          casper.waitForUrl('/sessions/new?return_to=', function () {
            test.assertUrlMatches('/pages/base');
            test.assertUrlMatches('#anchor');
          });
        });
  });

  it('should search', 24, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/navigation/global', 'global.json');
          lib.fmock('/api/components/suggestions', 'search.json');
          lib.fmock('/api/favourites', 'favorite.json');
        })

        .then(function () {
          casper.evaluate(function () {
            window.SS.user = 'user';
            window.SS.isUserAdmin = false;
            window.localStorage.setItem('sonar_recent_history',
                '[{"key":"localhistoryproject","name":"Local History Project","icon":"trk"}]');
            require(['apps/nav/app'], function (App) {
              App.start();
            });
          });
        })

        .then(function () {
          casper.waitForSelector('.navbar-global a[href*="/page/1"]');
        })

        .then(function () {
          test.assertExists('.navbar-global .js-search-dropdown-toggle');
          casper.click('.navbar-global .js-search-dropdown-toggle');
          casper.waitForSelector('.navbar-search');
        })

        .then(function () {
          // for top-level qualifiers
          test.assertExists('.js-search-results a[href*="/all_projects?qualifier=TRK"]');
          test.assertExists('.js-search-results a[href*="/all_projects?qualifier=VW"]');
          test.assertExists('.js-search-results a[href*="/all_projects?qualifier=DEV"]');

          // browsed recently
          test.assertExists('.js-search-results a[href*="localhistoryproject"]');
          test.assertSelectorContains('.js-search-results a[href*="localhistoryproject"]', 'Local History Project');

          // favorite
          test.assertExists('.js-search-results a[href*="favorite-project-key"]');
          test.assertSelectorContains('.js-search-results a[href*="favorite-project-key"]', 'Favorite Project');
          test.assertExists('.js-search-results a[href*="favorite-file-key"]');
          test.assertSelectorContains('.js-search-results a[href*="favorite-file-key"]', 'FavoriteFile.java');
        })

        .then(function () {
          casper.evaluate(function () {
            jQuery('.navbar-search [name="q"]').val('quality').keyup();
          });
          casper.evaluate(function () {
            jQuery('.navbar-search [name="q"]').keyup();
          });
          casper.waitForSelectorTextChange('.js-search-results');
        })

        .then(function () {
          test.assertElementCount('.js-search-results a', 7);
          test.assertExists('.js-search-results a[href*="/profiles"]');
          test.assertExists('.js-search-results a[href*="/quality_gates"]');
          test.assertExists('.js-search-results a[href*="/dashboard/index?did=50"]');
          test.assertExists('.js-search-results a[href*="quality-project"]');
          test.assertSelectorContains('.js-search-results a', 'SonarQube Java');
          test.assertSelectorContains('.js-search-results a', 'SonarQube Java :: Squid');
          test.assertSelectorContains('.js-search-results a', 'SonarQube Java :: Checks');
        })

        .then(function () {
          // should reset search results
          casper.evaluate(function () {
            jQuery('.navbar-search [name="q"]').val('').keyup();
          });
          casper.waitForSelectorTextChange('.js-search-results');
        })

        .then(function () {
          test.assertSelectorDoesntContain('.js-search-results a', 'SonarQube Java');
          test.assertExists('.js-search-results a[href*="/all_projects?qualifier=TRK"]');
          test.assertExists('.js-search-results a[href*="/all_projects?qualifier=VW"]');
          test.assertExists('.js-search-results a[href*="/all_projects?qualifier=DEV"]');

          test.assertExists('.js-search-results a[href*="localhistoryproject"]');
          test.assertSelectorContains('.js-search-results a[href*="localhistoryproject"]', 'Local History Project');
        });
  });

  it('should show context bar', 3, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/navigation/component', 'component.json',
              { data: { componentKey: 'org.codehaus.sonar-plugins.java:java' } });
        })

        .then(function () {
          casper.evaluate(function () {
            window.SS.isUserAdmin = false;
            require(['apps/nav/app'], function (App) {
              App.start({ space: 'component', componentKey: 'org.codehaus.sonar-plugins.java:java' });
            });
          });
        })

        .then(function () {
          casper.waitForText('SonarQube Java');
        })

        .then(function () {
          test.assertSelectorContains('.navbar-context .nav-crumbs', 'SonarQube Java');
          test.assertSelectorContains('.navbar-context .navbar-context-meta', '2.9-SNAPSHOT');
          test.assertSelectorContains('.navbar-context .navbar-context-meta', '2015');
        });
  });

  it('should favorite component', 1, function (casper, test) {
    return casper
        .start(lib.buildUrl('base'), function () {
          lib.setDefaultViewport();
          lib.fmock('/api/navigation/component', 'component.json',
              { data: { componentKey: 'org.codehaus.sonar-plugins.java:java' } });
          lib.smock('/api/favourites?key=org.codehaus.sonar-plugins.java%3Ajava', '{}', { data: { type: 'POST' } });
          lib.smock('/api/favourites/org.codehaus.sonar-plugins.java%3Ajava', '{}', { data: { type: 'DELETE' } });
        })

        .then(function () {
          casper.evaluate(function () {
            window.SS.isUserAdmin = false;
            require(['apps/nav/app'], function (App) {
              App.start({ space: 'component', componentKey: 'org.codehaus.sonar-plugins.java:java' });
            });
          });
        })

        .then(function () {
          casper.waitForText('SonarQube Java');
        })

        .then(function () {
          lib.capture();
          test.assertExists('.js-favorite.icon-not-favorite');
          casper.click('.js-favorite');
          casper.waitForSelector('.js-favorite.icon-favorite');
        })

        .then(function () {
          casper.click('.js-favorite');
          casper.waitForSelector('.js-favorite.icon-not-favorite');
        });
  });

});
