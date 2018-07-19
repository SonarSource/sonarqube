# sonar-docs

## Formatting

## Conditional Content

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
