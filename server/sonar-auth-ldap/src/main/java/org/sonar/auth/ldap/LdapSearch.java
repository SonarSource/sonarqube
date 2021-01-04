/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth.ldap;

import java.util.Arrays;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.PartialResultException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * Fluent API for building LDAP queries.
 *
 * @author Evgeny Mandrikov
 */
public class LdapSearch {

  private static final Logger LOG = Loggers.get(LdapSearch.class);

  private final LdapContextFactory contextFactory;

  private String baseDn;
  private int scope = SearchControls.SUBTREE_SCOPE;
  private String request;
  private String[] parameters;
  private String[] returningAttributes;

  public LdapSearch(LdapContextFactory contextFactory) {
    this.contextFactory = contextFactory;
  }

  /**
   * Sets BaseDN.
   */
  public LdapSearch setBaseDn(String baseDn) {
    this.baseDn = baseDn;
    return this;
  }

  public String getBaseDn() {
    return baseDn;
  }

  /**
   * Sets the search scope.
   *
   * @see SearchControls#ONELEVEL_SCOPE
   * @see SearchControls#SUBTREE_SCOPE
   * @see SearchControls#OBJECT_SCOPE
   */
  public LdapSearch setScope(int scope) {
    this.scope = scope;
    return this;
  }

  public int getScope() {
    return scope;
  }

  /**
   * Sets request.
   */
  public LdapSearch setRequest(String request) {
    this.request = request;
    return this;
  }

  public String getRequest() {
    return request;
  }

  /**
   * Sets search parameters.
   */
  public LdapSearch setParameters(String... parameters) {
    this.parameters = parameters;
    return this;
  }

  public String[] getParameters() {
    return parameters;
  }

  /**
   * Sets attributes, which should be returned by search.
   */
  public LdapSearch returns(String... attributes) {
    this.returningAttributes = attributes;
    return this;
  }

  public String[] getReturningAttributes() {
    return returningAttributes;
  }

  /**
   * @throws NamingException if unable to perform search
   */
  public NamingEnumeration<SearchResult> find() throws NamingException {
    LOG.debug("Search: {}", this);
    NamingEnumeration<SearchResult> result;
    InitialDirContext context = null;
    boolean threw = false;
    try {
      context = contextFactory.createBindContext();
      SearchControls controls = new SearchControls();
      controls.setSearchScope(scope);
      controls.setReturningAttributes(returningAttributes);
      result = context.search(baseDn, request, parameters, controls);
      threw = true;
    } finally {
      ContextHelper.close(context, threw);
    }
    return result;
  }

  /**
   * @return result, or null if not found
   * @throws NamingException if unable to perform search, or non unique result
   */
  public SearchResult findUnique() throws NamingException {
    NamingEnumeration<SearchResult> result = find();
    if (hasMore(result)) {
      SearchResult obj = result.next();
      if (!hasMore(result)) {
        return obj;
      }
      throw new NamingException("Non unique result for " + toString());
    }
    return null;
  }

  private static boolean hasMore(NamingEnumeration<SearchResult> result) throws NamingException {
    try {
      return result.hasMore();
    } catch (PartialResultException e) {
      LOG.debug("More result might be forthcoming if the referral is followed", e);
      // See LDAP-62 and http://docs.oracle.com/javase/jndi/tutorial/ldap/referral/jndi.html :
      // When the LDAP service provider receives a referral despite your having set Context.REFERRAL to "ignore", it will throw a
      // PartialResultException(in the API reference documentation) to indicate that more results might be forthcoming if the referral is
      // followed. In this case, the server does not support the Manage Referral control and is supporting referral updates in some other
      // way.
      return false;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
      "baseDn=" + baseDn +
      ", scope=" + scopeToString() +
      ", request=" + request +
      ", parameters=" + Arrays.toString(parameters) +
      ", attributes=" + Arrays.toString(returningAttributes) +
      "}";
  }

  private String scopeToString() {
    switch (scope) {
      case SearchControls.ONELEVEL_SCOPE:
        return "onelevel";
      case SearchControls.OBJECT_SCOPE:
        return "object";
      case SearchControls.SUBTREE_SCOPE:
      default:
        return "subtree";
    }
  }

}
