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

To start the SonarCloud Embedded docs dev server on port 3000:
```
cd sonar-enterprise/server/sonar-web
INSTANCE=SonarCloud yarn start
```

To start the Static docs dev server on port 8000:
```
cd sonar-enterprise/server/sonar-docs
yarn develop
```

## Writing docs
### Header
Each documentation file should contain a header at the top of the file delimited by "---" top and bottom. The header holds file metadata:
* The `title` tag defines the title of the page for the index
* The `order` tag defines the order of the page for the index. (Floats are interpreted correctly)
* The `scope` tag defines to which product the doc applies. Omit `scope` to have a file show up everywhere:
  * “sonarqube” - visible only for SonarQube and the static website
  * “sonarcloud” - visible only for SonarCloud
  * "static" - visible only on the static website

Ex.:
```
---
title: Demo page
order: 0
scope: static
---
```
Metadata tags can appear in any order, but by convention, `title` should come first.

### Includes
Basic syntax: `@include tooltips/quality-gates/quality-gate`
* path omits trailing '.md'
* path starts from 'src', regardless of where the including page is.

### Conditional Content

With special comments you can mark a page or a part of the content to be displayed only on SonarCloud, SonarQube or the static documentation website.

To display a page only in a certain context use the frontmatter option:

```md
---
scope: sonarcloud (or sonarqube, or static)
---
```

To display/hide a part of the content use special comments:

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

You can also use inline comments:

```md
this content is displayed on <!-- sonarcloud -->SonarCloud<!-- /sonarcloud --><!-- sonarqube -->SonarQube<!-- /sonarqube -->
```

### Page-level ToC
Basic syntax: `## Table of Contents`
Lists all h2 & h3  
The resulting table of contents will also list all h1 items, but h1 is used for the page title, and by convention should not also be used further down the page.

### Formatting
#### Links
* External page (automatically open in a new tab): `[Link text](https://www.sonarsource.com/)`
* Another documentation page: `[Link text](/short-lived-branches)`
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
```[[collapse]]
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
