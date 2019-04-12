# sonar-docs

### General

The documentation content lives in `sonar-enterprise/server/sonar-docs`  
We use an augmented GitHub markdown syntax:

* general: https://guides.github.com/features/mastering-markdown/
* general: https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet
* linebreaks: https://gist.github.com/shaunlebron/746476e6e7a4d698b373

## The first time

* Install nodejs, v8.11.3 not 10, which is the latest.
* Install https://yarnpkg.com/en
* Add to your .bashrc:
  * `export ARTIFACTORY_PRIVATE_USERNAME=...`
  * `export ARTIFACTORY_PRIVATE_PASSWORD=...`
* Open a new shell or execute those exports in your current session  
  You can validate this step by executing:
  * `echo $ARTIFACTORY_PRIVATE_USERNAME`
  * `echo $ARTIFACTORY_PRIVATE_PASSWORD`
* Run the following to set up the dev servers:

```
cd sonar-enterprise/server/sonar-web
yarn
cd ../sonar-docs
yarn
```

## Each time

Rebuild and start your SonarQube server (yarn needs this running)

```
cd sonar-enterprise/
./build.sh -x test -x obfuscate
./start.sh
```

To start the SonarQube Embedded docs dev server on port 3000:

```
cd sonar-enterprise/server/sonar-web
yarn start
```

To start the SonarCloud Embedded docs dev server on port 3001:

```
cd sonar-enterprise/server/sonar-web
INSTANCE=SonarCloud PORT=3001 yarn start
```

_You can have both SonarQube and SonarCloud embedded doc dev server running in parallel when you start them on different ports._

To start the Static docs dev server on port 8000:

```
cd sonar-enterprise/server/sonar-docs
yarn develop
```

## Testing
As documentation writers there are two ways it is possible for us to break the SonarQube build
* malformed markup
* broken links

Even without spinning up servers, you can double-check that your changes won't break the build.

**Test everything**  
You can run all the tests, and make sure that both your markup is well-formed and your links are correct by running the build script:
```
cd sonar-enterprise/
./build.sh -x test -x obfuscate
```
**Test links only**  
If you only want to double-check your links changes, you can
```
cd sonar-enterprise/server/sonar-docs
yarn jest
```

This will run the broken link test and stop at the first broken link it finds. Continue running this iteratively until it passes.

## Committing
**Always start your commit message with "DOCS".**

The convention is to start commit messages with the ticket number the changes are for. Since docs changes are often made without tickets, use "DOCS" instead.

## Navigation trees
Controlling the navigation trees of the tree instances is covered in [static/README.md](static)


## Writing docs

### URLs
All urls _must_ end with a trailing slash (`/`).

### Header

Each documentation file should contain a header at the top of the file delimited by "---" top and bottom. The header holds file metadata:

* The `title` tag defines the title of the page.
* The `url` tag is required and defines the path at which to publish the page. Reminder: end this with a trailing slash.
* The `nav` tag is optional and controls the title of the page inside the navigation tree.

Ex.:

```
---
title: Demo page
nav: Demo
url: /sonarcloud-pricing/
---
```

** Metadata conventions**
* Metadata tags can appear in any order, but by convention, `title` should come first.
* The `url` tag is required, and should start and end with '/'


### Includes

Basic syntax: `@include tooltips/quality-gates/quality-gate`

* path omits trailing '.md'
* path starts from 'src', regardless of where the including page is.

### Conditional Content

With special comments you can mark a page or a part of the content to be displayed only on SonarCloud, SonarQube or the static documentation website.

To drop in "SonarQube" or "SonarCloud" as appropriate, use:
```
{instance}
```

To display/hide some other part of the content, use special comments:

```md
<!-- sonarcloud -->

this content is displayed only on SonarCloud

<!-- /sonarcloud -->

<!-- sonarqube -->

this content is displayed in SonarQube and in the static website

<!-- /sonarqube -->

<!-- static -->

this content is displayed only in the static website

<!-- /static -->
```

You can also use these comments inline:

```md
this content is displayed on <!-- sonarcloud -->SonarCloud<!-- /sonarcloud --><!-- sonarqube -->SonarQube<!-- /sonarqube -->
```

### Page-level ToC

All h2 tags will automatically be part of the TOC both in the static and embed documentation.
The resulting table of contents will also list all h1 items, but h1 is used for the page title, and by convention should not also be used further down the page.

### Formatting

#### Links

* External page (automatically opens in a new tab): `[Link text](https://www.sonarsource.com/)`
* Another documentation page: `[Link text](/short-lived-branches/)`
  * path omits trailing '.md'
  * links inside tooltips always open in a new tab
* Internal SonarCloud app page: `[Link text](/#sonarcloud#/onboarding)`

  * it is possible to reference app pages only inside SonarCloud documentation page (`scope: sonarcloud`), because these links are not valid on the static documentation

#### Smart Links

Use this syntax to conditionally link from the embedded docs to pages within the SonarQube application. Specifically, in the static website links will be suppressed, but the link text will be shown. In the embedded documentation, administrative links will only be linked for administrative users.

* Internal SonarQube app page: `[Link text](/#sonarqube#/...)`
  * On SonarCloud, only the link text will be displayed, not wrapped into a link tag
* Internal SonarQube app page: `[Link text](/#sonarqube-admin#/...)`

#### Linebreaks

By default, single linebreaks are removed in rendering. I.e.

```
foo
bar
baz
```

Will render as

```
foo bar baz
```

To get a `<br/>` effect, add 2 spaces at the end of the line

```
foo  //<- 2 spaces
bar  //<- 2 spaces
baz  
```

Yields

```
foo
bar
baz
```

#### Collapsible block

Basic syntax:

```
[[collapse]]
| ## Block title
| Block content
```

The first line of the block must be a title. You can have as many lines as you want after that.

#### Images

Basic syntax: `![alt text.](/images/short-lived-branch-concept.png)`

* images are auto-sized to 100% if they're larger than 100%
* paths start from 'src', regardless of where the calling page is

#### Icons

* :warning: `![](/images/exclamation.svg)`
* :information_source: `![](/images/info.svg)`
* :heavy_check_mark: `![](/images/check.svg)`
* :x: `![](/images/cross.svg)`

#### Message box

Basic syntax:

```
[[warning]]
| This is a **warning** message.
```

**There must be a linebreak before the first '|'**

There are four options:

* danger (red)
* warning (yellow)
* success (green)
* info (blue)

You can also put icons inside messages:

```
[[danger]]
| ![](/images/cross.svg) This is a **danger** message.
```
