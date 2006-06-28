/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006 Sun Microsystems, Inc.
 */
package org.opends.server.protocols.ldap;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.opends.server.core.DirectoryServer;
import org.opends.server.protocols.asn1.ASN1Element;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.protocols.asn1.ASN1Sequence;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.ObjectClass;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.util.Base64;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.messages.ProtocolMessages.*;
import static org.opends.server.protocols.ldap.LDAPConstants.*;
import static org.opends.server.protocols.ldap.LDAPResultCode.*;
import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines the structures and methods for an LDAP search result entry
 * protocol op, which is used to return entries that match the associated search
 * criteria.
 */
public class SearchResultEntryProtocolOp
       extends ProtocolOp
{
  /**
   * The fully-qualified name of this class to use for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.protocols.ldap.SearchResultEntryProtocolOp";



  // The set of attributes for this search entry.
  private LinkedList<LDAPAttribute> attributes;

  // The DN for this search entry.
  private DN dn;



  /**
   * Creates a new LDAP search result entry protocol op with the specified DN
   * and no attributes.
   *
   * @param  dn  The DN for this search result entry.
   */
  public SearchResultEntryProtocolOp(DN dn)
  {
    assert debugConstructor(CLASS_NAME, String.valueOf(dn));

    this.dn         = dn;
    this.attributes = new LinkedList<LDAPAttribute>();
  }



  /**
   * Creates a new LDAP search result entry protocol op with the specified DN
   * and set of attributes.
   *
   * @param  dn          The DN for this search result entry.
   * @param  attributes  The set of attributes for this search result entry.
   */
  public SearchResultEntryProtocolOp(DN dn,
                                     LinkedList<LDAPAttribute> attributes)
  {
    assert debugConstructor(CLASS_NAME, String.valueOf(dn),
                            String.valueOf(attributes));

    this.dn = dn;

    if (attributes == null)
    {
      this.attributes = new LinkedList<LDAPAttribute>();
    }
    else
    {
      this.attributes = attributes;
    }
  }



  /**
   * Creates a new search result entry protocol op from the provided search
   * result entry.
   *
   * @param  searchEntry  The search result entry object to use to create this
   *                      search result entry protocol op.
   */
  public SearchResultEntryProtocolOp(SearchResultEntry searchEntry)
  {
    assert debugConstructor(CLASS_NAME, String.valueOf(searchEntry));

    this.dn = searchEntry.getDN();

    attributes = new LinkedList<LDAPAttribute>();

    Attribute ocAttr = searchEntry.getObjectClassAttribute();
    if (ocAttr != null)
    {
      attributes.add(new LDAPAttribute(ocAttr));
    }

    for (List<Attribute> attrList :
         searchEntry.getUserAttributes().values())
    {
      for (Attribute a : attrList)
      {
        attributes.add(new LDAPAttribute(a));
      }
    }

    for (List<Attribute> attrList :
         searchEntry.getOperationalAttributes().values())
    {
      for (Attribute a : attrList)
      {
        attributes.add(new LDAPAttribute(a));
      }
    }
  }



  /**
   * Retrieves the DN for this search result entry.
   *
   * @return  The DN for this search result entry.
   */
  public DN getDN()
  {
    assert debugEnter(CLASS_NAME, "getDN");

    return dn;
  }



  /**
   * Specifies the DN for this search result entry.
   *
   * @param  dn  The DN for this search result entry.
   */
  public void setDN(DN dn)
  {
    assert debugEnter(CLASS_NAME, "setDN", String.valueOf(dn));

    this.dn = dn;
  }



  /**
   * Retrieves the set of attributes for this search result entry.  The returned
   * list may be altered by the caller.
   *
   * @return  The set of attributes for this search result entry.
   */
  public LinkedList<LDAPAttribute> getAttributes()
  {
    assert debugEnter(CLASS_NAME, "getAttributes");

    return attributes;
  }



  /**
   * Retrieves the BER type for this protocol op.
   *
   * @return  The BER type for this protocol op.
   */
  public byte getType()
  {
    assert debugEnter(CLASS_NAME, "getType");

    return OP_TYPE_SEARCH_RESULT_ENTRY;
  }



  /**
   * Retrieves the name for this protocol op type.
   *
   * @return  The name for this protocol op type.
   */
  public String getProtocolOpName()
  {
    assert debugEnter(CLASS_NAME, "getProtocolOpName");

    return "Search Result Entry";
  }



  /**
   * Encodes this protocol op to an ASN.1 element suitable for including in an
   * LDAP message.
   *
   * @return  The ASN.1 element containing the encoded protocol op.
   */
  public ASN1Element encode()
  {
    assert debugEnter(CLASS_NAME, "encode");

    ArrayList<ASN1Element> elements = new ArrayList<ASN1Element>(2);
    elements.add(new ASN1OctetString(dn.toString()));


    ArrayList<ASN1Element> attrElements =
         new ArrayList<ASN1Element>(attributes.size());
    for (LDAPAttribute attr : attributes)
    {
      attrElements.add(attr.encode());
    }
    elements.add(new ASN1Sequence(attrElements));


    return new ASN1Sequence(OP_TYPE_SEARCH_RESULT_ENTRY, elements);
  }



  /**
   * Decodes the provided ASN.1 element as an LDAP search result entry protocol
   * op.
   *
   * @param  element  The ASN.1 element to be decoded.
   *
   * @return  The decoded search result entry protocol op.
   *
   * @throws  LDAPException  If a problem occurs while decoding the provided
   *                         ASN.1 element as an LDAP search result entry
   *                         protocol op.
   */
  public static SearchResultEntryProtocolOp decodeSearchEntry(ASN1Element
                                                                   element)
         throws LDAPException
  {
    assert debugEnter(CLASS_NAME, "decodeSearchEntry", String.valueOf(element));

    ArrayList<ASN1Element> elements;
    try
    {
      elements = element.decodeAsSequence().elements();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "decodeSearchEntry", e);

      int    msgID   = MSGID_LDAP_SEARCH_ENTRY_DECODE_SEQUENCE;
      String message = getMessage(msgID, String.valueOf(e));
      throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
    }


    int numElements = elements.size();
    if (numElements != 2)
    {
      int    msgID   = MSGID_LDAP_SEARCH_ENTRY_DECODE_INVALID_ELEMENT_COUNT;
      String message = getMessage(msgID, numElements);
      throw new LDAPException(PROTOCOL_ERROR, msgID, message);
    }


    DN dn;
    try
    {
      dn = DN.decode(elements.get(0).decodeAsOctetString().stringValue());
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "decodeSearchEntry", e);

      int    msgID   = MSGID_LDAP_SEARCH_ENTRY_DECODE_DN;
      String message = getMessage(msgID, String.valueOf(e));
      throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
    }



    LinkedList<LDAPAttribute> attributes;
    try
    {
      ArrayList<ASN1Element> attrElements =
           elements.get(1).decodeAsSequence().elements();
      attributes = new LinkedList<LDAPAttribute>();
      for (ASN1Element e : attrElements)
      {
        attributes.add(LDAPAttribute.decode(e));
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "decodeSearchEntry", e);

      int    msgID   = MSGID_LDAP_SEARCH_ENTRY_DECODE_ATTRS;
      String message = getMessage(msgID, String.valueOf(e));
      throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
    }


    return new SearchResultEntryProtocolOp(dn, attributes);
  }



  /**
   * Appends a string representation of this LDAP protocol op to the provided
   * buffer.
   *
   * @param  buffer  The buffer to which the string should be appended.
   */
  public void toString(StringBuilder buffer)
  {
    assert debugEnter(CLASS_NAME, "toString", "java.lang.StringBuilder");

    buffer.append("SearchResultEntry(dn=");
    dn.toString(buffer);
    buffer.append(", attrs={");

    if (! attributes.isEmpty())
    {
      Iterator<LDAPAttribute> iterator = attributes.iterator();
      iterator.next().toString(buffer);

      while (iterator.hasNext())
      {
        buffer.append(", ");
        iterator.next().toString(buffer);
      }
    }

    buffer.append("})");
  }



  /**
   * Appends a multi-line string representation of this LDAP protocol op to the
   * provided buffer.
   *
   * @param  buffer  The buffer to which the information should be appended.
   * @param  indent  The number of spaces from the margin that the lines should
   *                 be indented.
   */
  public void toString(StringBuilder buffer, int indent)
  {
    assert debugEnter(CLASS_NAME, "toString", "java.lang.StringBuilder",
                      String.valueOf(indent));

    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0 ; i < indent; i++)
    {
      indentBuf.append(' ');
    }

    buffer.append(indentBuf);
    buffer.append("Search Result Entry");
    buffer.append(EOL);

    buffer.append(indentBuf);
    buffer.append("  DN:  ");
    dn.toString(buffer);
    buffer.append(EOL);

    buffer.append("  Attributes:");
    buffer.append(EOL);

    for (LDAPAttribute attribute : attributes)
    {
      attribute.toString(buffer, indent+4);
    }
  }



  /**
   * Appends an LDIF representation of the entry to the provided buffer.
   *
   * @param  buffer      The buffer to which the entry should be appended.
   * @param  wrapColumn  The column at which long lines should be wrapped.
   */
  public void toLDIF(StringBuilder buffer, int wrapColumn)
  {
    assert debugEnter(CLASS_NAME, "toLDIF", "java.lang.StringBuilder",
                      String.valueOf(wrapColumn));


    // Add the DN to the buffer.
    String dnString = dn.toString();
    int    colsRemaining;
    if (needsBase64Encoding(dnString))
    {
      dnString = Base64.encode(getBytes(dnString));
      buffer.append("dn:: ");

      colsRemaining = wrapColumn - 5;
    }
    else
    {
      buffer.append("dn: ");

      colsRemaining = wrapColumn - 4;
    }

    int dnLength = dnString.length();
    if ((dnLength <= colsRemaining) || (colsRemaining <= 0))
    {
      buffer.append(dnString);
      buffer.append(EOL);
    }
    else
    {
      buffer.append(dnString.substring(0, colsRemaining));
      buffer.append(EOL);

      int startPos = colsRemaining;
      while ((dnLength - startPos) > (wrapColumn - 1))
      {
        buffer.append(" ");
        buffer.append(dnString.substring(startPos, (startPos+wrapColumn-1)));
        buffer.append(EOL);

        startPos += (wrapColumn-1);
      }

      if (startPos < dnLength)
      {
        buffer.append(" ");
        buffer.append(dnString.substring(startPos));
        buffer.append(EOL);
      }
    }


    // Add the attributes to the buffer.
    for (LDAPAttribute a : attributes)
    {
      String name       = a.getAttributeType();
      int    nameLength = name.length();

      for (ASN1OctetString v : a.getValues())
      {
        String valueString;
        if (needsBase64Encoding(v.value()))
        {
          valueString = Base64.encode(v.value());
          buffer.append(name);
          buffer.append(":: ");

          colsRemaining = wrapColumn - nameLength - 3;
        }
        else
        {
          valueString = v.stringValue();
          buffer.append(name);
          buffer.append(": ");

          colsRemaining = wrapColumn - nameLength - 2;
        }

        int valueLength = valueString.length();
        if ((valueLength <= colsRemaining) || (colsRemaining <= 0))
        {
          buffer.append(valueString);
          buffer.append(EOL);
        }
        else
        {
          buffer.append(valueString.substring(0, colsRemaining));
          buffer.append(EOL);

          int startPos = colsRemaining;
          while ((valueLength - startPos) > (wrapColumn - 1))
          {
            buffer.append(" ");
            buffer.append(valueString.substring(startPos,
                                                (startPos+wrapColumn-1)));
            buffer.append(EOL);

            startPos += (wrapColumn-1);
          }

          if (startPos < valueLength)
          {
            buffer.append(" ");
            buffer.append(valueString.substring(startPos));
            buffer.append(EOL);
          }
        }
      }
    }


    // Make sure to add an extra blank line to ensure that there will be one
    // between this entry and the next.
    buffer.append(EOL);
  }



  /**
   * Converts this protocol op to a search result entry.
   *
   * @return  The search result entry created from this protocol op.
   *
   * @throws  LDAPException  If a problem occurs while trying to create the
   *                         search result entry.
   */
  public SearchResultEntry toSearchResultEntry()
         throws LDAPException
  {
    assert debugEnter(CLASS_NAME, "toSearchResultEntry");

    HashMap<ObjectClass,String> objectClasses =
         new HashMap<ObjectClass,String>();
    HashMap<AttributeType,List<Attribute>> userAttributes =
         new HashMap<AttributeType,List<Attribute>>();
    HashMap<AttributeType,List<Attribute>> operationalAttributes =
         new HashMap<AttributeType,List<Attribute>>();


    for (LDAPAttribute a : attributes)
    {
      Attribute     attr     = a.toAttribute();
      AttributeType attrType = attr.getAttributeType();

      if (attrType.isObjectClassType())
      {
        for (ASN1OctetString os : a.getValues())
        {
          String ocName = os.toString();
          ObjectClass oc =
               DirectoryServer.getObjectClass(toLowerCase(ocName));
          if (oc == null)
          {
            oc = DirectoryServer.getDefaultObjectClass(ocName);
          }

          objectClasses.put(oc ,ocName);
        }
      }
      else if (attrType.isOperational())
      {
        List<Attribute> attrs = operationalAttributes.get(attrType);
        if (attrs == null)
        {
          attrs = new ArrayList<Attribute>(1);
          attrs.add(attr);
          operationalAttributes.put(attrType, attrs);
        }
        else
        {
          attrs.add(attr);
        }
      }
      else
      {
        List<Attribute> attrs = userAttributes.get(attrType);
        if (attrs == null)
        {
          attrs = new ArrayList<Attribute>(1);
          attrs.add(attr);
          userAttributes.put(attrType, attrs);
        }
        else
        {
          // Check to see if any of the existing attributes in the list have the
          // same set of options.  If so, then add the values to that attribute.
          boolean attributeSeen = false;
          for (Attribute ea : attrs)
          {
            if (ea.optionsEqual(attr.getOptions()))
            {
              LinkedHashSet<AttributeValue> valueSet = ea.getValues();
              valueSet.addAll(attr.getValues());
              attributeSeen = true;
            }
          }
          if (!attributeSeen)
          {
            // This is the first occurrence of the attribute and options.
            attrs.add(attr);
          }
        }
      }
    }


    Entry entry = new Entry(dn, objectClasses, userAttributes,
                            operationalAttributes);
    return new SearchResultEntry(entry);
  }
}

