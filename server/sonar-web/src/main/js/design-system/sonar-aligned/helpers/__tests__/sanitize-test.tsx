/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import { render, screen } from '@testing-library/react';
import {
  SafeHTMLInjection,
  sanitizeHTMLNoSVGNoMathML,
  sanitizeHTMLRestricted,
  sanitizeHTMLToPreventCSSInjection,
  sanitizeHTMLUserInput,
} from '../sanitize';

/*
 * Test code borrowed from OWASP's sanitizer tests
 * https://github.com/OWASP/java-html-sanitizer/blob/master/src/test/resources/org/owasp/html/htmllexerinput1.html
 */
const tainted = `<?xml version="not-even-close"?>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<!-- a test input for HtmlLexer -->

<html lang="en" xml:lang="en">
<head>
<title>Test File For HtmlLexer &amp; HtmlParser</title>
<link rel=stylesheet type="text/css" src=foo/bar.css />
<body
 bgcolor=white
 linkcolor = "blue"
 onload="document.writeln(
  &quot;&lt;p&gt;properly escaped code in a handler&lt;/p&gt;&quot;);"
>

<script type="text/javascript"><!--
document.writeln("<p>Some initialization code in global context</p>");
--></script>

<script type="text/javascript">
// hi there
document.writeln("<p>More initialization</p>");
</script>

<div id=clickydiv onclick="handleClicky(event)"
 ondblclick=this.onclick(event);return(false)>
Clicky
</div>

<input id=foo>
<gxp:attr name="onchange">alert("&lt;b&gt;hi&lt;/b&gt;");</gxp:attr>
</input>

<pre>&lt;div id=notarealtag onclick=notcode()&gt;</pre>

<!-- some tokenization corner cases -->

< notatag <atag/>

</ notatag> </redundantlyclosed/>

<messyattributes a=b=c d="e"f=g h =i j= k l = m checked n="o"/>

< < < all in one text block > > >

<xmp>Make sure that <!-- comments don't obscure the xmp close</xmp>
<% # some php code here
write("<pre>$horriblySyntacticConstruct1</pre>\n\n");
%>
<script type="text/javascript"><!--
alert("hello world");
// --></script>

<script>/* </script> */alert('hi');</script>
<script><!--/* </script> */alert('hi');--></script>

<xmp style=color:blue><!--/* </xmp> */alert('hi');--></xmp>

<style><!-- p { contentf: '</style>' } --></style>
<style>Foo<!-- > </style> --></style>
<textarea><!-- Zoicks </textarea>--></textarea>
<!-- An escaping text span start may share its U+002D HYPHEN-MINUS characters
   - with its corresponding escaping text span end. -->
<script><!--></script>
<script><!---></script>
<script><!----></script>

This is <b>bold</b> and this is <i>italic</i> and this is <u>underlined</u>.
<br />
A <blockquote>quote</blockquote> and a <code>code</code> and a <pre>pre</pre>.
An <h1>h1</h1> and an <h2>h2</h2> and an <h3>h3</h3> and an <h4>h4</h4> and an <h5>h5</h5> and an <h6>h6</h6>.
An <ol><li>ol</li></ol> and a <ul><li>ul</li></ul> and a <p style="color:blue">p</p>.
A <strong>strong</strong> and a <a href="foo" ping="pong" rel="noopener" target="__blank" >link</a>

<a href="javascript:alert('hello')" target="_blank">this is wrong</a>

<svg><text>SVG isn't always allowed</text></svg>

<math xmlns="http://www.w3.org/1998/Math/MathML">
  <infinity />
</math>

</body>
</html>
<![CDATA[ No such thing as a CDATA> section in HTML ]]>
<script>a<b</script>
<img src=foo.gif /><a href=><a href=/>
<span title=malformed attribs' do=don't id=foo checked onclick="a<b">Bar</span>`;

describe('sanitizeHTMLToPreventCSSInjection', () => {
  it('should strip off style attributes', () => {
    const clean = `
    <div id="clickydiv">
    Clicky
    </div>
    <input id="foo">
    alert("&lt;b&gt;hi&lt;/b&gt;");
    <pre>&lt;div id=notarealtag onclick=notcode()&gt;</pre>
    &lt; notatag
    &lt; &lt; &lt; all in one text block &gt; &gt; &gt;
    &lt;% # some php code here
    write("<pre>$horriblySyntacticConstruct1</pre>
    ");
    %&gt;
     */alert('hi');
     */alert('hi');--&gt;
     */alert('hi');--&gt; ' } --&gt;
     --&gt;
    <textarea>&lt;!-- Zoicks </textarea>--&gt;
    This is <b>bold</b> and this is <i>italic</i> and this is <u>underlined</u>.
    <br>
    A <blockquote>quote</blockquote> and a <code>code</code> and a <pre>pre</pre>.
    An <h1>h1</h1> and an <h2>h2</h2> and an <h3>h3</h3> and an <h4>h4</h4> and an <h5>h5</h5> and an <h6>h6</h6>.
    An <ol><li>ol</li></ol> and a <ul><li>ul</li></ul> and a <p>p</p>.
    A <strong>strong</strong> and a <a rel="noopener" href="foo">link</a>
    <a>this is wrong</a>
    <svg><text>SVG isn't always allowed</text></svg>
    <math xmlns="http://www.w3.org/1998/Math/MathML">
    </math>
     section in HTML ]]&gt;
    <img src="foo.gif"><a href=""></a><a href="/">
    <span checked="" id="foo" title="malformed">Bar</span></a>`;

    expect(sanitizeHTMLToPreventCSSInjection(tainted).trimEnd().replace(/\s+/g, ' ')).toBe(
      clean.replace(/\s+/g, ' '),
    );
  });
});

describe('sanitizeHTMLNoSVGNoMathML', () => {
  it('should not allow MathML and SVG', () => {
    const clean = `
    <div id="clickydiv">
    Clicky
    </div>
    <input id="foo">
    alert("&lt;b&gt;hi&lt;/b&gt;");
    <pre>&lt;div id=notarealtag onclick=notcode()&gt;</pre>
    &lt; notatag
    &lt; &lt; &lt; all in one text block &gt; &gt; &gt;
    &lt;% # some php code here
    write("<pre>$horriblySyntacticConstruct1</pre>
    ");
    %&gt;
     */alert('hi');
     */alert('hi');--&gt;
     */alert('hi');--&gt; ' } --&gt;
     --&gt;
    <textarea>&lt;!-- Zoicks </textarea>--&gt;
    This is <b>bold</b> and this is <i>italic</i> and this is <u>underlined</u>.
    <br>
    A <blockquote>quote</blockquote> and a <code>code</code> and a <pre>pre</pre>.
    An <h1>h1</h1> and an <h2>h2</h2> and an <h3>h3</h3> and an <h4>h4</h4> and an <h5>h5</h5> and an <h6>h6</h6>.
    An <ol><li>ol</li></ol> and a <ul><li>ul</li></ul> and a <p>p</p>.
    A <strong>strong</strong> and a <a rel="noopener" href="foo">link</a>
    <a>this is wrong</a>
     section in HTML ]]&gt;
    <img src="foo.gif"><a href=""></a><a href="/">
    <span checked="" id="foo" title="malformed">Bar</span></a>`;

    expect(sanitizeHTMLNoSVGNoMathML(tainted).trimEnd().replace(/\s+/g, ' ')).toBe(
      clean.replace(/\s+/g, ' '),
    );
  });
});

describe('sanitizeHTMLUserInput', () => {
  it('should preserve only specific formatting tags and attributes', () => {
    const clean = `
    Clicky
    alert("&lt;b&gt;hi&lt;/b&gt;");
    <pre>&lt;div id=notarealtag onclick=notcode()&gt;</pre>
    &lt; notatag
    &lt; &lt; &lt; all in one text block &gt; &gt; &gt;
    &lt;% # some php code here
    write("<pre>$horriblySyntacticConstruct1</pre>
    ");
    %&gt;
     */alert('hi');
     */alert('hi');--&gt;
     */alert('hi');--&gt; ' } --&gt;
     --&gt;
    &lt;!-- Zoicks --&gt;
    This is <b>bold</b> and this is <i>italic</i> and this is underlined.
    <br>
    A <blockquote>quote</blockquote> and a <code>code</code> and a <pre>pre</pre>.
    An <h1>h1</h1> and an <h2>h2</h2> and an <h3>h3</h3> and an <h4>h4</h4> and an <h5>h5</h5> and an <h6>h6</h6>.
    An <ol><li>ol</li></ol> and a <ul><li>ul</li></ul> and a <p>p</p>.
    A <strong>strong</strong> and a <a rel="noopener" href="foo">link</a>
    <a>this is wrong</a>
     section in HTML ]]&gt;
    <a href=""></a><a href="/">
    Bar</a>`;

    expect(sanitizeHTMLUserInput(tainted).trimEnd().replace(/\s+/g, ' ')).toBe(
      clean.replace(/\s+/g, ' '),
    );
  });
});

describe('sanitizeHTMLRestricted', () => {
  it('should preserve only a very limited list of formatting tags and attributes', () => {
    const clean = `
    Clicky
    alert("&lt;b&gt;hi&lt;/b&gt;");
    &lt;div id=notarealtag onclick=notcode()&gt;
    &lt; notatag
    &lt; &lt; &lt; all in one text block &gt; &gt; &gt;
    &lt;% # some php code here
    write("$horriblySyntacticConstruct1
    ");
    %&gt;
     */alert('hi');
     */alert('hi');--&gt;
     */alert('hi');--&gt; ' } --&gt;
     --&gt;
    &lt;!-- Zoicks --&gt;
    This is <b>bold</b> and this is <i>italic</i> and this is underlined.
    <br>
    A quote and a <code>code</code> and a pre.
    An h1 and an h2 and an h3 and an h4 and an h5 and an h6.
    An <li>ol</li> and a <ul><li>ul</li></ul> and a <p>p</p>.
    A <strong>strong</strong> and a <a href="foo">link</a>
    <a>this is wrong</a>
     section in HTML ]]&gt;
    <a href=""></a><a href="/">
    Bar</a>`;

    expect(sanitizeHTMLRestricted(tainted).trimEnd().replace(/\s+/g, ' ')).toBe(
      clean.replace(/\s+/g, ' '),
    );
  });
});

describe('SafeHTMLInjection', () => {
  it('should default to a span and the SanitizeLevel.FORBID_STYLE level', () => {
    const tainted = `
    <head>
      <link rel=stylesheet type="text/css" src=foo/bar.css />
      <style>some style</style>
    </head>
  
    <body>
      <p style="color:blue">a stylish paragraph</p>
      
      <svg><text>SVG isn't always allowed</text></svg>

      <math xmlns="http://www.w3.org/1998/Math/MathML">
        <infinity />
      </math>
    </body>
    `;

    render(<SafeHTMLInjection htmlAsString={tainted} />);

    expect(screen.getByText('a stylish paragraph')).toBeInTheDocument();
    expect(screen.getByText("SVG isn't always allowed")).toBeInTheDocument();
  });
});
