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
package org.forgerock.opendj.adapter.server2x;

import static org.fest.assertions.Assertions.assertThat;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.forgerock.opendj.ldap.Modification;
import org.forgerock.opendj.ldap.ModificationType;
import org.forgerock.opendj.ldap.controls.Control;
import org.forgerock.opendj.ldap.controls.GenericControl;
import org.forgerock.opendj.ldap.controls.PersistentSearchChangeType;
import org.forgerock.opendj.ldap.controls.PersistentSearchRequestControl;
import org.forgerock.opendj.ldap.controls.PostReadRequestControl;
import org.forgerock.opendj.ldap.controls.ProxiedAuthV2RequestControl;
import org.forgerock.opendj.ldap.requests.BindClient;
import org.forgerock.opendj.ldap.requests.CRAMMD5SASLBindRequest;
import org.forgerock.opendj.ldap.requests.GenericBindRequest;
import org.forgerock.opendj.ldap.requests.PlainSASLBindRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.responses.Responses;
import org.forgerock.testng.ForgeRockTestCase;
import org.opends.server.protocols.ldap.LDAPControl;
import org.opends.server.protocols.ldap.LDAPFilter;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.FilterType;
import org.opends.server.types.LDAPException;
import org.opends.server.types.SearchResultReference;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

import static org.forgerock.opendj.adapter.server2x.StaticUtils.from;
import static org.forgerock.opendj.adapter.server2x.StaticUtils.to;
import static org.forgerock.opendj.adapter.server2x.StaticUtils.getCredentials;

/**
 * This class defines a set of tests for the StaticUtils.class.
 * <p>
 * Reminder :
 *
 * <pre>
 * to - from SDK to server.
 * from - from server to SDK.
 * </pre>
 */
@SuppressWarnings("javadoc")
@Test()
public class StaticUtilsTestCase extends ForgeRockTestCase {

    /**
     * Launched before the tests, this function starts the embedded server.
     *
     * @throws Exception
     *             If the server could not be initialized.
     */
    @BeforeGroups(groups = "needRunningServer")
    public void startServer() throws Exception {
        EmbeddedServerTestCaseUtils.startServer();
    }

    /**
     * Stops the server at the end of the test class.
     */
    @AfterGroups(groups = "needRunningServer")
    public void shutDownServer() {
        // Stops the server.
        EmbeddedServerTestCaseUtils.shutDownServer();
    }

    /**
     * Converts a SDK Distinguished Name to a LDAP server Distinguish Name.
     * Needs a running server to work.
     *
     * @throws DirectoryException
     */
    @Test(groups = { "needRunningServer" })
    public final void testToDN() throws DirectoryException {
        org.forgerock.opendj.ldap.DN sdkDN =
                org.forgerock.opendj.ldap.DN.valueOf("uid=scarter,ou=People,dc=example,dc=com");

        org.opends.server.types.DN srvDN = to(sdkDN);
        assertThat(srvDN.toString()).isEqualTo("uid=scarter,ou=People,dc=example,dc=com");
    }

    /**
     * Converts a SDK byteString to an LDAP Server ByteString.
     *
     * @throws DirectoryException
     */
    @Test()
    public final void testToByteString() throws DirectoryException {
        org.forgerock.opendj.ldap.ByteString sdkByteString = ByteString.valueOf("This is a test");

        org.opends.server.types.ByteString srvByteString = to(sdkByteString);
        assertThat(srvByteString.toString()).isEqualTo("This is a test");
    }

    /**
     * Converts a SDK DereferenceAliasesPolicy to an LDAP dereferencePolicy.
     *
     * @throws DirectoryException
     */
    @Test()
    public final void testToDeferencePolicy() {
        org.forgerock.opendj.ldap.DereferenceAliasesPolicy sdkDeferenceAliasesPolicy =
                org.forgerock.opendj.ldap.DereferenceAliasesPolicy.ALWAYS;

        org.opends.server.types.DereferencePolicy dereferencePolicy = to(sdkDeferenceAliasesPolicy);
        assertThat(dereferencePolicy).isEqualTo(
                org.opends.server.types.DereferencePolicy.DEREF_ALWAYS);
    }

    /**
     * Converts a SDK control to a LDAP server control.
     *
     * @throws DirectoryException
     */
    @Test()
    public final void testToControl() throws DirectoryException {

        final PersistentSearchRequestControl control =
                PersistentSearchRequestControl.newControl(false, true,
                        true, // isCritical, changesOnly, returnECs
                        PersistentSearchChangeType.ADD, PersistentSearchChangeType.DELETE,
                        PersistentSearchChangeType.MODIFY, PersistentSearchChangeType.MODIFY_DN);

        // control :
        // PersistentSearchRequestControl(oid=2.16.840.1.113730.3.4.3, criticality=true,
        // changeTypes=[add, delete, modify, modifyDN]([add, delete, modify, modifyDN]),
        // changesOnly=true, returnECs=true)

        org.opends.server.protocols.ldap.LDAPControl srvControl = to(control);
        assertThat(srvControl.isCritical()).isFalse();
        assertThat(srvControl.getOID()).isEqualTo("2.16.840.1.113730.3.4.3");
        assertThat(control.getValue().toString()).isEqualTo(srvControl.getValue().toString());

        // A PostReadRequestControl(SDK)
        final PostReadRequestControl control2 =
                PostReadRequestControl.newControl(true, "description");

        srvControl = to(control2);
        assertThat(srvControl.getOID()).isEqualTo("1.3.6.1.1.13.2");
        assertThat(srvControl.isCritical()).isTrue();
        assertThat(control2.getValue().toString()).isEqualTo(srvControl.getValue().toString());
    }

    /**
     * Converts a list of SDK controls to a list of LDAP server controls.
     *
     * @throws DirectoryException
     */
    @Test()
    public final void testToListOfControl() throws DirectoryException {

        final PersistentSearchRequestControl control =
                PersistentSearchRequestControl.newControl(false, true,
                        true, // isCritical, changesOnly, returnECs
                        PersistentSearchChangeType.ADD, PersistentSearchChangeType.DELETE,
                        PersistentSearchChangeType.MODIFY, PersistentSearchChangeType.MODIFY_DN);

        assertThat(control.getOID()).isEqualTo("2.16.840.1.113730.3.4.3");

        final PostReadRequestControl control2 =
                PostReadRequestControl.newControl(true, "description");
        assertThat(control2.getOID()).isEqualTo("1.3.6.1.1.13.2");

        final ProxiedAuthV2RequestControl control3 =
                ProxiedAuthV2RequestControl
                        .newControl("dn:uid=kvaughan,ou=People,dc=example,dc=com");
        assertThat(control3.getOID()).isEqualTo("2.16.840.1.113730.3.4.18");

        List<org.forgerock.opendj.ldap.controls.Control> mySDKControlsList =
                new LinkedList<org.forgerock.opendj.ldap.controls.Control>();
        mySDKControlsList.add(control);
        mySDKControlsList.add(control2);
        mySDKControlsList.add(control3);

        List<org.opends.server.types.Control> listofControl = to(mySDKControlsList);
        assertThat(listofControl.size()).isEqualTo(3);
        assertThat(listofControl.get(0).getOID()).isEqualTo("2.16.840.1.113730.3.4.3");
        assertThat(listofControl.get(0).isCritical()).isFalse();
        assertThat(listofControl.get(1).getOID()).isEqualTo("1.3.6.1.1.13.2");
        assertThat(listofControl.get(1).isCritical()).isTrue();
        assertThat(listofControl.get(2).getOID()).isEqualTo("2.16.840.1.113730.3.4.18");
        assertThat(listofControl.get(2).isCritical()).isTrue();
    }

    /**
     * Converts an SDK attribute to an LDAP server attribute.
     *
     * @throws DirectoryException
     */
    @Test()
    public final void testToAttribute() throws DirectoryException {
        org.forgerock.opendj.ldap.Attribute attribute = new LinkedAttribute("test", "value1");

        org.opends.server.types.RawAttribute srvAttribute = to(attribute);
        assertThat(srvAttribute.getAttributeType().toString()).isEqualTo("test");
        assertThat(srvAttribute.getValues().size()).isEqualTo(1);
        assertThat(srvAttribute.getValues().get(0).toString()).isEqualTo("value1");

        org.forgerock.opendj.ldap.Attribute attribute2 =
                new LinkedAttribute("Another", ByteString.valueOf("myValue"));

        org.opends.server.types.RawAttribute srvAttribute2 = to(attribute2);
        assertThat(srvAttribute2.getAttributeType().toString()).isEqualTo("Another");
        assertThat(srvAttribute2.getValues().size()).isEqualTo(1);
        assertThat(srvAttribute2.getValues().get(0).toString()).isEqualTo("myValue");

    }

    /**
     * Converts an SDK multi-valued attribute to an LDAP Server Attribute.
     *
     * @throws DirectoryException
     */
    @Test()
    public final void testToAttributeMultiValued() throws DirectoryException {
        org.forgerock.opendj.ldap.Attribute attribute =
                new LinkedAttribute("testMultiValuedAttribute", "value1", "value2");

        org.opends.server.types.RawAttribute srvAttribute = to(attribute);
        assertThat(srvAttribute.getAttributeType().toString())
                .isEqualTo("testMultiValuedAttribute");
        assertThat(srvAttribute.getValues().size()).isEqualTo(2);
        assertThat(srvAttribute.getValues().get(0).toString()).isEqualTo("value1");
        assertThat(srvAttribute.getValues().get(1).toString()).isEqualTo("value2");

        org.forgerock.opendj.ldap.Attribute attribute2 =
                new LinkedAttribute("AnotherMultiValuedAttribute", "value1", "value2", "value3",
                        "value4");

        org.opends.server.types.RawAttribute srvAttribute2 = to(attribute2);
        assertThat(srvAttribute2.getAttributeType().toString()).isEqualTo(
                "AnotherMultiValuedAttribute");
        assertThat(srvAttribute2.getValues().size()).isEqualTo(4);
        assertThat(srvAttribute2.getValues().get(0).toString()).isEqualTo("value1");
        assertThat(srvAttribute2.getValues().get(1).toString()).isEqualTo("value2");
        assertThat(srvAttribute2.getValues().get(2).toString()).isEqualTo("value3");
        assertThat(srvAttribute2.getValues().get(3).toString()).isEqualTo("value4");

    }

    /**
     * Converts a SDK modification to an LDAP server raw modification.
     */
    @Test()
    public final void testToModification() {
        org.forgerock.opendj.ldap.Attribute attribute =
                new LinkedAttribute("test", ByteString.valueOf("value1"), ByteString
                        .valueOf("value2"));

        Modification mod = new Modification(ModificationType.ADD, attribute);

        org.opends.server.types.RawModification srvModification = to(mod);
        assertThat(srvModification.getModificationType().toString()).isEqualTo("Add");
        assertThat(srvModification.getAttribute().getAttributeType()).isEqualTo("test");
        assertThat(srvModification.getAttribute().getValues().size()).isEqualTo(2);

        mod = new Modification(ModificationType.INCREMENT, attribute);
        srvModification = to(mod);
        assertThat(srvModification.getModificationType().toString()).isEqualTo("Increment");
    }

    /**
     * Converts a SDK filter to an LDAP server filter.
     *
     * @throws LDAPException
     */
    @Test()
    public final void testToFilter() throws LDAPException {
        Filter filter = Filter.valueOf("!(description=*)");
        org.opends.server.protocols.ldap.LDAPFilter srvFilter =
                LDAPFilter.decode(filter.toString());
        assertThat(srvFilter.getAttributeType()).isNull();
        assertThat(srvFilter.getFilterType()).isEqualTo(FilterType.NOT);
        assertThat(srvFilter.getNOTComponent().toString()).isEqualTo("(description=*)");

        filter = Filter.valueOf("(description=bjensen)");
        srvFilter = LDAPFilter.decode(filter.toString());
        assertThat(srvFilter.getAttributeType()).isEqualTo("description");
        assertThat(srvFilter.getFilterType()).isEqualTo(FilterType.EQUALITY);
        assertThat(srvFilter.getAssertionValue().toString()).isEqualTo("bjensen");
    }

    /**
     * Converts a SDK search result reference to a LDAP server search result
     * reference.
     *
     * @throws LDAPException
     */
    @Test()
    public final void testToSearchResultReference() throws LDAPException {
        String uri = "ldap://hostb/OU=People,O=MNN,C=WW??sub";
        final org.forgerock.opendj.ldap.responses.SearchResultReference sdkSearchResultReference =
                Responses.newSearchResultReference(uri);

        final org.opends.server.types.SearchResultReference srvResultReference =
                new SearchResultReference(uri);

        final org.opends.server.types.SearchResultReference srvResultReference2 =
                to(sdkSearchResultReference);
        assertThat(srvResultReference.getReferralURLString()).isEqualTo(
                srvResultReference2.getReferralURLString());
    }

    /**
     * Converts an LDAP byte string to an SDK byte string.
     */
    @Test()
    public final void testFromByteString() throws LDAPException {
        String str = "This is a test";
        org.opends.server.types.ByteString srvByteString =
                org.opends.server.types.ByteString.valueOf(str);

        ByteString sdkByteString = from(srvByteString);
        ByteString expectedSdkByteString = ByteString.valueOf(str);
        assertThat(sdkByteString).isEqualTo(expectedSdkByteString);
    }

    /**
     * Converts an LDAP control to an SDK control.
     */
    @Test()
    public static void testFromLDAPControl() {
        org.opends.server.protocols.ldap.LDAPControl ldapControl =
                new LDAPControl("1.2.3.4", false, to("myData"));
        Control sdkControl = from(ldapControl);

        Control expectedSdkControl = GenericControl.newControl("1.2.3.4", false, "myData");
        assertThat(sdkControl.getOID()).isEqualTo(expectedSdkControl.getOID());
        assertThat(sdkControl.isCritical()).isEqualTo(expectedSdkControl.isCritical());
        assertThat(sdkControl.getValue()).isEqualTo(expectedSdkControl.getValue());
    }

    /**
     * Converts a server control to an SDK control.
     */
    @Test()
    public static void testFromControl() {
        final org.opends.server.types.Control control =
                new LDAPControl("1.2.3.4", false, to("myData"));
        Control sdkControl = from(control);

        Control expectedSdkControl = GenericControl.newControl("1.2.3.4", false, "myData");
        assertThat(sdkControl.getOID()).isEqualTo(expectedSdkControl.getOID());
        assertThat(sdkControl.isCritical()).isEqualTo(expectedSdkControl.isCritical());
        assertThat(sdkControl.getValue()).isEqualTo(expectedSdkControl.getValue());
    }

    /**
     * For an SASL bind request, credentials are composed by uid and password
     * (in this config).
     *
     * @throws ErrorResultException
     */
    @Test(groups = { "needRunningServer" })
    public static void testgetCredentials() throws ErrorResultException {
        final PlainSASLBindRequest request =
                Requests.newPlainSASLBindRequest("u:user.0", ("password").toCharArray());

        final String serverName = InetAddress.getLoopbackAddress().getCanonicalHostName();
        final BindClient bindClient = request.createBindClient(serverName);
        final GenericBindRequest genericBindRequest = bindClient.nextBindRequest();
        final org.opends.server.types.ByteString expectedValue =
                org.opends.server.types.ByteString.valueOf("\u0000u:user.0\u0000password");

        assertThat(getCredentials(genericBindRequest.getAuthenticationValue())).isEqualTo(
                expectedValue);
    }

    /**
     * For an CRAMMD5 SALS request, the credentials are empty.
     *
     * @throws ErrorResultException
     */
    @Test(groups = { "needRunningServer" })
    public static void testgetCredentialsEmptyByteString() throws ErrorResultException {
        final CRAMMD5SASLBindRequest request =
                Requests.newCRAMMD5SASLBindRequest("u:user.2", ("password").toCharArray());

        final String serverName = InetAddress.getLoopbackAddress().getCanonicalHostName();
        final BindClient bindClient = request.createBindClient(serverName);
        final GenericBindRequest genericBindRequest = bindClient.nextBindRequest();
        final org.opends.server.types.ByteString expectedValue =
                org.opends.server.types.ByteString.empty();

        assertThat(getCredentials(genericBindRequest.getAuthenticationValue())).isEqualTo(
                expectedValue);
    }
}
