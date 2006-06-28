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
import java.util.Iterator;
import java.util.List;

import org.opends.server.protocols.asn1.ASN1Element;
import org.opends.server.protocols.asn1.ASN1Enumerated;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.protocols.asn1.ASN1Sequence;
import org.opends.server.types.DN;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.messages.ProtocolMessages.*;
import static org.opends.server.protocols.ldap.LDAPConstants.*;
import static org.opends.server.protocols.ldap.LDAPResultCode.*;
import static org.opends.server.util.ServerConstants.*;



/**
 * This class defines the structures and methods for an LDAP extended response
 * protocol op, which is used to provide information about the result of
 * processing a extended request.
 */
public class ExtendedResponseProtocolOp
       extends ProtocolOp
{
  /**
   * The fully-qualified name of this class to use for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.protocols.ldap.ExtendedResponseProtocolOp";



  // The value for this extended response.
  private ASN1OctetString value;

  // The matched DN for this response.
  private DN matchedDN;

  // The result code for this response.
  private int resultCode;

  // The set of referral URLs for this response.
  private List<String> referralURLs;

  // The error message for this response.
  private String errorMessage;

  // The OID for this extended response.
  private String oid;



  /**
   * Creates a new extended response protocol op with the provided result code.
   *
   * @param  resultCode  The result code for this response.
   */
  public ExtendedResponseProtocolOp(int resultCode)
  {
    assert debugConstructor(CLASS_NAME, String.valueOf(resultCode));

    this.resultCode = resultCode;

    errorMessage = null;
    matchedDN    = null;
    referralURLs = null;
    oid          = null;
    value        = null;
  }



  /**
   * Creates a new extended response protocol op with the provided result code
   * and error message.
   *
   * @param  resultCode    The result code for this response.
   * @param  errorMessage  The error message for this response.
   */
  public ExtendedResponseProtocolOp(int resultCode, String errorMessage)
  {
    assert debugEnter(CLASS_NAME, String.valueOf(resultCode),
                      String.valueOf(errorMessage));

    this.resultCode   = resultCode;
    this.errorMessage = errorMessage;

    matchedDN    = null;
    referralURLs = null;
    oid          = null;
    value        = null;
  }



  /**
   * Creates a new extended response protocol op with the provided information.
   *
   * @param  resultCode    The result code for this response.
   * @param  errorMessage  The error message for this response.
   * @param  matchedDN     The matched DN for this response.
   * @param  referralURLs  The referral URLs for this response.
   */
  public ExtendedResponseProtocolOp(int resultCode, String errorMessage,
                                    DN matchedDN, List<String> referralURLs)
  {
    assert debugEnter(CLASS_NAME, String.valueOf(resultCode),
                      String.valueOf(errorMessage), String.valueOf(matchedDN),
                      String.valueOf(referralURLs));

    this.resultCode   = resultCode;
    this.errorMessage = errorMessage;
    this.matchedDN    = matchedDN;
    this.referralURLs = referralURLs;

    oid   = null;
    value = null;
  }



  /**
   * Creates a new extended response protocol op with the provided information.
   *
   * @param  resultCode    The result code for this response.
   * @param  errorMessage  The error message for this response.
   * @param  matchedDN     The matched DN for this response.
   * @param  referralURLs  The referral URLs for this response.
   * @param  oid           The OID for this extended response.
   * @param  value         The value for this extended response.
   */
  public ExtendedResponseProtocolOp(int resultCode, String errorMessage,
                                    DN matchedDN, List<String> referralURLs,
                                    String oid, ASN1OctetString value)
  {
    assert debugEnter(CLASS_NAME, String.valueOf(resultCode),
                      String.valueOf(errorMessage), String.valueOf(matchedDN),
                      String.valueOf(referralURLs));

    this.resultCode   = resultCode;
    this.errorMessage = errorMessage;
    this.matchedDN    = matchedDN;
    this.referralURLs = referralURLs;
    this.oid          = oid;
    this.value        = value;
  }



  /**
   * Retrieves the result code for this response.
   *
   * @return  The result code for this response.
   */
  public int getResultCode()
  {
    assert debugEnter(CLASS_NAME, "getResultCode");

    return resultCode;
  }



  /**
   * Specifies the result code for this response.
   *
   * @param  resultCode  The result code for this response.
   */
  public void setResultCode(int resultCode)
  {
    assert debugEnter(CLASS_NAME, "setResultCode", String.valueOf(resultCode));

    this.resultCode = resultCode;
  }



  /**
   * Retrieves the error message for this response.
   *
   * @return  The error message for this response, or <CODE>null</CODE> if none
   *          is available.
   */
  public String getErrorMessage()
  {
    assert debugEnter(CLASS_NAME, "getErrorMessage");

    return errorMessage;
  }



  /**
   * Specifies the error message for this response.
   *
   * @param  errorMessage  The error message for this response.
   */
  public void setErrorMessage(String errorMessage)
  {
    assert debugEnter(CLASS_NAME, "setErrorMessage",
                      String.valueOf(errorMessage));

    this.errorMessage = errorMessage;
  }



  /**
   * Retrieves the matched DN for this response.
   *
   * @return  The matched DN for this response, or <CODE>null</CODE> if none is
   *          available.
   */
  public DN getMatchedDN()
  {
    assert debugEnter(CLASS_NAME, "getMatchedDN");

    return matchedDN;
  }



  /**
   * Specifies the matched DN for this response.
   *
   * @param  matchedDN  The matched DN for this response.
   */
  public void setMatchedDN(DN matchedDN)
  {
    assert debugEnter(CLASS_NAME, "setMatchedDN", String.valueOf(matchedDN));

    this.matchedDN = matchedDN;
  }



  /**
   * Retrieves the set of referral URLs for this response.
   *
   * @return  The set of referral URLs for this response, or <CODE>null</CODE>
   *          if none are available.
   */
  public List<String> getReferralURLs()
  {
    assert debugEnter(CLASS_NAME, "getReferralURLs");

    return referralURLs;
  }



  /**
   * Specifies the set of referral URLs for this response.
   *
   * @param  referralURLs  The set of referral URLs for this response.
   */
  public void setReferralURLs(List<String> referralURLs)
  {
    assert debugEnter(CLASS_NAME, "setReferralURLs",
                      String.valueOf(referralURLs));

    this.referralURLs = referralURLs;
  }



  /**
   * Retrieves the OID for this extended response.
   *
   * @return  The OID for this extended response, or <CODE>null</CODE> if none
   *          was provided.
   */
  public String getOID()
  {
    assert debugEnter(CLASS_NAME, "getOID");

    return oid;
  }



  /**
   * Specifies the OID for this extended response.
   *
   * @param  oid  The OID for this extended response.
   */
  public void setOID(String oid)
  {
    assert debugEnter(CLASS_NAME, "setOID", String.valueOf(oid));

    this.oid = oid;
  }



  /**
   * Retrieves the value for this extended response.
   *
   * @return  The value for this extended response, or <CODE>null</CODE> if none
   *          was provided.
   */
  public ASN1OctetString getValue()
  {
    assert debugEnter(CLASS_NAME, "getValue");

    return value;
  }



  /**
   * Specifies the value for this extended response.
   *
   * @param  value  The value for this extended response.
   */
  public void setValue(ASN1OctetString value)
  {
    assert debugEnter(CLASS_NAME, "setValue", String.valueOf(value));

    this.value = value;
  }



  /**
   * Retrieves the BER type for this protocol op.
   *
   * @return  The BER type for this protocol op.
   */
  public byte getType()
  {
    assert debugEnter(CLASS_NAME, "getType");

    return OP_TYPE_EXTENDED_RESPONSE;
  }



  /**
   * Retrieves the name for this protocol op type.
   *
   * @return  The name for this protocol op type.
   */
  public String getProtocolOpName()
  {
    assert debugEnter(CLASS_NAME, "getProtocolOpName");

    return "Extended Response";
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

    ArrayList<ASN1Element> elements = new ArrayList<ASN1Element>(6);
    elements.add(new ASN1Enumerated(resultCode));

    if (matchedDN == null)
    {
      elements.add(new ASN1OctetString());
    }
    else
    {
      elements.add(new ASN1OctetString(matchedDN.toString()));
    }

    elements.add(new ASN1OctetString(errorMessage));

    if ((referralURLs != null) && (! referralURLs.isEmpty()))
    {
      ArrayList<ASN1Element> referralElements =
           new ArrayList<ASN1Element>(referralURLs.size());

      for (String s : referralURLs)
      {
        referralElements.add(new ASN1OctetString(s));
      }

      elements.add(new ASN1Sequence(TYPE_REFERRAL_SEQUENCE, referralElements));
    }

    if ((oid != null) && (oid.length() > 0))
    {
      elements.add(new ASN1OctetString(TYPE_EXTENDED_RESPONSE_OID, oid));
    }

    if (value != null)
    {
      value.setType(TYPE_EXTENDED_RESPONSE_VALUE);
      elements.add(value);
    }

    return new ASN1Sequence(OP_TYPE_EXTENDED_RESPONSE, elements);
  }



  /**
   * Decodes the provided ASN.1 element as a extended response protocol op.
   *
   * @param  element  The ASN.1 element to decode.
   *
   * @return  The decoded extended response protocol op.
   *
   * @throws  LDAPException  If a problem occurs while attempting to decode the
   *                         ASN.1 element to a protocol op.
   */
  public static ExtendedResponseProtocolOp decodeExtendedResponse(ASN1Element
                                                                       element)
         throws LDAPException
  {
    assert debugEnter(CLASS_NAME, "decodeExtendedResponse",
                      String.valueOf(element));

    ArrayList<ASN1Element> elements;
    try
    {
      elements = element.decodeAsSequence().elements();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "decodeExtendedResponse", e);

      int    msgID   = MSGID_LDAP_RESULT_DECODE_SEQUENCE;
      String message = getMessage(msgID, String.valueOf(e));
      throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
    }


    int numElements = elements.size();
    if ((numElements < 3) || (numElements > 6))
    {
      int    msgID   = MSGID_LDAP_EXTENDED_RESULT_DECODE_INVALID_ELEMENT_COUNT;
      String message = getMessage(msgID, numElements);
      throw new LDAPException(PROTOCOL_ERROR, msgID, message);
    }


    int resultCode;
    try
    {
      resultCode = elements.get(0).decodeAsInteger().intValue();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "decodeExtendedResponse", e);

      int    msgID   = MSGID_LDAP_RESULT_DECODE_RESULT_CODE;
      String message = getMessage(msgID, String.valueOf(e));
      throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
    }


    DN matchedDN;
    try
    {
      String dnString = elements.get(1).decodeAsOctetString().stringValue();
      if (dnString.length() == 0)
      {
        matchedDN = null;
      }
      else
      {
        matchedDN = DN.decode(dnString);
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "decodeExtendedResponse", e);

      int    msgID   = MSGID_LDAP_RESULT_DECODE_MATCHED_DN;
      String message = getMessage(msgID, String.valueOf(e));
      throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
    }


    String errorMessage;
    try
    {
      errorMessage = elements.get(2).decodeAsOctetString().stringValue();
      if (errorMessage.length() == 0)
      {
        errorMessage = null;
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "decodeExtendedResponse", e);

      int    msgID   = MSGID_LDAP_RESULT_DECODE_ERROR_MESSAGE;
      String message = getMessage(msgID, String.valueOf(e));
      throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
    }


    ArrayList<String> referralURLs = null;
    String            oid          = null;
    ASN1OctetString   value        = null;
    for (int i=3; i < elements.size(); i++)
    {
      element = elements.get(i);
      switch (element.getType())
      {
        case TYPE_REFERRAL_SEQUENCE:
          try
          {
            ArrayList<ASN1Element> referralElements =
                 element.decodeAsSequence().elements();
            referralURLs = new ArrayList<String>(referralElements.size());

            for (ASN1Element e : referralElements)
            {
              referralURLs.add(e.decodeAsOctetString().stringValue());
            }
          }
          catch (Exception e)
          {
            assert debugException(CLASS_NAME, "decodeExtendedResponse", e);

            int    msgID   = MSGID_LDAP_EXTENDED_RESULT_DECODE_REFERRALS;
            String message = getMessage(msgID, String.valueOf(e));
            throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
          }

          break;
        case TYPE_EXTENDED_RESPONSE_OID:
          try
          {
            oid = element.decodeAsOctetString().stringValue();
          }
          catch (Exception e)
          {
            assert debugException(CLASS_NAME, "decodeExtendedResponse", e);

            int    msgID   = MSGID_LDAP_EXTENDED_RESULT_DECODE_OID;
            String message = getMessage(msgID, String.valueOf(e));
            throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
          }

          break;
        case TYPE_EXTENDED_RESPONSE_VALUE:
          try
          {
            value = element.decodeAsOctetString();
          }
          catch (Exception e)
          {
            assert debugException(CLASS_NAME, "decodeExtendedResponse", e);

            int    msgID   = MSGID_LDAP_EXTENDED_RESULT_DECODE_VALUE;
            String message = getMessage(msgID, String.valueOf(e));
            throw new LDAPException(PROTOCOL_ERROR, msgID, message, e);
          }

          break;
        default:
          int    msgID   = MSGID_LDAP_EXTENDED_RESULT_DECODE_INVALID_TYPE;
          String message = getMessage(msgID, element.getType());
          throw new LDAPException(PROTOCOL_ERROR, msgID, message);
      }
    }


    return new ExtendedResponseProtocolOp(resultCode, errorMessage, matchedDN,
                                          referralURLs, oid, value);
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

    buffer.append("ExtendedResponse(resultCode=");
    buffer.append(resultCode);

    if ((errorMessage != null) && (errorMessage.length() > 0))
    {
      buffer.append(", errorMessage=");
      buffer.append(errorMessage);
    }

    if (matchedDN != null)
    {
      buffer.append(", matchedDN=");
      buffer.append(matchedDN.toString());
    }

    if ((referralURLs != null) && (! referralURLs.isEmpty()))
    {
      buffer.append(", referralURLs={");

      Iterator<String> iterator = referralURLs.iterator();
      buffer.append(iterator.next());

      while (iterator.hasNext())
      {
        buffer.append(", ");
        buffer.append(iterator.next());
      }

      buffer.append("}");
    }

    if ((oid != null) && (oid.length() > 0))
    {
      buffer.append(", oid=");
      buffer.append(oid);
    }

    if (value != null)
    {
      buffer.append(", value=");
      value.toString(buffer);
    }

    buffer.append(")");
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
    buffer.append("Extended Response");
    buffer.append(EOL);

    buffer.append(indentBuf);
    buffer.append("  Result Code:  ");
    buffer.append(resultCode);
    buffer.append(EOL);

    if (errorMessage != null)
    {
      buffer.append(indentBuf);
      buffer.append("  Error Message:  ");
      buffer.append(errorMessage);
      buffer.append(EOL);
    }

    if (matchedDN != null)
    {
      buffer.append(indentBuf);
      buffer.append("  Matched DN:  ");
      matchedDN.toString(buffer);
      buffer.append(EOL);
    }

    if ((referralURLs != null) && (! referralURLs.isEmpty()))
    {
      buffer.append(indentBuf);
      buffer.append("  Referral URLs:  ");
      buffer.append(EOL);

      for (String s : referralURLs)
      {
        buffer.append(indentBuf);
        buffer.append("  ");
        buffer.append(s);
        buffer.append(EOL);
      }
    }

    if ((oid != null) && (oid.length() > 0))
    {
      buffer.append(indentBuf);
      buffer.append("  Response OID:  ");
      buffer.append(oid);
      buffer.append(EOL);
    }

    if (value != null)
    {
      buffer.append(indentBuf);
      buffer.append("  Response Value:  ");
      value.toString(buffer);
      buffer.append(EOL);
    }
  }
}

