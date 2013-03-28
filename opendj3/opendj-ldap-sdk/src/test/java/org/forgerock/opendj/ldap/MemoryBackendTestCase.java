/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2013 ForgeRock AS.
 */
package org.forgerock.opendj.ldap;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.opendj.ldap.Connections.newInternalConnection;
import static org.forgerock.opendj.ldap.requests.Requests.newAddRequest;
import static org.forgerock.opendj.ldap.requests.Requests.newDeleteRequest;
import static org.forgerock.opendj.ldap.requests.Requests.newModifyRequest;
import static org.forgerock.opendj.ldap.requests.Requests.newSimpleBindRequest;
import static org.forgerock.opendj.ldif.LDIFEntryReader.valueOfLDIFEntry;

import java.io.IOException;
import java.util.Arrays;

import org.forgerock.opendj.ldap.controls.AssertionRequestControl;
import org.forgerock.opendj.ldap.controls.PermissiveModifyRequestControl;
import org.forgerock.opendj.ldap.controls.PostReadRequestControl;
import org.forgerock.opendj.ldap.controls.PostReadResponseControl;
import org.forgerock.opendj.ldap.controls.PreReadRequestControl;
import org.forgerock.opendj.ldap.controls.PreReadResponseControl;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.opendj.ldif.LDIFEntryReader;
import org.testng.annotations.Test;

/**
 * Memory backend tests.
 */
@SuppressWarnings("javadoc")
public class MemoryBackendTestCase extends SdkTestCase {

    @Test
    public void testAdd() throws Exception {
        final Connection connection = getConnection();
        final Entry newDomain =
                valueOfLDIFEntry("dn: dc=new domain,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: new domain");
        connection.add(newDomain);
        assertThat(connection.readEntry("dc=new domain,dc=com")).isEqualTo(newDomain);
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testAddAlreadyExists() throws Exception {
        final Connection connection = getConnection();
        connection.add(valueOfLDIFEntry("dn: dc=example,dc=com", "objectClass: domain",
                "objectClass: top", "dc: example"));
    }

    @Test(expectedExceptions = EntryNotFoundException.class)
    public void testAddNoParent() throws Exception {
        final Connection connection = getConnection();
        connection.add(valueOfLDIFEntry("dn: dc=new domain,dc=missing,dc=com",
                "objectClass: domain", "objectClass: top", "dc: new domain"));
    }

    @Test
    public void testAddPostRead() throws Exception {
        final Connection connection = getConnection();
        final Entry newDomain =
                valueOfLDIFEntry("dn: dc=new domain,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: new domain");
        assertThat(
                connection.add(
                        newAddRequest(newDomain)
                                .addControl(PostReadRequestControl.newControl(true))).getControl(
                        PostReadResponseControl.DECODER, new DecodeOptions()).getEntry())
                .isEqualTo(newDomain);
    }

    @Test(expectedExceptions = ErrorResultException.class)
    public void testAddPreRead() throws Exception {
        final Connection connection = getConnection();
        final Entry newDomain =
                valueOfLDIFEntry("dn: dc=new domain,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: new domain");
        connection.add(newAddRequest(newDomain).addControl(PreReadRequestControl.newControl(true)));
    }

    @Test
    public void testCompareFalse() throws Exception {
        final Connection connection = getConnection();
        assertThat(connection.compare("dc=example,dc=com", "objectclass", "person").matched())
                .isFalse();
    }

    @Test(expectedExceptions = EntryNotFoundException.class)
    public void testCompareNoSuchObject() throws Exception {
        final Connection connection = getConnection();
        connection.compare("uid=missing,ou=people,dc=example,dc=com", "uid", "missing");
    }

    @Test
    public void testCompareTrue() throws Exception {
        final Connection connection = getConnection();
        assertThat(connection.compare("dc=example,dc=com", "objectclass", "domain").matched())
                .isTrue();
    }

    @Test(expectedExceptions = AssertionFailureException.class)
    public void testDeleteAssertionFalse() throws Exception {
        final Connection connection = getConnection();
        connection.delete(newDeleteRequest("dc=xxx,dc=com").addControl(
                AssertionRequestControl.newControl(true, Filter.valueOf("(objectclass=person)"))));
    }

    @Test
    public void testDeleteAssertionTrue() throws Exception {
        final Connection connection = getConnection();
        connection.delete(newDeleteRequest("dc=xxx,dc=com").addControl(
                AssertionRequestControl.newControl(true, Filter.valueOf("(objectclass=domain)"))));
    }

    @Test(expectedExceptions = EntryNotFoundException.class)
    public void testDeleteNoSuchObject() throws Exception {
        final Connection connection = getConnection();
        connection.delete("uid=missing,ou=people,dc=example,dc=com");
    }

    @Test
    public void testDeleteOnLeaf() throws Exception {
        final Connection connection = getConnection();
        connection.delete("uid=test1,ou=people,dc=example,dc=com");
        try {
            connection.readEntry("dc=example,dc=com");
        } catch (final EntryNotFoundException expected) {
            // Do nothing.
        }
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testDeleteOnNonLeaf() throws Exception {
        final Connection connection = getConnection();
        try {
            connection.delete("dc=example,dc=com");
        } finally {
            assertThat(connection.readEntry("dc=example,dc=com")).isNotNull();
        }
    }

    @Test(expectedExceptions = ErrorResultException.class)
    public void testDeletePostRead() throws Exception {
        final Connection connection = getConnection();
        connection.delete(newDeleteRequest("dc=xxx,dc=com").addControl(
                PostReadRequestControl.newControl(true)));
    }

    @Test
    public void testDeletePreRead() throws Exception {
        final Connection connection = getConnection();
        assertThat(
                connection.delete(
                        newDeleteRequest("dc=xxx,dc=com").addControl(
                                PreReadRequestControl.newControl(true))).getControl(
                        PreReadResponseControl.DECODER, new DecodeOptions()).getEntry()).isEqualTo(
                valueOfLDIFEntry("dn: dc=xxx,dc=com", "objectClass: domain", "objectClass: top",
                        "dc: xxx"));
    }

    @Test
    public void testDeleteSubtree() throws Exception {
        final Connection connection = getConnection();
        connection.deleteSubtree("dc=example,dc=com");
        for (final String name : Arrays.asList("dc=example,dc=com", "ou=people,dc=example,dc=com",
                "uid=test1,ou=people,dc=example,dc=com", "uid=test2,ou=people,dc=example,dc=com")) {
            try {
                connection.readEntry(name);
            } catch (final EntryNotFoundException expected) {
                // Do nothing.
            }
        }
        assertThat(connection.readEntry("dc=xxx,dc=com")).isNotNull();
    }

    @Test
    public void testModify() throws Exception {
        final Connection connection = getConnection();
        connection.modify("dn: dc=example,dc=com", "changetype: modify", "add: description",
                "description: test description");
        assertThat(connection.readEntry("dc=example,dc=com")).isEqualTo(
                valueOfLDIFEntry("dn: dc=example,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: example", "description: test description"));
    }

    @Test(expectedExceptions = AssertionFailureException.class)
    public void testModifyAssertionFalse() throws Exception {
        final Connection connection = getConnection();
        connection.modify(newModifyRequest("dn: dc=example,dc=com", "changetype: modify",
                "add: description", "description: test description").addControl(
                AssertionRequestControl.newControl(true, Filter.valueOf("(objectclass=person)"))));
    }

    @Test
    public void testModifyAssertionTrue() throws Exception {
        final Connection connection = getConnection();
        connection.modify(newModifyRequest("dn: dc=example,dc=com", "changetype: modify",
                "add: description", "description: test description").addControl(
                AssertionRequestControl.newControl(true, Filter.valueOf("(objectclass=domain)"))));
    }

    @Test
    public void testModifyBindPostRead() throws Exception {
        final Connection connection = getConnection();
        assertThat(
                connection.modify(
                        newModifyRequest("dn: dc=example,dc=com", "changetype: modify",
                                "add: description", "description: test description").addControl(
                                PostReadRequestControl.newControl(true))).getControl(
                        PostReadResponseControl.DECODER, new DecodeOptions()).getEntry())
                .isEqualTo(
                        valueOfLDIFEntry("dn: dc=example,dc=com", "objectClass: domain",
                                "objectClass: top", "dc: example", "description: test description"));
    }

    @Test
    public void testModifyIncrement() throws Exception {
        final Connection connection = getConnection();
        connection.modify("dn: dc=example,dc=com", "changetype: modify", "add: integer",
                "integer: 100", "-", "increment: integer", "integer: 10");
        assertThat(connection.readEntry("dc=example,dc=com")).isEqualTo(
                valueOfLDIFEntry("dn: dc=example,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: example", "integer: 110"));
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testModifyIncrementBadDelta() throws Exception {
        final Connection connection = getConnection();
        connection.modify("dn: dc=example,dc=com", "changetype: modify", "add: integer",
                "integer: 100", "-", "increment: integer", "integer: nan");
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testModifyIncrementBadValue() throws Exception {
        final Connection connection = getConnection();
        connection.modify("dn: dc=example,dc=com", "changetype: modify", "add: integer",
                "integer: nan", "-", "increment: integer", "integer: 10");
    }

    @Test(expectedExceptions = EntryNotFoundException.class)
    public void testModifyNoSuchObject() throws Exception {
        final Connection connection = getConnection();
        connection.modify("dn: dc=missing,dc=com", "changetype: modify", "add: description",
                "description: test description");
    }

    @Test
    public void testModifyPermissiveWithDuplicateValues() throws Exception {
        final Connection connection = getConnection();
        connection.modify(newModifyRequest("dn: dc=example,dc=com", "changetype: modify",
                "add: dc", "dc: example").addControl(
                PermissiveModifyRequestControl.newControl(true)));
        assertThat(connection.readEntry("dc=example,dc=com")).isEqualTo(
                valueOfLDIFEntry("dn: dc=example,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: example"));
    }

    @Test
    public void testModifyPermissiveWithMissingValues() throws Exception {
        final Connection connection = getConnection();
        connection.modify(newModifyRequest("dn: dc=example,dc=com", "changetype: modify",
                "delete: dc", "dc: xxx")
                .addControl(PermissiveModifyRequestControl.newControl(true)));
        assertThat(connection.readEntry("dc=example,dc=com")).isEqualTo(
                valueOfLDIFEntry("dn: dc=example,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: example"));
    }

    @Test
    public void testModifyPreRead() throws Exception {
        final Connection connection = getConnection();
        assertThat(
                connection.modify(
                        newModifyRequest("dn: dc=example,dc=com", "changetype: modify",
                                "add: description", "description: test description").addControl(
                                PreReadRequestControl.newControl(true))).getControl(
                        PreReadResponseControl.DECODER, new DecodeOptions()).getEntry()).isEqualTo(
                valueOfLDIFEntry("dn: dc=example,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: example"));
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testModifyStrictWithDuplicateValues() throws Exception {
        final Connection connection = getConnection();
        connection.modify("dn: dc=example,dc=com", "changetype: modify", "add: dc", "dc: example");
    }

    @Test(expectedExceptions = ConstraintViolationException.class)
    public void testModifyStrictWithMissingValues() throws Exception {
        final Connection connection = getConnection();
        connection.modify("dn: dc=example,dc=com", "changetype: modify", "delete: dc", "dc: xxx");
    }

    @Test
    public void testSearchBase() throws Exception {
        final Connection connection = getConnection();
        assertThat(connection.readEntry("dc=example,dc=com")).isEqualTo(
                valueOfLDIFEntry("dn: dc=example,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: example"));
    }

    @Test(expectedExceptions = EntryNotFoundException.class)
    public void testSearchBaseNoSuchObject() throws Exception {
        final Connection connection = getConnection();
        connection.readEntry("dc=missing,dc=com");
    }

    @Test
    public void testSearchOneLevel() throws Exception {
        final Connection connection = getConnection();
        final ConnectionEntryReader reader =
                connection.search("dc=com", SearchScope.SINGLE_LEVEL, "(objectClass=*)");
        assertThat(reader.readEntry()).isEqualTo(
                valueOfLDIFEntry("dn: dc=example,dc=com", "objectClass: domain",
                        "objectClass: top", "dc: example"));
        assertThat(reader.readEntry()).isEqualTo(
                valueOfLDIFEntry("dn: dc=xxx,dc=com", "objectClass: domain", "objectClass: top",
                        "dc: xxx"));
        assertThat(reader.hasNext()).isFalse();
    }

    @Test
    public void testSearchSubtree() throws Exception {
        final Connection connection = getConnection();
        assertThat(
                connection.searchSingleEntry("dc=example,dc=com", SearchScope.WHOLE_SUBTREE,
                        "(uid=test1)")).isEqualTo(getUser1Entry());
    }

    @Test(expectedExceptions = EntryNotFoundException.class)
    public void testSearchSubtreeNotFound() throws Exception {
        final Connection connection = getConnection();
        connection.searchSingleEntry("dc=example,dc=com", SearchScope.WHOLE_SUBTREE,
                "(uid=missing)");
    }

    @Test
    public void testSimpleBind() throws Exception {
        final Connection connection = getConnection();
        connection.bind("uid=test1,ou=people,dc=example,dc=com", "password".toCharArray());
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void testSimpleBindBadPassword() throws Exception {
        final Connection connection = getConnection();
        connection.bind("uid=test1,ou=people,dc=example,dc=com", "bad".toCharArray());
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void testSimpleBindNoSuchUser() throws Exception {
        final Connection connection = getConnection();
        connection.bind("uid=missing,ou=people,dc=example,dc=com", "password".toCharArray());
    }

    @Test
    public void testSimpleBindPostRead() throws Exception {
        final Connection connection = getConnection();
        assertThat(
                connection.bind(
                        newSimpleBindRequest("uid=test1,ou=people,dc=example,dc=com",
                                "password".toCharArray()).addControl(
                                PostReadRequestControl.newControl(true))).getControl(
                        PostReadResponseControl.DECODER, new DecodeOptions()).getEntry())
                .isEqualTo(getUser1Entry());
    }

    @Test
    public void testSimpleBindPreRead() throws Exception {
        final Connection connection = getConnection();
        assertThat(
                connection.bind(
                        newSimpleBindRequest("uid=test1,ou=people,dc=example,dc=com",
                                "password".toCharArray()).addControl(
                                PreReadRequestControl.newControl(true))).getControl(
                        PreReadResponseControl.DECODER, new DecodeOptions()).getEntry()).isEqualTo(
                getUser1Entry());
    }

    private Connection getConnection() throws IOException {
        // @formatter:off
        final MemoryBackend backend =
                new MemoryBackend(new LDIFEntryReader(
                        "dn: dc=com",
                        "objectClass: domain",
                        "objectClass: top",
                        "dc: com",
                        "",
                        "dn: dc=example,dc=com",
                        "objectClass: domain",
                        "objectClass: top",
                        "dc: example",
                        "",
                        "dn: ou=People,dc=example,dc=com",
                        "objectClass: organizationalunit",
                        "objectClass: top",
                        "ou: People",
                        "",
                        "dn: uid=test1,ou=People,dc=example,dc=com",
                        "objectClass: top",
                        "objectClass: person",
                        "uid: test1",
                        "userpassword: password",
                        "cn: test user 1",
                        "sn: user 1",
                        "",
                        "dn: uid=test2,ou=People,dc=example,dc=com",
                        "objectClass: top",
                        "objectClass: person",
                        "uid: test2",
                        "userpassword: password",
                        "cn: test user 2",
                        "sn: user 2",
                        "",
                        "dn: dc=xxx,dc=com",
                        "objectClass: domain",
                        "objectClass: top",
                        "dc: xxx"
                ));
        // @formatter:on

        return newInternalConnection(backend);
    }

    private Entry getUser1Entry() {
        return valueOfLDIFEntry("dn: uid=test1,ou=People,dc=example,dc=com", "objectClass: top",
                "objectClass: person", "uid: test1", "userpassword: password", "cn: test user 1",
                "sn: user 1");
    }

}
