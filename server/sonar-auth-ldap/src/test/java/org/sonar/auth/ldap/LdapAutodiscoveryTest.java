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

import java.net.UnknownHostException;
import java.util.Arrays;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.auth.ldap.LdapAutodiscovery.LdapSrvRecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LdapAutodiscoveryTest {

  @Test
  public void testGetDnsDomain() {
    assertThat(LdapAutodiscovery.getDnsDomainName("localhost")).isNull();
    assertThat(LdapAutodiscovery.getDnsDomainName("godin.example.org")).isEqualTo("example.org");
    assertThat(LdapAutodiscovery.getDnsDomainName("godin.usr.example.org")).isEqualTo("usr.example.org");
  }

  @Test
  public void testGetDnsDomainWithoutParameter() {
    try {
      LdapAutodiscovery.getDnsDomainName();
    } catch (UnknownHostException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetDnsDomainDn() {
    assertThat(LdapAutodiscovery.getDnsDomainDn("example.org")).isEqualTo("dc=example,dc=org");
  }

  @Test
  public void testEqualsAndHashCode() {
    assertThat(new LdapSrvRecord("http://foo:389", 1, 1)).isEqualTo(new LdapSrvRecord("http://foo:389", 2, 0));
    assertThat(new LdapSrvRecord("http://foo:389", 1, 1)).isNotEqualTo(new LdapSrvRecord("http://foo:388", 1, 1));

    assertThat(new LdapSrvRecord("http://foo:389", 1, 1)).hasSameHashCodeAs(new LdapSrvRecord("http://foo:389", 1, 1).hashCode());
  }

  @Test
  public void testGetLdapServer() throws NamingException {
    DirContext context = mock(DirContext.class);
    Attributes attributes = mock(Attributes.class);
    Attribute attribute = mock(Attribute.class);
    NamingEnumeration namingEnumeration = mock(NamingEnumeration.class);

    when(context.getAttributes(Mockito.anyString(), Mockito.anyObject())).thenReturn(attributes);
    when(attributes.get("srv")).thenReturn(attribute);
    when(attribute.getAll()).thenReturn(namingEnumeration);
    when(namingEnumeration.hasMore()).thenReturn(true, true, true, true, true, false);
    when(namingEnumeration.next())
      .thenReturn("10 40 389 ldap5.example.org.")
      .thenReturn("0 10 389 ldap3.example.org")
      .thenReturn("0 60 389 ldap1.example.org")
      .thenReturn("0 30 389 ldap2.example.org")
      .thenReturn("10 60 389 ldap4.example.org");

    assertThat(new LdapAutodiscovery().getLdapServers(context, "example.org.")).extracting("serverUrl")
      .isEqualTo(
        Arrays.asList("ldap://ldap1.example.org:389", "ldap://ldap2.example.org:389", "ldap://ldap3.example.org:389", "ldap://ldap4.example.org:389",
          "ldap://ldap5.example.org:389"));
  }

}
