/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
/* globals casper: false */

var lib = require('../lib'),
    testName = lib.testName('Navigation');


lib.initMessages();
lib.changeWorkingDirectory('nav-spec');
lib.configureCasper();


casper.test.begin(testName('Global Spaces'), 8, function (test) {
  casper
      .start(lib.buildUrl('nav'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          require(['/js/nav/app.js']);
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
        test.assertExists('.navbar-global a[href*="/dependencies"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Global Dashboards'), 12, function (test) {
  casper
      .start(lib.buildUrl('nav'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          window.navbarOptions.set({
            globalDashboards: [
              { url: '/dashboard/?did=1', name: 'First Global Dashboard' },
              { url: '/dashboard/?did=2', name: 'Second Global Dashboard' },
              { url: '/dashboard/?did=3', name: 'Third Global Dashboard' }
            ]
          });

          require(['/js/nav/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.navbar-global .nav');
      })

      .then(function () {
        // check links existence
        test.assertExists('.navbar-global a[href*="/dashboard/?did=1"]');
        test.assertSelectorContains('.navbar-global a[href*="/dashboard/?did=1"]', 'First Global Dashboard');
        test.assertExists('.navbar-global a[href*="/dashboard/?did=2"]');
        test.assertSelectorContains('.navbar-global a[href*="/dashboard/?did=2"]', 'Second Global Dashboard');
        test.assertExists('.navbar-global a[href*="/dashboard/?did=3"]');
        test.assertSelectorContains('.navbar-global a[href*="/dashboard/?did=3"]', 'Third Global Dashboard');
      })

      .then(function () {
        // check that dashboards are not visible by default
        test.assertNotVisible('.navbar-global a[href*="/dashboard/?did=1"]');
        test.assertNotVisible('.navbar-global a[href*="/dashboard/?did=2"]');
        test.assertNotVisible('.navbar-global a[href*="/dashboard/?did=3"]');

        // check dropdown
        casper.click('.navbar-global .t-dashboards [data-toggle="dropdown"]');
        test.assertVisible('.navbar-global a[href*="/dashboard/?did=1"]');
        test.assertVisible('.navbar-global a[href*="/dashboard/?did=2"]');
        test.assertVisible('.navbar-global a[href*="/dashboard/?did=3"]');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Global Plugin Pages'), 12, function (test) {
  casper
      .start(lib.buildUrl('nav'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          window.navbarOptions.set({
            globalPages: [
              { url: '/page/1', name: 'First Global Page' },
              { url: '/page/2', name: 'Second Global Page' },
              { url: '/page/3', name: 'Third Global Page' }
            ]
          });

          require(['/js/nav/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.navbar-global .nav');
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
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Login'), 3, function (test) {
  casper
      .start(lib.buildUrl('nav#anchor'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();

          require(['/js/nav/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.navbar-global .nav');
      })

      .then(function () {
        test.assertExists('.navbar-global .js-login');
        casper.click('.navbar-global .js-login');
        casper.waitForUrl('/sessions/new?return_to=', function () {
          test.assertUrlMatches('/pages/nav');
          test.assertUrlMatches('#anchor');
        });
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Search'), 23, function (test) {
  casper
      .start(lib.buildUrl('nav'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/components/suggestions', 'search.json');
        lib.mockRequestFromFile('/api/favourites', 'favorite.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.user = 'user';
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          window.navbarOptions.set({ qualifiers: ['TRK', 'VW', 'DEV'] });
          window.navbarOptions.set({ globalDashboards: [{ name: 'Quality', url: '/dashboard/?did=50' }] });
          window.localStorage.setItem('sonar_recent_history',
              '[{"key":"localhistoryproject","name":"Local History Project","icon":"trk"}]');

          require(['/js/nav/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.navbar-global .nav');
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
        test.assertElementCount('.js-search-results a', 6);
        test.assertExists('.js-search-results a[href*="/profiles"]');
        test.assertExists('.js-search-results a[href*="/quality_gates"]');
        test.assertExists('.js-search-results a[href*="/dashboard/?did=50"]');
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
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});


casper.test.begin(testName('Context'), function (test) {
  casper
      .start(lib.buildUrl('nav'), function () {
        lib.setDefaultViewport();
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          window.navbarOptions.set({
            contextId: '1',
            contextKey: 'org.codehaus.sonar-plugins.java:java',
            contextUuid: 'acfc6a2d-d28c-4302-987c-697544fb096e',
            contextComparable: true,
            canFavoriteContext: false,
            isContextFavorite: false,
            contextVersion: '2.9-SNAPSHOT',
            contextDate: '2015-03-03T09:43:37+01:00'
          });
          window.navbarOptions.set({
            contextBreadcrumbs: [
              {
                q: 'TRK',
                url: '/dashboard/index?id=org.codehaus.sonar-plugins.java%3Ajava',
                name: 'SonarQube Java'
              }
            ]
          });

          require(['/js/nav/app.js']);
        });
      })

      .then(function () {
        casper.waitForSelector('.navbar-context .nav');
      })

      .then(function () {
        test.assertSelectorContains('.navbar-context .nav-crumbs', 'SonarQube Java');
        test.assertSelectorContains('.navbar-context .navbar-context-meta', '2.9-SNAPSHOT');
        test.assertSelectorContains('.navbar-context .navbar-context-meta', '2015');
      })

      .then(function () {
        lib.sendCoverage();
      })

      .run(function () {
        test.done();
      });
});
