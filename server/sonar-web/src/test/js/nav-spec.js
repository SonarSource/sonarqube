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

        lib.mockRequestFromFile('/api/navigation/global', 'global.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          require(['apps/nav/app']);
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

        lib.mockRequestFromFile('/api/navigation/global', 'global.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();

          require(['apps/nav/app']);
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

        lib.mockRequestFromFile('/api/navigation/global', 'global.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();

          require(['apps/nav/app']);
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

          require(['apps/nav/app']);
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


casper.test.begin(testName('Search'), 24, function (test) {
  casper
      .start(lib.buildUrl('nav'), function () {
        lib.setDefaultViewport();

        lib.mockRequestFromFile('/api/navigation/global', 'global.json');
        lib.mockRequestFromFile('/api/components/suggestions', 'search.json');
        lib.mockRequestFromFile('/api/favourites', 'favorite.json');
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.user = 'user';
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          window.localStorage.setItem('sonar_recent_history',
              '[{"key":"localhistoryproject","name":"Local History Project","icon":"trk"}]');

          require(['apps/nav/app']);
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

        lib.mockRequestFromFile('/api/navigation/component', 'component.json',
            { data: { componentKey: 'org.codehaus.sonar-plugins.java:java' } });
      })

      .then(function () {
        casper.evaluate(function () {
          window.SS.isUserAdmin = false;
          window.navbarOptions = new Backbone.Model();
          window.navbarOptions.set({ space: 'component', componentKey: 'org.codehaus.sonar-plugins.java:java' });

          require(['apps/nav/app']);
        });
      })

      .then(function () {
        casper.waitForText('SonarQube Java');
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
