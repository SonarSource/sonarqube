---
title: Supporting New Languages
url: /extend/new-languages/
---


The steps to cover a new programming language are:

1. Write the grammar. This is the hardest part.
1. Write a parser (a parser simply parses an input based on your grammar to yield a parse tree).
1. Test your grammar, to ensure it is able to parse real-life language files.
1. Write a few parse tree visitors. Some visitors will compute metrics such as [executable lines](/extend/executable-lines/), while others will enforce [coding rules](/extend/adding-coding-rules/). A dozen or so visitors is sufficient for an initial release.
1. Write a scanner Sensor, in a SonarQube plugin, to launch the visitors. 
1. Compute
   1. issues
   1. raw measures
   1. code duplications
   1. syntax highlighting
   1. symbol table
   1. coverage information (lines/branches to cover, line/branch hits)
   
In fulfilling these steps, the [SonarSource Language Recognizer (SSLR)](https://github.com/SonarSource/sslr) can be an important resource.
   

 

