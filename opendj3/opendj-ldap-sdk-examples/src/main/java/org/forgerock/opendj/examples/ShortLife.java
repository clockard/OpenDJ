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
 *      Copyright 2012 ForgeRock AS
 *
 */

package org.forgerock.opendj.examples;

import java.io.IOException;

import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.Entries;
import org.forgerock.opendj.ldap.Entry;
import org.forgerock.opendj.ldap.ErrorResultException;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.ResultCode;
import org.forgerock.opendj.ldap.TreeMapEntry;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.responses.Result;
import org.forgerock.opendj.ldif.LDIFEntryWriter;

/**
 * A command-line client that creates, updates, renames, and deletes a
 * short-lived entry in order to demonstrate LDAP write operations using the
 * synchronous API. The client takes as arguments the host and port for the
 * directory server, and expects to find the entries and access control
 * instructions as defined in <a
 * href="http://opendj.forgerock.org/Example.ldif">Example.ldif</a>.
 *
 * <ul>
 * <li>host - host name of the directory server</li>
 * <li>port - port number of the directory server, e.g. 1389, 1636</li>
 * </ul>
 * All arguments are required.
 */
public final class ShortLife {

    /**
     * Connect to directory server as a user with rights to add, modify, and
     * delete entries, and then proceed to carry out the operations.
     *
     * @param args
     *            The command line arguments: host, port
     */
    public static void main(final String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: host port");
            System.err.println("For example: localhost 1389");
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        // User credentials of a "Directory Administrators" group member.
        // Kirsten Vaughan is authorized to create, update, and delete
        // entries.
        //
        // You might prompt an administrator user for information needed to
        // authenticate, or your application might have its own account with
        // rights to perform all necessary operations.
        String adminDN = "uid=kvaughan,ou=people,dc=example,dc=com";
        char[] adminPwd = "bribery".toCharArray();

        // An entry to add to the directory
        DN entryDN = DN.valueOf("cn=Bob,ou=People,dc=example,dc=com");
        Entry entry = new TreeMapEntry(entryDN);
        entry.addAttribute("cn", "Bob");
        entry.addAttribute("objectclass", "top");
        entry.addAttribute("objectclass", "person");
        entry.addAttribute("objectclass", "organizationalPerson");
        entry.addAttribute("objectclass", "inetOrgPerson");
        entry.addAttribute("mail", "subgenius@example.com");
        entry.addAttribute("sn", "Dobbs");

        LDIFEntryWriter writer = new LDIFEntryWriter(System.out);

        final LDAPConnectionFactory factory = new LDAPConnectionFactory(host, port);
        Connection connection = null;
        try {
            connection = factory.getConnection();
            Result result = connection.bind(adminDN, adminPwd);
            if (!result.isSuccess()) {
                System.out.println("Bind result: "
                        + result.getResultCode().toString() + " "
                        + result.getDiagnosticMessage());
                System.exit(result.getResultCode().intValue());
            }

            System.out.println("Creating an entry...");
            writeToConsole(writer, entry);
            result = connection.add(entry);
            if (!result.isSuccess()) {
                System.out.println("Add result: "
                        + result.getResultCode().toString() + " "
                        + result.getDiagnosticMessage());
                System.exit(result.getResultCode().intValue());
            }
            System.out.println("...done.");

            System.out.println("Updating mail address, adding description...");
            Entry old = TreeMapEntry.deepCopyOfEntry(entry);
            entry = entry.replaceAttribute("mail", "spammer@example.com");
            entry = entry.addAttribute("description", "Good user gone bad");
            writeToConsole(writer, entry);
            ModifyRequest request = Entries.diffEntries(old, entry);
            result = connection.modify(request);
            if (!result.isSuccess()) {
                System.out.println("Modify result: "
                        + result.getResultCode().toString() + " "
                        + result.getDiagnosticMessage());
                System.exit(result.getResultCode().intValue());
            }
            System.out.println("...done.");

            System.out.println("Renaming the entry...");
            DN newDN = DN.valueOf("cn=Ted,ou=People,dc=example,dc=com");
            entry = entry.setName(newDN);
            writeToConsole(writer, entry);
            result = connection.modifyDN(entryDN.toString(), "cn=Ted");
            if (!result.isSuccess()) {
                System.out.println("Rename result: "
                        + result.getResultCode().toString() + " "
                        + result.getDiagnosticMessage());
                System.exit(result.getResultCode().intValue());
            }
            System.out.println("...done.");

            System.out.println("Deleting the entry...");
            writeToConsole(writer, entry);
            result = connection.delete(newDN.toString());
            if (!result.isSuccess()) {
                System.out.println("Delete result: "
                        + result.getResultCode().toString() + " "
                        + result.getDiagnosticMessage());
                System.exit(result.getResultCode().intValue());
            }
            System.out.println("...done.");
        } catch (final ErrorResultException e) {
            System.err.println(e.getMessage());
            System.exit(e.getResult().getResultCode().intValue());
            return;
        } catch (final IOException e) {
            System.err.println(e.getMessage());
            System.exit(ResultCode.CLIENT_SIDE_LOCAL_ERROR.intValue());
            return;
        } finally {
            if (connection != null) {
                connection.close();
            }
            try {
                writer.close();
            } catch (final IOException ignored) {
                // Ignore.
            }
        }
    }

    /**
     * Write the entry in LDIF form to System.out.
     *
     * @param entry
     *            The entry to write to the console.
     */
    private static void writeToConsole(LDIFEntryWriter writer, Entry entry) throws IOException {
        writer.writeEntry(entry);
        writer.flush();
    }

    /**
     * Constructor not used.
     */
    private ShortLife() {
        // Not used.
    }
}
