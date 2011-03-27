= JRuby-OpenSSL

* http://jruby-extras.rubyforge.org/jruby-openssl

== DESCRIPTION:

JRuby-OpenSSL is an add-on gem for JRuby that emulates the Ruby OpenSSL native library.

JRuby offers *just enough* compatibility for most Ruby applications that use OpenSSL. 

Libraries that appear to work fine:

    Rails, Net::HTTPS

Notable libraries that do *not* yet work include:

    Net::SSH, Net::SFTP, etc.

Please report bugs and incompatibilities (preferably with testcases) to either the JRuby 
mailing list [1] or the JRuby bug tracker [2].

[1]: http://xircles.codehaus.org/projects/jruby/lists

[2]: http://jira.codehaus.org/browse/JRUBY