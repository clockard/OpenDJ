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
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */
package org.opends.server.types;



import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.opends.server.api.AttributeValueDecoder;
import org.opends.server.api.ProtocolElement;
import org.opends.server.api.plugin.LDIFPluginResult;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.PluginConfigManager;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.util.LDIFException;

import static org.opends.server.loggers.debug.DebugLogger.debugCought;
import static
    org.opends.server.loggers.debug.DebugLogger.debugEnabled;
import static
    org.opends.server.loggers.debug.DebugLogger.debugVerbose;
import static org.opends.server.loggers.debug.DebugLogger.debugInfo;
import static
    org.opends.server.loggers.debug.DebugLogger.debugWarning;
import static org.opends.server.loggers.Error.*;
import static org.opends.server.messages.CoreMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.messages.UtilityMessages.*;
import static org.opends.server.util.LDIFWriter.*;
import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines a data structure for a Directory Server entry.
 * It includes a DN and a set of attributes.
 * <BR><BR>
 * The entry also contains a volatile attachment object, which should
 * be used to associate the entry with a special type of object that
 * is based on its contents.  For example, if the entry holds access
 * control information, then the attachment might be an object that
 * contains a representation of that access control definition in a
 * more useful form.  This is only useful if the entry is to be
 * cached, since the attachment may be accessed if the entry is
 * retrieved from the cache, but if the entry is retrieved from the
 * backend repository it cannot be guaranteed to contain any
 * attachment (and in most cases will not).  This attachment is
 * volatile in that it is not always guaranteed to be present, it may
 * be removed or overwritten at any time, and it will be invalidated
 * and removed if the entry is altered in any way.
 */
public class Entry
       implements ProtocolElement
{



  // The set of operational attributes for this entry.
  private Map<AttributeType,List<Attribute>> operationalAttributes;

  // The set of user attributes for this entry.
  private Map<AttributeType,List<Attribute>> userAttributes;

  // The set of objectclasses for this entry.
  private Map<ObjectClass,String> objectClasses;

  // The DN for this entry.
  private DN dn;

  // A generic attachment that may be used to associate this entry
  // with some other object.
  private transient Object attachment;

  // The schema used to govern this entry.
  private Schema schema;



  /**
   * Creates a new entry with the provided information.
   *
   * @param  dn                     The distinguished name for this
   *                                entry.
   * @param  objectClasses          The set of objectclasses for this
   *                                entry as a mapping between the
   *                                objectclass and the name to use to
   *                                reference it.
   * @param  userAttributes         The set of user attributes for
   *                                this entry as a mapping between
   *                                the attribute type and the list of
   *                                attributes with that type.
   * @param  operationalAttributes  The set of operational attributes
   *                                for this entry as a mapping
   *                                between the attribute type and the
   *                                list of attributes with that type.
   */
  public Entry(DN dn, Map<ObjectClass,String> objectClasses,
               Map<AttributeType,List<Attribute>> userAttributes,
               Map<AttributeType,List<Attribute>>
                    operationalAttributes)
  {


    attachment = null;
    schema     = DirectoryServer.getSchema();


    if (dn == null)
    {
      this.dn = DN.nullDN();
    }
    else
    {
      this.dn = dn;
    }

    if (objectClasses == null)
    {
      this.objectClasses = new HashMap<ObjectClass,String>();
    }
    else
    {
      this.objectClasses = objectClasses;
    }

    if (userAttributes == null)
    {
      this.userAttributes =
           new HashMap<AttributeType,List<Attribute>>();
    }
    else
    {
      this.userAttributes = userAttributes;
    }

    if (operationalAttributes == null)
    {
      this.operationalAttributes =
           new HashMap<AttributeType,List<Attribute>>();
    }
    else
    {
      this.operationalAttributes = operationalAttributes;
    }
  }



  /**
   * Retrieves the distinguished name for this entry.
   *
   * @return  The distinguished name for this entry.
   */
  public DN getDN()
  {

    return dn;
  }



  /**
   * Specifies the distinguished name for this entry.
   *
   * @param  dn  The distinguished name for this entry.
   */
  public void setDN(DN dn)
  {

    if (dn == null)
    {
      this.dn = DN.nullDN();
    }
    else
    {
      this.dn = dn;
    }

    attachment = null;
  }



  /**
   * Retrieves the set of objectclasses defined for this entry.  The
   * caller should be allowed to modify the contents of this list, but
   * if it does then it should also invalidate the attachment.
   *
   * @return  The set of objectclasses defined for this entry.
   */
  public Map<ObjectClass,String> getObjectClasses()
  {

    return objectClasses;
  }



  /**
   * Indicates whether this entry has the specified objectclass.
   *
   * @param  objectClass  The objectclass for which to make the
   *                      determination.
   *
   * @return  <CODE>true</CODE> if this entry has the specified
   *          objectclass, or <CODE>false</CODE> if not.
   */
  public boolean hasObjectClass(ObjectClass objectClass)
  {

    return objectClasses.containsKey(objectClass);
  }



  /**
   * Retrieves the structural objectclass for this entry.
   *
   * @return  The structural objectclass for this entry, or
   *          <CODE>null</CODE> if there is none for some reason.  If
   *          there are multiple structural classes in the entry, then
   *          the first will be returned.
   */
  public ObjectClass getStructuralObjectClass()
  {

    ObjectClass structuralClass = null;

    for (ObjectClass oc : objectClasses.keySet())
    {
      if (oc.getObjectClassType() == ObjectClassType.STRUCTURAL)
      {
        if (structuralClass == null)
        {
          structuralClass = oc;
        }
        else
        {
          if (oc.isDescendantOf(structuralClass))
          {
            structuralClass = oc;
          }
        }
      }
    }

    return structuralClass;
  }



  /**
   * Specifies the set of objectclasses for this entry.
   *
   * @param  objectClassNames  The values containing the names or OIDs
   *                           of the objectClasses for this entry.
   *
   * @throws  DirectoryException  If a problem occurs while attempting
   *                              to set the objectclasses for this
   *                              entry.
   */
  public void setObjectClasses(
                   Collection<AttributeValue> objectClassNames)
         throws DirectoryException
  {

    attachment = null;

    // Iterate through all the provided objectclass names and make
    // sure that they are names of valid objectclasses.
    LinkedHashMap<ObjectClass,String> ocMap =
         new LinkedHashMap<ObjectClass,String>();
    for (AttributeValue v : objectClassNames)
    {
      String name = v.getStringValue();

      String lowerName;
      try
      {
        lowerName = v.getNormalizedStringValue();
      }
      catch (Exception e)
      {
        if (debugEnabled())
        {
          debugCought(DebugLogLevel.ERROR, e);
        }

        lowerName = toLowerCase(v.getStringValue());
      }

      ObjectClass oc = DirectoryServer.getObjectClass(lowerName);
      if (oc == null)
      {
        int    msgID   = MSGID_ENTRY_ADD_UNKNOWN_OC;
        String message = getMessage(msgID, name, String.valueOf(dn));
        throw new DirectoryException(ResultCode.OBJECTCLASS_VIOLATION,
                                     message, msgID);
      }

      ocMap.put(oc, name);
    }


    // If we've gotten here, then everything is fine so put the new
    // set of objectclasses.
    objectClasses = ocMap;
  }



  /**
   * Adds the objectClass with the given name to this entry.
   *
   * @param  objectClassName  The value containing the name or OID of
   *                          the objectClass to add to this entry.
   *
   * @throws  DirectoryException  If a problem occurs while attempting
   *                              to add the objectclass to this
   *                              entry.
   */
  public void addObjectClass(AttributeValue objectClassName)
         throws DirectoryException
  {

    attachment = null;

    String name = objectClassName.getStringValue();

    String lowerName;
    try
    {
      lowerName = objectClassName.getNormalizedStringValue();
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      lowerName = toLowerCase(name);
    }

    ObjectClass oc = DirectoryServer.getObjectClass(lowerName, true);
    if (objectClasses.containsKey(oc))
    {
      int    msgID   = MSGID_ENTRY_ADD_DUPLICATE_OC;
      String message = getMessage(msgID, name, String.valueOf(dn));
      throw new DirectoryException(ResultCode.OBJECTCLASS_VIOLATION,
                                   message, msgID);
    }

    objectClasses.put(oc, name);
  }



  /**
   * Adds the provided objectClass to this entry.
   *
   * @param  oc The objectClass to add to this entry.
   *
   * @throws  DirectoryException  If a problem occurs while attempting
   *                              to add the objectclass to this
   *                              entry.
   */
  public void addObjectClass(ObjectClass oc)
         throws DirectoryException
  {

    attachment = null;

    if (objectClasses.containsKey(oc))
    {
      int    msgID   = MSGID_ENTRY_ADD_DUPLICATE_OC;
      String message = getMessage(msgID, oc.getNameOrOID(),
                                  String.valueOf(dn));
      throw new DirectoryException(ResultCode.OBJECTCLASS_VIOLATION,
                                   message, msgID);
    }

    objectClasses.put(oc, oc.getNameOrOID());
  }



  /**
   * Adds the objectclasses corresponding to the provided set of names
   * to this entry.
   *
   * @param  objectClassNames  The values containing the names or OIDs
   *                           of the objectClasses to add to this
   *                           entry.
   *
   * @throws  DirectoryException  If a problem occurs while attempting
   *                              to add the set of objectclasses to
   *                              this entry.
   */
  public void addObjectClasses(
                   Collection<AttributeValue> objectClassNames)
         throws DirectoryException
  {

    attachment = null;


    // Iterate through all the provided objectclass names and make
    // sure that they are names of valid objectclasses not already
    // assigned to the entry.
    LinkedHashMap<ObjectClass,String> tmpOCMap =
         new LinkedHashMap<ObjectClass,String>();
    for (AttributeValue v : objectClassNames)
    {
      String name = v.getStringValue();

      String lowerName;
      try
      {
        lowerName = v.getNormalizedStringValue();
      }
      catch (Exception e)
      {
        if (debugEnabled())
        {
          debugCought(DebugLogLevel.ERROR, e);
        }

        lowerName = toLowerCase(v.getStringValue());
      }

      ObjectClass oc = DirectoryServer.getObjectClass(lowerName);
      if (oc == null)
      {
        int    msgID   = MSGID_ENTRY_ADD_UNKNOWN_OC;
        String message = getMessage(msgID, name, String.valueOf(dn));
        throw new DirectoryException(ResultCode.OBJECTCLASS_VIOLATION,
                                     message, msgID);
      }

      if (objectClasses.containsKey(oc))
      {
        int    msgID   = MSGID_ENTRY_ADD_DUPLICATE_OC;
        String message = getMessage(msgID, name, String.valueOf(dn));
        throw new DirectoryException(ResultCode.OBJECTCLASS_VIOLATION,
                                     message, msgID);
      }

      if (oc.isObsolete())
      {
        int    msgID   = MSGID_ENTRY_ADD_OBSOLETE_OC;
        String message = getMessage(msgID, name, String.valueOf(dn));
        throw new DirectoryException(ResultCode.OBJECTCLASS_VIOLATION,
                                     message, msgID);
      }

      tmpOCMap.put(oc, name);
    }


    // If we've gotten here, then everything is OK, so add the new
    // classes.
    for (ObjectClass oc : tmpOCMap.keySet())
    {
      String name = tmpOCMap.get(oc);

      objectClasses.put(oc, name);
    }
  }



  /**
   * Retrieves the entire set of attributes for this entry.  This will
   * include both user and operational attributes.  The caller must
   * not modify the contents of this list.  Also note that this method
   * is less efficient than calling either (or both)
   * <CODE>getUserAttributes</CODE> or
   * <CODE>getOperationalAttributes</CODE>, so it should only be used
   * when calls to those methods are not appropriate.
   *
   * @return  The entire set of attributes for this entry.
   */
  public List<Attribute> getAttributes()
  {

    ArrayList<Attribute> attributes = new ArrayList<Attribute>();

    for (List<Attribute> list : userAttributes.values())
    {
      for (Attribute a : list)
      {
        attributes.add(a);
      }
    }

    for (List<Attribute> list : operationalAttributes.values())
    {
      for (Attribute a : list)
      {
        attributes.add(a);
      }
    }

    return attributes;
  }



  /**
   * Retrieves the entire set of user (i.e., non-operational)
   * attributes for this entry.  The caller should be allowed to
   * modify the contents of this list, but if it does then it should
   * also invalidate the attachment.
   *
   * @return  The entire set of user attributes for this entry.
   */
  public Map<AttributeType,List<Attribute>> getUserAttributes()
  {

    return userAttributes;
  }



  /**
   * Retrieves the entire set of operational attributes for this
   * entry.  The caller should be allowed to modify the contents of
   * this list, but if it does then it should also invalidate the
   * attachment.
   *
   * @return  The entire set of operational attributes for this entry.
   */
  public Map<AttributeType,List<Attribute>> getOperationalAttributes()
  {

    return operationalAttributes;
  }



  /**
   * Retrieves an attribute holding the objectclass information for
   * this entry.  The returned attribute must not be altered.
   *
   * @return  An attribute holding the objectclass information for
   *          this entry, or <CODE>null</CODE> if it does not have any
   *          objectclass information.
   */
  public Attribute getObjectClassAttribute()
  {

    if ((objectClasses == null) || objectClasses.isEmpty())
    {
      return null;
    }

    LinkedHashSet<AttributeValue> ocValues =
         new LinkedHashSet<AttributeValue>(objectClasses.size());
    for (String s : objectClasses.values())
    {
      ocValues.add(new AttributeValue(new ASN1OctetString(s),
                            new ASN1OctetString(toLowerCase(s))));
    }

    return new Attribute(
                    DirectoryServer.getObjectClassAttributeType(),
                    "objectClass", ocValues);
  }



  /**
   * Indicates whether this entry contains the specified attribute.
   *
   * @param  attributeType  The attribute type for which to make the
   *                        determination.
   *
   * @return  <CODE>true</CODE> if this entry contains the specified
   *          attribute, or <CODE>false</CODE> if not.
   */
  public boolean hasAttribute(AttributeType attributeType)
  {

    if (userAttributes.containsKey(attributeType) ||
        operationalAttributes.containsKey(attributeType))
    {
      return true;
    }

    if (attributeType.mayHaveSubordinateTypes())
    {
      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        if (userAttributes.containsKey(at) ||
            operationalAttributes.containsKey(at))
        {
          return true;
        }
      }
    }

    return (attributeType.isObjectClassType() &&
            (! objectClasses.isEmpty()));
  }



  /**
   * Indicates whether this entry contains the specified attribute
   * with all of the options in the provided set.
   *
   * @param  attributeType     The attribute type for which to make
   *                           the determination.
   * @param  attributeOptions  The set of options to use in the
   *                           determination.
   *
   * @return  <CODE>true</CODE> if this entry contains the specified
   *          attribute, or <CODE>false</CODE> if not.
   */
  public boolean hasAttribute(AttributeType attributeType,
                              Set<String> attributeOptions)
  {

    List<Attribute> attributes;
    if (attributeType.mayHaveSubordinateTypes())
    {
      attributes = new LinkedList<Attribute>();
      List<Attribute> attrs = userAttributes.get(attributeType);
      if (attrs != null)
      {
        attributes.addAll(attrs);
      }

      attrs = operationalAttributes.get(attributeType);
      if (attrs != null)
      {
        attributes.addAll(attrs);
      }

      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        attrs = userAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }

        attrs = operationalAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }
      }
    }
    else
    {
      attributes = userAttributes.get(attributeType);
      if (attributes == null)
      {
        attributes = operationalAttributes.get(attributeType);
        if (attributes == null)
        {
          if (attributeType.isObjectClassType() &&
              (! objectClasses.isEmpty()))
          {
            return ((attributeOptions == null) ||
                    attributeOptions.isEmpty());
          }
          else
          {
            return false;
          }
        }
      }
    }

    // It's possible that there could be an attribute without any
    // values, which we should treat as not having the requested
    // attribute.
    for (Attribute a : attributes)
    {
      if (a.hasValue() && a.hasOptions(attributeOptions))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Retrieves the requested attribute element(s) for the specified
   * attribute type.  The list returned may include multiple elements
   * if the same attribute exists in the entry multiple times with
   * different sets of options.
   *
   * @param  attributeType  The attribute type to retrieve.
   *
   * @return  The requested attribute element(s) for the specified
   *          attribute type, or <CODE>null</CODE> if the specified
   *          attribute type is not present in this entry.
   */
  public List<Attribute> getAttribute(AttributeType attributeType)
  {

    if (attributeType.mayHaveSubordinateTypes())
    {
      List<Attribute> attributes = new LinkedList<Attribute>();

      List<Attribute> attrs = userAttributes.get(attributeType);
      if (attrs != null)
      {
        attributes.addAll(attrs);
      }

      attrs = operationalAttributes.get(attributeType);
      if (attrs != null)
      {
        attributes.addAll(attrs);
      }

      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        attrs = userAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }

        attrs = operationalAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }
      }

      if (attributes.isEmpty())
      {
        return null;
      }
      else
      {
        return attributes;
      }
    }
    else
    {
      List<Attribute> attributes = userAttributes.get(attributeType);

      if (attributes == null)
      {
        attributes = operationalAttributes.get(attributeType);
        if (attributes == null)
        {
          if (attributeType.isObjectClassType() &&
              (! objectClasses.isEmpty()))
          {
            attributes = new ArrayList<Attribute>(1);
            attributes.add(getObjectClassAttribute());
            return attributes;
          }
          else
          {
            return null;
          }
        }
        else
        {
          return attributes;
        }
      }
      else
      {
        return attributes;
      }
    }
  }



  /**
   * Retrieves the requested attribute element(s) for the attribute
   * with the specified name or OID.  The list returned may include
   * multiple elements if the same attribute exists in the entry
   * multiple times with different sets of options.
   * <BR><BR>
   * Note that this method should only be used in cases in which the
   * Directory Server schema has no reference of an attribute type
   * with the specified name.  It is not as accurate or efficient as
   * the version of this method that takes an
   * <CODE>AttributeType</CODE> argument.
   *
   * @param  lowerName  The name or OID of the attribute to return,
   *                    formatted in all lowercase characters.
   *
   * @return  The requested attribute element(s) for the specified
   *          attribute type, or <CODE>null</CODE> if the specified
   *          attribute type is not present in this entry.
   */
  public List<Attribute> getAttribute(String lowerName)
  {

    for (AttributeType attr : userAttributes.keySet())
    {
      if (attr.hasNameOrOID(lowerName))
      {
        return getAttribute(attr);
      }
    }

    for (AttributeType attr : operationalAttributes.keySet())
    {
      if (attr.hasNameOrOID(lowerName))
      {
        return getAttribute(attr);
      }
    }

    if (lowerName.equals(OBJECTCLASS_ATTRIBUTE_TYPE_NAME) &&
        (! objectClasses.isEmpty()))
    {
      LinkedList<Attribute> attrList = new LinkedList<Attribute>();
      attrList.add(getObjectClassAttribute());
      return attrList;
    }

    return null;
  }



  /**
   * Retrieves the requested attribute element(s) for the specified
   * attribute type.  The list returned may include multiple elements
   * if the same attribute exists in the entry multiple times with
   * different sets of options.
   *
   * @param  attributeType  The attribute type to retrieve.
   * @param  options        The set of attribute options to include in
   *                        matching elements.
   *
   * @return  The requested attribute element(s) for the specified
   *          attribute type, or <CODE>null</CODE> if the specified
   *          attribute type is not present in this entry with the
   *          provided set of options.
   */
  public List<Attribute> getAttribute(AttributeType attributeType,
                                      Set<String> options)
  {

    List<Attribute> attributes = new LinkedList<Attribute>();
    if (attributeType.mayHaveSubordinateTypes())
    {
      List<Attribute> attrs = userAttributes.get(attributeType);
      if (attrs != null)
      {
        attributes.addAll(attrs);
      }

      attrs = operationalAttributes.get(attributeType);
      if (attrs != null)
      {
        attributes.addAll(attrs);
      }

      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        attrs = userAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }

        attrs = operationalAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }
      }
    }
    else
    {
      List<Attribute> attrs = userAttributes.get(attributeType);
      if (attrs == null)
      {
        attrs = operationalAttributes.get(attributeType);
        if (attrs == null)
        {
          if (attributeType.isObjectClassType() &&
              (! objectClasses.isEmpty()) &&
              ((options == null) || options.isEmpty()))
          {
            attributes.add(getObjectClassAttribute());
            return attributes;
          }
          else
          {
            return null;
          }
        }
        else
        {
          attributes.addAll(attrs);
        }
      }
      else
      {
        attributes.addAll(attrs);
      }
    }


    Iterator<Attribute> iterator = attributes.iterator();
    while (iterator.hasNext())
    {
      Attribute a = iterator.next();
      if (! a.hasOptions(options))
      {
        iterator.remove();
      }
    }

    if (attributes.isEmpty())
    {
      return null;
    }
    else
    {
      return attributes;
    }
  }



  /**
   * Retrieves the requested attribute element(s) for the attribute
   * with the specified name or OID and set of options.  The list
   * returned may include multiple elements if the same attribute
   * exists in the entry multiple times with different sets of
   * matching options.
   * <BR><BR>
   * Note that this method should only be used in cases in which the
   * Directory Server schema has no reference of an attribute type
   * with the specified name.  It is not as accurate or efficient as
   * the version of this method that takes an
   * <CODE>AttributeType</CODE> argument.
   *
   * @param  lowerName  The name or OID of the attribute to return,
   *                    formatted in all lowercase characters.
   * @param  options    The set of attribute options to include in
   *                    matching elements.
   *
   * @return  The requested attribute element(s) for the specified
   *          attribute type, or <CODE>null</CODE> if the specified
   *          attribute type is not present in this entry.
   */
  public List<Attribute> getAttribute(String lowerName,
                                      Set<String> options)
  {

    for (AttributeType attr : userAttributes.keySet())
    {
      if (attr.hasNameOrOID(lowerName))
      {
        return getAttribute(attr, options);
      }
    }

    for (AttributeType attr : operationalAttributes.keySet())
    {
      if (attr.hasNameOrOID(lowerName))
      {
        return getAttribute(attr, options);
      }
    }

    if (lowerName.equals(OBJECTCLASS_ATTRIBUTE_TYPE_NAME) &&
        ((options == null) || options.isEmpty()))
    {
      LinkedList<Attribute> attributes = new LinkedList<Attribute>();
      attributes.add(getObjectClassAttribute());
      return attributes;
    }
    else
    {
      return null;
    }
  }



  /**
   * Retrieves the requested attribute type from the entry and decodes
   * a single value as an object of type T.
   * <p>
   * If the requested attribute type is not present then
   * <code>null</code> is returned. If more than one attribute value
   * is present, then the first value found will be decoded and
   * returned.
   * <p>
   * The attribute value is decoded using the specified
   * {@link org.opends.server.api.AttributeValueDecoder}.
   *
   * @param <T>
   *          Decode the attribute value to an object of this type.
   * @param attributeType
   *          The attribute type to retrieve.
   * @param decoder
   *          The attribute value decoder.
   * @return The decoded attribute value or <code>null</code> if no
   *         attribute value having the specified attribute type was
   *         found.
   * @throws DirectoryException
   *           If the requested attribute value could not be decoded
   *           successfully.
   */
  public final <T> T getAttributeValue(AttributeType attributeType,
      AttributeValueDecoder<T> decoder) throws DirectoryException
  {

    List<Attribute> attributes = getAttribute(attributeType);
    AttributeValueIterable values =
         new AttributeValueIterable(attributes);
    Iterator<AttributeValue> iterator = values.iterator();

    if (iterator.hasNext())
    {
      return decoder.decode(iterator.next());
    }
    else
    {
      return null;
    }
  }



  /**
   * Retrieves the requested attribute type from the entry and decodes
   * any values as objects of type T and then places them in the
   * specified collection.
   * <p>
   * If the requested attribute type is not present then no decoded
   * values will be added to the container.
   * <p>
   * The attribute value is decoded using the specified
   * {@link org.opends.server.api.AttributeValueDecoder}.
   *
   * @param <T>
   *          Decode the attribute values to objects of this type.
   * @param attributeType
   *          The attribute type to retrieve.
   * @param decoder
   *          The attribute value decoder.
   * @param collection
   *          The collection to which decoded values should be added.
   * @return The collection containing the decoded attribute value.
   * @throws DirectoryException
   *           If one or more of the requested attribute values could
   *           not be decoded successfully.
   */
  public final <T> Collection<T> getAttributeValues(
      AttributeType attributeType,
      AttributeValueDecoder<? extends T> decoder,
      Collection<T> collection)
      throws DirectoryException
  {

    List<Attribute> attributes = getAttribute(attributeType);
    AttributeValueIterable values =
         new AttributeValueIterable(attributes);

    for (AttributeValue value : values)
    {
      collection.add(decoder.decode(value));
    }

    return collection;
  }



  /**
   * Indicates whether this entry contains the specified user
   * attribute.
   *
   * @param attributeType
   *          The attribute type for which to make the determination.
   * @return <CODE>true</CODE> if this entry contains the specified
   *         user attribute, or <CODE>false</CODE> if not.
   */
  public boolean hasUserAttribute(AttributeType attributeType)
  {

    if (userAttributes.containsKey(attributeType))
    {
      return true;
    }

    if (attributeType.mayHaveSubordinateTypes())
    {
      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        if (userAttributes.containsKey(at))
        {
          return true;
        }
      }
    }

    return false;
  }



  /**
   * Retrieves the requested user attribute element(s) for the
   * specified attribute type.  The list returned may include multiple
   * elements if the same attribute exists in the entry multiple times
   * with different sets of options.
   *
   * @param  attributeType  The attribute type to retrieve.
   *
   * @return  The requested attribute element(s) for the specified
   *          attribute type, or <CODE>null</CODE> if there is no such
   *          user attribute.
   */
  public List<Attribute> getUserAttribute(AttributeType attributeType)
  {

    if (attributeType.mayHaveSubordinateTypes())
    {
      LinkedList<Attribute> attributes = new LinkedList<Attribute>();

      List<Attribute> attrs = userAttributes.get(attributeType);
      if (attrs != null)
      {
        attributes.addAll(attrs);
      }

      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        attrs = userAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }
      }

      if (attributes.isEmpty())
      {
        return null;
      }
      else
      {
        return attributes;
      }
    }
    else
    {
      return userAttributes.get(attributeType);
    }
  }



  /**
   * Retrieves the requested user attribute element(s) for the
   * specified attribute type.  The list returned may include multiple
   * elements if the same attribute exists in the entry multiple times
   * with different sets of options.
   *
   * @param  attributeType  The attribute type to retrieve.
   * @param  options        The set of attribute options to include in
   *                        matching elements.
   *
   * @return  The requested attribute element(s) for the specified
   *          attribute type, or <CODE>null</CODE> if there is no such
   *          user attribute with the specified set of options.
   */
  public List<Attribute> getUserAttribute(AttributeType attributeType,
                                          Set<String> options)
  {

    LinkedList<Attribute> attributes = new LinkedList<Attribute>();
    List<Attribute> attrs = userAttributes.get(attributeType);
    if (attrs != null)
    {
      attributes.addAll(attrs);
    }

    if (attributeType.mayHaveSubordinateTypes())
    {
      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        attrs = userAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }
      }
    }

    Iterator<Attribute> iterator = attributes.iterator();
    while (iterator.hasNext())
    {
      Attribute a = iterator.next();
      if (! a.hasOptions(options))
      {
        iterator.remove();
      }
    }

    if (attributes.isEmpty())
    {
      return null;
    }
    else
    {
      return attributes;
    }
  }



  /**
   * Retrieves a duplicate of the user attribute list for the
   * specified type.
   *
   * @param  attributeType  The attribute type for which to retrieve a
   *                        duplicate attribute list.
   *
   * @return  A duplicate of the requested attribute list, or
   *          <CODE>null</CODE> if there is no such user attribute.
   */
  public List<Attribute> duplicateUserAttribute(
                             AttributeType attributeType)
  {

    LinkedList<Attribute> attributes = new LinkedList<Attribute>();

    List<Attribute> attrs = userAttributes.get(attributeType);
    if (attrs != null)
    {
      for (Attribute a : attrs)
      {
        attributes.add(a.duplicate());
      }
    }

    if (attributeType.mayHaveSubordinateTypes())
    {
      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        attrs = userAttributes.get(at);
        if (attrs != null)
        {
          for (Attribute a : attrs)
          {
            attributes.add(a.duplicate());
          }
        }
      }
    }

    if (attributes.isEmpty())
    {
      return null;
    }
    else
    {
      return attributes;
    }
  }



  /**
   * Makes a copy of attributes matching the specified options.
   *
   * @param  attrList       The attributes to be copied.
   * @param  options        The set of attribute options to include in
   *                        matching elements.
   * @param  omitValues     <CODE>true</CODE> if the values are to be
   *                        omitted.
   *
   * @return  A copy of the attributes matching the specified options,
   *          or <CODE>null</CODE> if there is no such attribute with
   *          the specified set of options.
   */
  private static List<Attribute> duplicateAttribute(
       List<Attribute> attrList,
       Set<String> options,
       boolean omitValues)
  {

    if (attrList == null)
    {
      return null;
    }

    ArrayList<Attribute> duplicateList =
         new ArrayList<Attribute>(attrList.size());
    for (Attribute a : attrList)
    {
      if (a.hasOptions(options))
      {
        duplicateList.add(a.duplicate(omitValues));
      }
    }

    if (duplicateList.isEmpty())
    {
      return null;
    }
    else
    {
      return duplicateList;
    }
  }



  /**
   * Retrieves a copy of the requested user attribute element(s) for
   * the specified attribute type.  The list returned may include
   * multiple elements if the same attribute exists in the entry
   * multiple times with different sets of options.
   *
   * @param  attributeType  The attribute type to retrieve.
   * @param  options        The set of attribute options to include in
   *                        matching elements.
   * @param  omitValues     <CODE>true</CODE> if the values are to be
   *                        omitted.
   *
   * @return  A copy of the requested attribute element(s) for the
   *          specified attribute type, or <CODE>null</CODE> if there
   *          is no such user attribute with the specified set of
   *          options.
   */
  public List<Attribute> duplicateUserAttribute(
       AttributeType attributeType,
       Set<String> options,
       boolean omitValues)
  {

    List<Attribute> currentList = getUserAttribute(attributeType);
    return duplicateAttribute(currentList, options, omitValues);
  }



  /**
   * Retrieves a copy of the requested operational attribute
   * element(s) for the specified attribute type.  The list returned
   * may include multiple elements if the same attribute exists in
   * the entry multiple times with different sets of options.
   *
   * @param  attributeType  The attribute type to retrieve.
   * @param  options        The set of attribute options to include in
   *                        matching elements.
   * @param  omitValues     <CODE>true</CODE> if the values are to be
   *                        omitted.
   *
   * @return  A copy of the requested attribute element(s) for the
   *          specified attribute type, or <CODE>null</CODE> if there
   *          is no such user attribute with the specified set of
   *          options.
   */
  public List<Attribute> duplicateOperationalAttribute(
       AttributeType attributeType,
       Set<String> options,
       boolean omitValues)
  {

    List<Attribute> currentList =
         getOperationalAttribute(attributeType);
    return duplicateAttribute(currentList, options, omitValues);
  }


  /**
   * Indicates whether this entry contains the specified operational
   * attribute.
   *
   * @param  attributeType  The attribute type for which to make the
   *                        determination.
   *
   * @return  <CODE>true</CODE> if this entry contains the specified
   *          operational attribute, or <CODE>false</CODE> if not.
   */
  public boolean hasOperationalAttribute(AttributeType attributeType)
  {

    if (operationalAttributes.containsKey(attributeType))
    {
      return true;
    }

    if (attributeType.mayHaveSubordinateTypes())
    {
      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        if (operationalAttributes.containsKey(at))
        {
          return true;
        }
      }
    }

    return false;
  }



  /**
   * Retrieves the requested operational attribute element(s) for the
   * specified attribute type.  The list returned may include multiple
   * elements if the same attribute exists in the entry multiple times
   * with different sets of options.
   *
   * @param  attributeType  The attribute type to retrieve.
   *
   * @return  The requested attribute element(s) for the specified
   *          attribute type, or <CODE>null</CODE> if there is no such
   *          operational attribute.
   */
  public List<Attribute> getOperationalAttribute(
                              AttributeType attributeType)
  {

    if (attributeType.mayHaveSubordinateTypes())
    {
      LinkedList<Attribute> attributes = new LinkedList<Attribute>();

      List<Attribute> attrs =
           operationalAttributes.get(attributeType);
      if (attrs != null)
      {
        attributes.addAll(attrs);
      }

      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        attrs = operationalAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }
      }

      if (attributes.isEmpty())
      {
        return null;
      }
      else
      {
        return attributes;
      }
    }
    else
    {
      return operationalAttributes.get(attributeType);
    }
  }



  /**
   * Retrieves the requested operational attribute element(s) for the
   * specified attribute type.  The list returned may include multiple
   * elements if the same attribute exists in the entry multiple times
   * with different sets of options.
   *
   * @param  attributeType  The attribute type to retrieve.
   * @param  options        The set of attribute options to include in
   *                        matching elements.
   *
   * @return  The requested attribute element(s) for the specified
   *          attribute type, or <CODE>null</CODE> if there is no such
   *          operational attribute with the specified set of options.
   */
  public List<Attribute> getOperationalAttribute(
                              AttributeType attributeType,
                              Set<String> options)
  {

    LinkedList<Attribute> attributes = new LinkedList<Attribute>();
    List<Attribute> attrs = operationalAttributes.get(attributeType);
    if (attrs != null)
    {
      attributes.addAll(attrs);
    }

    if (attributeType.mayHaveSubordinateTypes())
    {
      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        attrs = operationalAttributes.get(at);
        if (attrs != null)
        {
          attributes.addAll(attrs);
        }
      }
    }

    Iterator<Attribute> iterator = attributes.iterator();
    while (iterator.hasNext())
    {
      Attribute a = iterator.next();
      if (! a.hasOptions(options))
      {
        iterator.remove();
      }
    }

    if (attributes.isEmpty())
    {
      return null;
    }
    else
    {
      return attributes;
    }
  }



  /**
   * Retrieves a duplicate of the operational attribute list for the
   * specified type.
   *
   * @param  attributeType  The attribute type for which to retrieve a
   *                        duplicate attribute list.
   *
   * @return  A duplicate of the requested attribute list, or
   *          <CODE>null</CODE> if there is no such operational
   *          attribute.
   */
  public List<Attribute> duplicateOperationalAttribute(
                              AttributeType attributeType)
  {

    LinkedList<Attribute> attributes = new LinkedList<Attribute>();

    List<Attribute> attrs = operationalAttributes.get(attributeType);
    if (attrs != null)
    {
      for (Attribute a : attrs)
      {
        attributes.add(a.duplicate());
      }
    }

    if (attributeType.mayHaveSubordinateTypes())
    {
      for (AttributeType at : schema.getSubTypes(attributeType))
      {
        attrs = operationalAttributes.get(at);
        if (attrs != null)
        {
          for (Attribute a : attrs)
          {
            attributes.add(a.duplicate());
          }
        }
      }
    }

    if (attributes.isEmpty())
    {
      return null;
    }
    else
    {
      return attributes;
    }
  }


  /**
   * Puts the provided attribute in this entry.  If an attribute
   * already exists with the provided type, it will be overwritten.
   * Otherwise, a new attribute will be added.  Note that no
   * validation will be performed.
   *
   * @param  attributeType  The attribute type for the set of
   *                        attributes to add.
   * @param  attributeList  The set of attributes to add for the given
   *                        type.
   */
  public void putAttribute(AttributeType attributeType,
                           List<Attribute> attributeList)
  {


    attachment = null;


    // See if there is already a set of attributes with the specified
    // type.  If so, then overwrite it.
    List<Attribute> attrList = userAttributes.get(attributeType);
    if (attrList != null)
    {
      userAttributes.put(attributeType, attributeList);
      return;
    }

    attrList = operationalAttributes.get(attributeType);
    if (attrList != null)
    {
      operationalAttributes.put(attributeType, attributeList);
      return;
    }


    // This is a new attribute, so add it to the set of user or
    // operational attributes as appropriate.
    if (attributeType.isOperational())
    {
      operationalAttributes.put(attributeType, attributeList);
    }
    else
    {
      userAttributes.put(attributeType, attributeList);
    }
  }



  /**
   * Adds the provided attribute to this entry.  If an attribute with
   * the provided type already exists, then the values will be merged.
   *
   * @param  attribute        The attribute to add or merge with this
   *                          entry.
   * @param  duplicateValues  A list to which any duplicate values
   *                          will be added.
   */
  public void addAttribute(Attribute attribute,
                           List<AttributeValue> duplicateValues)
  {

    attachment = null;

    List<Attribute> attrList =
         getAttribute(attribute.getAttributeType());
    if (attrList == null)
    {
      // There are no instances of the specified attribute in this
      // entry, so simply add it.
      attrList = new ArrayList<Attribute>(1);
      attrList.add(attribute);

      AttributeType attrType = attribute.getAttributeType();
      if (attrType.isOperational())
      {
        operationalAttributes.put(attrType, attrList);
      }
      else
      {
        userAttributes.put(attrType, attrList);
      }

      return;
    }
    else
    {
      // There are some instances of this attribute, but they may not
      // have exactly the same set of options.  See if we can find an
      // attribute with the same set of options to merge in the
      // values.  If not, then add the new attribute to the list.
      HashSet<String> options = attribute.getOptions();
      for (Attribute a : attrList)
      {
        if (a.optionsEqual(options))
        {
          // There is an attribute with the same set of options.
          // Merge the value lists together.
          LinkedHashSet<AttributeValue> existingValues =
               a.getValues();
          LinkedHashSet<AttributeValue> newValues =
               attribute.getValues();
          for (AttributeValue v : newValues)
          {
            if (! existingValues.add(v))
            {
              duplicateValues.add(v);
            }
          }

          return;
        }
      }

      attrList.add(attribute);
    }
  }



  /**
   * Removes all instances of the specified attribute type from this
   * entry.  If the provided attribute type is the objectclass type,
   * then all objectclass values will be removed (but must be replaced
   * for the entry to be valid).  If the specified attribute type is
   * not present in this entry, then this method will have no effect.
   *
   * @param  attributeType  The attribute type for the attribute to
   *                        remove from this entry.
   *
   * @return  <CODE>true</CODE> if the attribute was found and
   *          removed, or <CODE>false</CODE> if it was not present in
   *          the entry.
   */
  public boolean removeAttribute(AttributeType attributeType)
  {

    attachment = null;

    if (attributeType.isObjectClassType())
    {
      objectClasses.clear();
      return true;
    }
    else
    {
      return ((userAttributes.remove(attributeType) != null) ||
              (operationalAttributes.remove(attributeType) != null));
    }
  }



  /**
   * Removes the attribute with the provided type and set of options
   * from this entry.  If the provided set of options is
   * <CODE>null</CODE> or empty, then all occurrences of the specified
   * attribute will be removed.  Otherwise, only the instance with the
   * exact set of options provided will be removed.  This has no
   * effect if the specified attribute is not present in this entry
   * with the given set of options.
   *
   * @param  attributeType  The attribute type for the attribute to
   *                        remove from this entry.
   * @param  options        The set of attribute options to use when
   *                        determining which attribute to remove.
   *
   * @return  <CODE>true</CODE> if the attribute was found and
   *          removed, or <CODE>false</CODE> if it was not present in
   *          the entry.
   */
  public boolean removeAttribute(AttributeType attributeType,
                                 Set<String> options)
  {

    attachment = null;

    if ((options == null) || options.isEmpty())
    {
      return removeAttribute(attributeType);
    }
    else
    {
      List<Attribute> attrList = userAttributes.get(attributeType);
      if (attrList == null)
      {
        attrList = operationalAttributes.get(attributeType);
        if (attrList == null)
        {
          return false;
        }
      }

      boolean removed = false;

      Iterator<Attribute> iterator = attrList.iterator();
      while (iterator.hasNext())
      {
        Attribute a = iterator.next();
        if (a.optionsEqual(options))
        {
          iterator.remove();
          removed = true;
          break;
        }
      }

      if (attrList.isEmpty())
      {
        userAttributes.remove(attributeType);
        operationalAttributes.remove(attributeType);
      }

      return removed;
    }
  }



  /**
   * Removes the provided attribute from this entry.  If the given
   * attribute does not have any values, then all values of the
   * associated attribute type (taking into account the options in the
   * provided type) will be removed.  Otherwise, only the specified
   * values will be removed.
   *
   * @param  attribute      The attribute containing the information
   *                        to use to perform the removal.
   * @param  missingValues  A list to which any values contained in
   *                        the provided attribute but not present in
   *                        the entry will be added.
   *
   * @return  <CODE>true</CODE> if the attribute type was present and
   *          the specified values that were present were removed, or
   *          <CODE>false</CODE> if the attribute type was not present
   *          in the entry.  If the attribute type was present but
   *          only contained some of the values in the provided
   *          attribute, then this method will return
   *          <CODE>true</CODE> but will add those values to the
   *          provided list.
   */
  public boolean removeAttribute(Attribute attribute,
                                 List<AttributeValue> missingValues)
  {


    attachment = null;


    if (attribute.getAttributeType().isObjectClassType())
    {
      LinkedHashSet<AttributeValue> valueSet = attribute.getValues();
      if ((valueSet == null) || valueSet.isEmpty())
      {
        objectClasses.clear();
        return true;
      }

      boolean allSuccessful = true;

      for (AttributeValue v : attribute.getValues())
      {
        String ocName;
        try
        {
          ocName = v.getNormalizedStringValue();
        }
        catch (Exception e)
        {
          if (debugEnabled())
          {
            debugCought(DebugLogLevel.ERROR, e);
          }

          ocName = toLowerCase(v.getStringValue());
        }

        boolean matchFound = false;

        for (ObjectClass oc : objectClasses.keySet())
        {
          if (oc.hasNameOrOID(ocName))
          {
            matchFound = true;
            objectClasses.remove(oc);
            break;
          }
        }

        if (! matchFound)
        {
          allSuccessful = false;
          missingValues.add(v);
        }
      }

      return allSuccessful;
    }


    if (attribute.hasOptions())
    {
      HashSet<String> options = attribute.getOptions();

      LinkedHashSet<AttributeValue> valueSet = attribute.getValues();
      if ((valueSet == null) || valueSet.isEmpty())
      {
        return removeAttribute(attribute.getAttributeType(), options);
      }

      List<Attribute> attrList =
           getAttribute(attribute.getAttributeType());
      if (attrList == null)
      {
        return false;
      }

      for (Attribute a : attrList)
      {
        if (a.optionsEqual(options))
        {
          LinkedHashSet<AttributeValue> existingValueSet =
               a.getValues();

          for (AttributeValue v : valueSet)
          {
            if (! existingValueSet.remove(v))
            {
              missingValues.add(v);
            }
          }

          if (existingValueSet.isEmpty())
          {
            return removeAttribute(attribute.getAttributeType(),
                                   options);
          }

          return true;
        }
      }

      return false;
    }
    else
    {
      LinkedHashSet<AttributeValue> valueSet = attribute.getValues();
      if ((valueSet == null) || valueSet.isEmpty())
      {
        return removeAttribute(attribute.getAttributeType());
      }

      List<Attribute> attrList =
           getAttribute(attribute.getAttributeType());
      if (attrList == null)
      {
        return false;
      }

      for (Attribute a : attrList)
      {
        if (! a.hasOptions())
        {
          LinkedHashSet<AttributeValue> existingValueSet =
               a.getValues();

          for (AttributeValue v : valueSet)
          {
            if (! existingValueSet.remove(v))
            {
              missingValues.add(v);
            }
          }

          if (existingValueSet.isEmpty())
          {
            return removeAttribute(attribute.getAttributeType());
          }

          return true;
        }
      }

      return false;
    }
  }



  /**
   * Indicates whether the specified attribute type is allowed by any
   * of the objectclasses associated with this entry.
   *
   * @param  attributeType  The attribute type for which to make the
   *                        determination.
   *
   * @return  <CODE>true</CODE> if the specified attribute is allowed
   *          by any of the objectclasses associated with this entry,
   *          or <CODE>false</CODE> if it is not.
   */
  public boolean allowsAttribute(AttributeType attributeType)
  {

    for (ObjectClass o : objectClasses.keySet())
    {
      if (o.isRequiredOrOptional(attributeType))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Indicates whether the specified attribute type is required by any
   * of the objectclasses associated with this entry.
   *
   * @param  attributeType  The attribute type for which to make the
   *                        determination.
   *
   * @return  <CODE>true</CODE> if the specified attribute is required
   *          by any of the objectclasses associated with this entry,
   *          o r<CODE>false</CODE> if it is not.
   */
  public boolean requiresAttribute(AttributeType attributeType)
  {

    for (ObjectClass o : objectClasses.keySet())
    {
      if (o.isRequired(attributeType))
      {
        return true;
      }
    }

    return false;
  }



  /**
   * Indicates whether this entry contains the specified attribute
   * value.
   *
   * @param  attributeType  The attribute type for the attribute.
   * @param  options        The set of options for the attribute.
   * @param  value          The value for the attribute.
   *
   * @return  <CODE>true</CODE> if this entry contains the specified
   *          attribute value, or <CODE>false</CODE> if it does not.
   */
  public boolean hasValue(AttributeType attributeType,
                          Set<String> options, AttributeValue value)
  {

    List<Attribute> attrList = getAttribute(attributeType);
    if ((attrList == null) || attrList.isEmpty())
    {
      return false;
    }

    for (Attribute a : attrList)
    {
      if (a.optionsEqual(options))
      {
        return a.hasValue(value);
      }
    }

    return false;
  }



  /**
   * Applies the provided modification to this entry.  No schema
   * checking will be performed.
   *
   * @param  mod  The modification to apply to this entry.
   *
   * @throws  DirectoryException  If a problem occurs while attempting
   *                              to apply the modification.  Note
   *                              that even if a problem occurs, then
   *                              the entry may have been altered in
   *                              some way.
   */
  public void applyModification(Modification mod)
         throws DirectoryException
  {

    Attribute     a = mod.getAttribute();
    AttributeType t = a.getAttributeType();

    // We'll need to handle changes to the objectclass attribute in a
    // special way.
    if (t.isObjectClassType())
    {
      LinkedHashMap<ObjectClass,String> ocs = new
             LinkedHashMap<ObjectClass,String>();
      for (AttributeValue v : a.getValues())
      {
        String ocName    = v.getStringValue();
        String lowerName = toLowerCase(ocName);
        ObjectClass oc   =
             DirectoryServer.getObjectClass(lowerName, true);
        ocs.put(oc, ocName);
      }

      switch (mod.getModificationType())
      {
        case ADD:
          for (ObjectClass oc : ocs.keySet())
          {
            if (objectClasses.containsKey(oc))
            {
              int    msgID   = MSGID_ENTRY_DUPLICATE_VALUES;
              String message = getMessage(msgID, a.getName());
              throw new DirectoryException(
                             ResultCode.ATTRIBUTE_OR_VALUE_EXISTS,
                             message, msgID);
            }
            else
            {
              objectClasses.put(oc, ocs.get(oc));
            }
          }
          break;

        case DELETE:
          for (ObjectClass oc : ocs.keySet())
          {
            if (objectClasses.remove(oc) == null)
            {
              int    msgID   = MSGID_ENTRY_NO_SUCH_VALUE;
              String message = getMessage(msgID, a.getName());
              throw new DirectoryException(
                             ResultCode.NO_SUCH_ATTRIBUTE, message,
                             msgID);
            }
          }
          break;

        case REPLACE:
          objectClasses = ocs;
          break;

        case INCREMENT:
          int msgID = MSGID_ENTRY_OC_INCREMENT_NOT_SUPPORTED;
          String message = getMessage(msgID);
          throw new DirectoryException(
                         ResultCode.UNWILLING_TO_PERFORM, message,
                         msgID);

        default:
          msgID   = MSGID_ENTRY_UNKNOWN_MODIFICATION_TYPE;
          message = getMessage(msgID,
                         String.valueOf(mod.getModificationType()));
          throw new DirectoryException(
                         ResultCode.UNWILLING_TO_PERFORM, message,
                         msgID);
      }

      return;
    }

    switch (mod.getModificationType())
    {
      case ADD:
        LinkedList<AttributeValue> duplicateValues =
             new LinkedList<AttributeValue>();
        addAttribute(a, duplicateValues);
        if (! duplicateValues.isEmpty())
        {
          int    msgID   = MSGID_ENTRY_DUPLICATE_VALUES;
          String message = getMessage(msgID, a.getName());
          throw new DirectoryException(
                         ResultCode.ATTRIBUTE_OR_VALUE_EXISTS,
                         message, msgID);
        }
        break;

      case DELETE:
        LinkedList<AttributeValue> missingValues =
             new LinkedList<AttributeValue>();
        removeAttribute(a, missingValues);
        if (! missingValues.isEmpty())
        {
          int    msgID   = MSGID_ENTRY_NO_SUCH_VALUE;
          String message = getMessage(msgID, a.getName());
          throw new DirectoryException(ResultCode.NO_SUCH_ATTRIBUTE,
                                       message, msgID);
        }
        break;

      case REPLACE:
        removeAttribute(t, a.getOptions());

        if (a.hasValue())
        {
          // We know that we won't have any duplicate values, so  we
          // don't kneed to worry about checking for them.
          duplicateValues = new LinkedList<AttributeValue>();
          addAttribute(a, duplicateValues);
        }
        break;

      case INCREMENT:
        List<Attribute> attrList = getAttribute(t);
        if ((attrList == null) || attrList.isEmpty())
        {
          int    msgID   = MSGID_ENTRY_INCREMENT_NO_SUCH_ATTRIBUTE;
          String message = getMessage(msgID, a.getName());
          throw new DirectoryException(ResultCode.NO_SUCH_ATTRIBUTE,
                                       message, msgID);
        }
        else if (attrList.size() != 1)
        {
          int    msgID   = MSGID_ENTRY_INCREMENT_MULTIPLE_VALUES;
          String message = getMessage(msgID, a.getName());
          throw new DirectoryException(
                         ResultCode.CONSTRAINT_VIOLATION, message,
                         msgID);
        }

        LinkedHashSet<AttributeValue> values =
             attrList.get(0).getValues();
        if (values.isEmpty())
        {
          int    msgID   = MSGID_ENTRY_INCREMENT_NO_SUCH_ATTRIBUTE;
          String message = getMessage(msgID, a.getName());
          throw new DirectoryException(ResultCode.NO_SUCH_ATTRIBUTE,
                                       message, msgID);
        }
        else if (values.size() > 1)
        {
          int    msgID   = MSGID_ENTRY_INCREMENT_MULTIPLE_VALUES;
          String message = getMessage(msgID, a.getName());
          throw new DirectoryException(
                         ResultCode.CONSTRAINT_VIOLATION, message,
                         msgID);
        }

        LinkedHashSet<AttributeValue> newValues = a.getValues();
        if (newValues.size() != 1)
        {
          int msgID = MSGID_ENTRY_INCREMENT_INVALID_VALUE_COUNT;
          String message = getMessage(msgID);
          throw new DirectoryException(
                         ResultCode.CONSTRAINT_VIOLATION, message,
                         msgID);
        }

        long newValue;
        try
        {
          String s = values.iterator().next().getStringValue();
          long currentValue = Long.parseLong(s);

          s = a.getValues().iterator().next().getStringValue();
          long increment = Long.parseLong(s);

          newValue = currentValue+increment;
        }
        catch (NumberFormatException nfe)
        {
          int msgID = MSGID_ENTRY_INCREMENT_CANNOT_PARSE_AS_INT;
          String message = getMessage(msgID);
          throw new DirectoryException(
                         ResultCode.CONSTRAINT_VIOLATION, message,
                         msgID);
        }

        values.clear();
        values.add(new AttributeValue(t, String.valueOf(newValue)));
        break;

      default:
        int    msgID   = MSGID_ENTRY_UNKNOWN_MODIFICATION_TYPE;
        String message =
             getMessage(msgID,
                        String.valueOf(mod.getModificationType()));
        throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM,
                                     message, msgID);
    }
  }



  /**
   * Applies all of the provided modifications to this entry.
   *
   * @param  mods  The modifications to apply to this entry.
   *
   * @throws  DirectoryException  If a problem occurs while attempting
   *                              to apply the modifications.  Note
   *                              that even if a problem occurs, then
   *                              the entry may have been altered in
   *                              some way.
   */
  public void applyModifications(List<Modification> mods)
         throws DirectoryException
  {

    for (Modification m : mods)
    {
      applyModification(m);
    }
  }



  /**
   * Indicates whether this entry conforms to the server's schema
   * requirements.  The checks performed by this method include:
   *
   * <UL>
   *   <LI>Make sure that all required attributes are present, either
   *       in the list of user or operational attributes.</LI>
   *   <LI>Make sure that all user attributes are allowed by at least
   *       one of the objectclasses.  The operational attributes will
   *       not be checked in this manner.</LI>
   *   <LI>Make sure that all single-valued attributes contained in
   *       the entry have only a single value.</LI>
   *   <LI>Make sure that the entry contains a single structural
   *       objectclass.</LI>
   *   <LI>Make sure that the entry complies with any defined name
   *       forms, DIT content rules, and DIT structure rules.</LI>
   * </UL>
   *
   * @param  parentEntry             The entry that is the immediate
   *                                 parent of this entry, which may
   *                                 be checked for DIT structure rule
   *                                 conformance.  This may be
   *                                 {@code null} if there is no
   *                                 parent or if it is unavailable
   *                                to the caller.
   * @param  parentProvided          Indicates whether the caller
   *                                 attempted to provide the parent.
   *                                 If not, then the parent entry
   *                                 will be loaded on demand if it is
   *                                 required.
   * @param  validateNameForms       Indicates whether to validate the
   *                                 entry against name form
   *                                 definitions.  This should only be
   *                                 {@code true} for add and modify
   *                                 DN operations, as well as for
   *                                 for imports.
   * @param  validateStructureRules  Indicates whether to validate the
   *                                 entry against DIT structure rule
   *                                 definitions.  This should only
   *                                 be {@code true} for add and
   *                                 modify DN operations.
   * @param  invalidReason           The buffer to which an
   *                                 explanation will be appended if
   *                                 this entry does not conform to
   *                                 the server's schema
   *                                 configuration.
   *
   * @return  {@code true} if this entry conforms to the server's
   *          schema requirements, or {@code false} if it does not.
   */
  public boolean conformsToSchema(Entry parentEntry,
                                  boolean parentProvided,
                                  boolean validateNameForms,
                                  boolean validateStructureRules,
                                  StringBuilder invalidReason)
  {


    // Get the structural objectclass for the entry.  If there isn't
    // one, or if there's more than one, then see if that's OK.
    AcceptRejectWarn structuralPolicy =
         DirectoryServer.getSingleStructuralObjectClassPolicy();
    ObjectClass structuralClass = null;
    boolean multipleOCErrorLogged = false;
    for (ObjectClass oc : objectClasses.keySet())
    {
      if (oc.getObjectClassType() == ObjectClassType.STRUCTURAL)
      {
        if ((structuralClass == null) ||
            oc.isDescendantOf(structuralClass))
        {
          structuralClass = oc;
        }
        else if (! structuralClass.isDescendantOf(oc))
        {
          int msgID = MSGID_ENTRY_SCHEMA_MULTIPLE_STRUCTURAL_CLASSES;
          String message = getMessage(msgID, String.valueOf(dn),
                                structuralClass.getNameOrOID(),
                                oc.getNameOrOID());

          if (structuralPolicy == AcceptRejectWarn.REJECT)
          {
            invalidReason.append(message);
            return false;
          }
          else if (structuralPolicy == AcceptRejectWarn.WARN)
          {
            if (! multipleOCErrorLogged)
            {
              logError(ErrorLogCategory.SCHEMA,
                       ErrorLogSeverity.SEVERE_WARNING, message,
                       msgID);
              multipleOCErrorLogged = true;
            }
          }
        }
      }
    }

    NameForm         nameForm         = null;
    DITContentRule   ditContentRule   = null;
    DITStructureRule ditStructureRule = null;
    if (structuralClass == null)
    {
      int msgID = MSGID_ENTRY_SCHEMA_NO_STRUCTURAL_CLASS;
      String message = getMessage(msgID, String.valueOf(dn));

      if (structuralPolicy == AcceptRejectWarn.REJECT)
      {
        invalidReason.append(message);
        return false;
      }
      else if (structuralPolicy == AcceptRejectWarn.WARN)
      {
        logError(ErrorLogCategory.SCHEMA,
                 ErrorLogSeverity.SEVERE_WARNING, message, msgID);
      }
    }
    else
    {
      ditContentRule =
           DirectoryServer.getDITContentRule(structuralClass);
      if ((ditContentRule != null) && ditContentRule.isObsolete())
      {
        ditContentRule = null;
      }

      if (validateNameForms)
      {
        nameForm = DirectoryServer.getNameForm(structuralClass);
        if ((nameForm != null) && nameForm.isObsolete())
        {
          nameForm = null;
        }

        if (validateStructureRules && (nameForm != null))
        {
          ditStructureRule =
               DirectoryServer.getDITStructureRule(nameForm);
          if ((ditStructureRule != null) &&
              ditStructureRule.isObsolete())
          {
            ditStructureRule = null;
          }
        }
      }
    }


    // Make sure that we recognize all of the objectclasses, that all
    // auxiliary classes are allowed by the DIT content rule, and that
    // all attributes required by the object classes are present.
    for (ObjectClass o : objectClasses.keySet())
    {
      if (DirectoryServer.getObjectClass(o.getOID()) == null)
      {
        int    msgID   = MSGID_ENTRY_SCHEMA_UNKNOWN_OC;
        String message = getMessage(msgID, String.valueOf(dn), o
                                    .getNameOrOID());
        invalidReason.append(message);
        return false;
      }

      if ((o.getObjectClassType() == ObjectClassType.AUXILIARY) &&
          (ditContentRule != null) &&
          (! ditContentRule.getAuxiliaryClasses().contains(o)))
      {
        int msgID = MSGID_ENTRY_SCHEMA_DISALLOWED_AUXILIARY_CLASS;
        String message = getMessage(msgID, String.valueOf(dn),
                                    o.getNameOrOID(),
                                    ditContentRule.getName());
        if (structuralPolicy == AcceptRejectWarn.REJECT)
        {
          invalidReason.append(message);
          return false;
        }
        else if (structuralPolicy == AcceptRejectWarn.WARN)
        {
          logError(ErrorLogCategory.SCHEMA,
                   ErrorLogSeverity.SEVERE_WARNING, message, msgID);
        }
      }

      for (AttributeType t : o.getRequiredAttributes())
      {
        if (! (userAttributes.containsKey(t) ||
               operationalAttributes.containsKey(t) ||
               t.isObjectClassType()))
        {
          int msgID = MSGID_ENTRY_SCHEMA_MISSING_REQUIRED_ATTR_FOR_OC;
          String message = getMessage(msgID, String.valueOf(dn),
                                      t.getNameOrOID(),
                                      o.getNameOrOID());
          invalidReason.append(message);
          return false;
        }
      }
    }


    // Make sure all the user attributes are allowed, have at least
    // one value, and if they are single-valued that they have exactly
    // one value.
    for (AttributeType t : userAttributes.keySet())
    {
      boolean found = false;
      for (ObjectClass o : objectClasses.keySet())
      {
        if (o.isRequiredOrOptional(t))
        {
          found = true;
          break;
        }
      }

      if ((! found) && (ditContentRule != null))
      {
        if (ditContentRule.isRequiredOrOptional(t))
        {
          found = true;
        }
      }

      if (! found)
      {
        int msgID = MSGID_ENTRY_SCHEMA_DISALLOWED_USER_ATTR_FOR_OC;
        String message = getMessage(msgID, String.valueOf(dn),
                                    t.getNameOrOID());
        invalidReason.append(message);
        return false;
      }

      List<Attribute> attrList = userAttributes.get(t);
      if (attrList != null)
      {
        for (Attribute a : attrList)
        {
          LinkedHashSet<AttributeValue> values = a.getValues();
          if (values.isEmpty())
          {
            int    msgID   = MSGID_ENTRY_SCHEMA_ATTR_NO_VALUES;
            String message = getMessage(msgID, String.valueOf(dn),
                                        t.getNameOrOID());

            invalidReason.append(message);
            return false;
          }
          else if (t.isSingleValue() && (values.size() != 1))
          {
            int    msgID   = MSGID_ENTRY_SCHEMA_ATTR_SINGLE_VALUED;
            String message = getMessage(msgID, String.valueOf(dn),
                                        t.getNameOrOID());

            invalidReason.append(message);
            return false;
          }
        }
      }
    }


    // Iterate through all of the operational attributes and make sure
    // that all of the single-valued attributes only have one value.
    for (AttributeType t : operationalAttributes.keySet())
    {
      if (t.isSingleValue())
      {
        List<Attribute> attrList = operationalAttributes.get(t);
        if (attrList != null)
        {
          for (Attribute a : attrList)
          {
            if (a.getValues().size() > 1)
            {
              int    msgID   = MSGID_ENTRY_SCHEMA_ATTR_SINGLE_VALUED;
              String message = getMessage(msgID, String.valueOf(dn),
                                          t.getNameOrOID());

              invalidReason.append(message);
              return false;
            }
          }
        }
      }
    }


    // If there is a name form for this entry, then make sure that the
    // RDN for the entry is in compliance with it.
    if (nameForm != null)
    {
      RDN rdn = dn.getRDN();
      if (rdn != null)
      {
        // Make sure that all the required attributes are present.
        for (AttributeType t : nameForm.getRequiredAttributes())
        {
          if (! rdn.hasAttributeType(t))
          {
            int msgID = MSGID_ENTRY_SCHEMA_RDN_MISSING_REQUIRED_ATTR;
            String message = getMessage(msgID, String.valueOf(dn),
                                        t.getNameOrOID(),
                                        nameForm.getNameOrOID());

            if (structuralPolicy == AcceptRejectWarn.REJECT)
            {
              invalidReason.append(message);
              return false;
            }
            else if (structuralPolicy == AcceptRejectWarn.WARN)
            {
              logError(ErrorLogCategory.SCHEMA,
                       ErrorLogSeverity.SEVERE_WARNING, message,
                       msgID);
            }
          }
        }

        // Make sure that all attributes in the RDN are allowed.
        int numAVAs = rdn.getNumValues();
        for (int i = 0; i < numAVAs; i++)
        {
          AttributeType t = rdn.getAttributeType(i);
          if (! nameForm.isRequiredOrOptional(t))
          {
            int    msgID   = MSGID_ENTRY_SCHEMA_RDN_DISALLOWED_ATTR;
            String message = getMessage(msgID, String.valueOf(dn),
                                        t.getNameOrOID(),
                                        nameForm.getNameOrOID());

            if (structuralPolicy == AcceptRejectWarn.REJECT)
            {
              invalidReason.append(message);
              return false;
            }
            else if (structuralPolicy == AcceptRejectWarn.WARN)
            {
              logError(ErrorLogCategory.SCHEMA,
                       ErrorLogSeverity.SEVERE_WARNING, message,
                       msgID);
            }
          }
        }
      }
    }


    // If there is a DIT content rule for this entry, then make sure
    // that the entry is in compliance with it.
    if (ditContentRule != null)
    {
      // Make sure that all of the required attributes are present.
      for (AttributeType t : ditContentRule.getRequiredAttributes())
      {
        if (! (userAttributes.containsKey(t) ||
               operationalAttributes.containsKey(t) ||
               t.isObjectClassType()))
        {
          int msgID =
               MSGID_ENTRY_SCHEMA_MISSING_REQUIRED_ATTR_FOR_DCR;
          String message = getMessage(msgID, String.valueOf(dn),
                                      t.getNameOrOID(),
                                      ditContentRule.getName());

          if (structuralPolicy == AcceptRejectWarn.REJECT)
          {
            invalidReason.append(message);
            return false;
          }
          else if (structuralPolicy == AcceptRejectWarn.WARN)
          {
            logError(ErrorLogCategory.SCHEMA,
                     ErrorLogSeverity.SEVERE_WARNING, message, msgID);
          }
        }
      }

      // Make sure that none of the prohibited attributes are present.
      for (AttributeType t : ditContentRule.getProhibitedAttributes())
      {
        if (userAttributes.containsKey(t) ||
            operationalAttributes.containsKey(t))
        {
          int    msgID   = MSGID_ENTRY_SCHEMA_PROHIBITED_ATTR_FOR_DCR;
          String message = getMessage(msgID, String.valueOf(dn),
                                      t.getNameOrOID(),
                                      ditContentRule.getName());

          if (structuralPolicy == AcceptRejectWarn.REJECT)
          {
            invalidReason.append(message);
            return false;
          }
          else if (structuralPolicy == AcceptRejectWarn.WARN)
          {
            logError(ErrorLogCategory.SCHEMA,
                     ErrorLogSeverity.SEVERE_WARNING, message, msgID);
          }
        }
      }
    }


    // If there is a DIT structure rule for this entry, then make sure
    // that the entry is in compliance with it.
    if ((ditStructureRule != null) &&
        ditStructureRule.hasSuperiorRules())
    {
      if (parentProvided)
      {
        if (parentEntry != null)
        {
          boolean dsrValid =
               validateDITStructureRule(ditStructureRule,
                                        structuralClass, parentEntry,
                                        structuralPolicy,
                                        invalidReason);
          if (! dsrValid)
          {
            return false;
          }
        }
      }
      else
      {
        // Get the DN of the parent entry if possible.
        DN parentDN = dn.getParentDNInSuffix();
        if (parentDN != null)
        {
          // Get the parent entry and check its structural class.
          Lock lock = null;
          for (int i=0; i < 3; i++)
          {
            lock = LockManager.lockRead(parentDN);
            if (lock != null)
            {
              break;
            }
          }

          if (lock == null)
          {
            int msgID = MSGID_ENTRY_SCHEMA_DSR_COULD_NOT_LOCK_PARENT;
            String message = getMessage(msgID, String.valueOf(dn),
                                  String.valueOf(parentDN));

            if (structuralPolicy == AcceptRejectWarn.REJECT)
            {
              invalidReason.append(message);
              return false;
            }
            else if (structuralPolicy == AcceptRejectWarn.WARN)
            {
              logError(ErrorLogCategory.SCHEMA,
                       ErrorLogSeverity.SEVERE_WARNING, message,
                       msgID);
            }
          }
          else
          {
            try
            {
              parentEntry = DirectoryServer.getEntry(parentDN);
              if (parentEntry == null)
              {
                int msgID = MSGID_ENTRY_SCHEMA_DSR_NO_PARENT_ENTRY;
                String message =
                     getMessage(msgID, String.valueOf(dn),
                                String.valueOf(parentDN));

                if (structuralPolicy == AcceptRejectWarn.REJECT)
                {
                  invalidReason.append(message);
                  return false;
                }
                else if (structuralPolicy == AcceptRejectWarn.WARN)
                {
                  logError(ErrorLogCategory.SCHEMA,
                           ErrorLogSeverity.SEVERE_WARNING, message,
                           msgID);
                }
              }
              else
              {
                boolean dsrValid =
                     validateDITStructureRule(ditStructureRule,
                                              structuralClass,
                                              parentEntry,
                                              structuralPolicy,
                                              invalidReason);
                if (! dsrValid)
                {
                  return false;
                }
              }
            }
            catch (Exception e)
            {
              if (debugEnabled())
              {
                debugCought(DebugLogLevel.ERROR, e);
              }

              int    msgID   = MSGID_ENTRY_SCHEMA_COULD_NOT_CHECK_DSR;
              String message =
                   getMessage(msgID, String.valueOf(dn),
                              ditStructureRule.getNameOrRuleID(),
                              stackTraceToSingleLineString(e));

              if (structuralPolicy == AcceptRejectWarn.REJECT)
              {
                invalidReason.append(message);
                return false;
              }
              else if (structuralPolicy == AcceptRejectWarn.WARN)
              {
                logError(ErrorLogCategory.SCHEMA,
                         ErrorLogSeverity.SEVERE_WARNING, message,
                         msgID);
              }
            }
            finally
            {
              LockManager.unlock(parentDN, lock);
            }
          }
        }
      }
    }
    else if (validateStructureRules)
    {
      // There is no DIT structure rule for this entry, but there may
      // be one for the parent entry.  If there is such a rule for the
      // parent entry, then this entry will not be valid.
      boolean parentExists = false;
      ObjectClass parentStructuralClass = null;
      if (parentEntry != null)
      {
        parentExists = true;
        parentStructuralClass =
             parentEntry.getStructuralObjectClass();
      }
      else if (! parentProvided)
      {
        DN parentDN = getDN().getParentDNInSuffix();
        if (parentDN != null)
        {
          // Get the parent entry and check its structural class.
          Lock lock = null;
          for (int i=0; i < 3; i++)
          {
            lock = LockManager.lockRead(parentDN);
            if (lock != null)
            {
              break;
            }
          }

          if (lock == null)
          {
            int msgID = MSGID_ENTRY_SCHEMA_DSR_COULD_NOT_LOCK_PARENT;
            String message = getMessage(msgID, String.valueOf(dn),
                                  String.valueOf(parentDN));

            if (structuralPolicy == AcceptRejectWarn.REJECT)
            {
              invalidReason.append(message);
              return false;
            }
            else if (structuralPolicy == AcceptRejectWarn.WARN)
            {
              logError(ErrorLogCategory.SCHEMA,
                       ErrorLogSeverity.SEVERE_WARNING, message,
                       msgID);
            }
          }
          else
          {
            try
            {
              parentEntry = DirectoryServer.getEntry(parentDN);
              if (parentEntry == null)
              {
                int msgID = MSGID_ENTRY_SCHEMA_DSR_NO_PARENT_ENTRY;
                String message =
                     getMessage(msgID, String.valueOf(dn),
                                String.valueOf(parentDN));

                if (structuralPolicy == AcceptRejectWarn.REJECT)
                {
                  invalidReason.append(message);
                  return false;
                }
                else if (structuralPolicy == AcceptRejectWarn.WARN)
                {
                  logError(ErrorLogCategory.SCHEMA,
                           ErrorLogSeverity.SEVERE_WARNING, message,
                           msgID);
                }
              }
              else
              {
                parentExists = true;
                parentStructuralClass =
                     parentEntry.getStructuralObjectClass();
              }
            }
            catch (Exception e)
            {
              if (debugEnabled())
              {
                debugCought(DebugLogLevel.ERROR, e);
              }

              int msgID =
                   MSGID_ENTRY_SCHEMA_COULD_NOT_CHECK_PARENT_DSR;
              String message =
                   getMessage(msgID, String.valueOf(dn),
                              stackTraceToSingleLineString(e));

              if (structuralPolicy == AcceptRejectWarn.REJECT)
              {
                invalidReason.append(message);
                return false;
              }
              else if (structuralPolicy == AcceptRejectWarn.WARN)
              {
                logError(ErrorLogCategory.SCHEMA,
                         ErrorLogSeverity.SEVERE_WARNING, message,
                         msgID);
              }
            }
            finally
            {
              LockManager.unlock(parentDN, lock);
            }
          }
        }
      }

      if (parentExists)
      {
        if (parentStructuralClass == null)
        {
          int    msgID   = MSGID_ENTRY_SCHEMA_DSR_NO_PARENT_OC;
          String message = getMessage(msgID, String.valueOf(dn),
                                String.valueOf(parentEntry.getDN()));

          if (structuralPolicy == AcceptRejectWarn.REJECT)
          {
            invalidReason.append(message);
            return false;
          }
          else if (structuralPolicy == AcceptRejectWarn.WARN)
          {
            logError(ErrorLogCategory.SCHEMA,
                     ErrorLogSeverity.SEVERE_WARNING, message, msgID);
          }
        }
        else
        {
          NameForm parentNF =
               DirectoryServer.getNameForm(parentStructuralClass);
          if ((parentNF != null) && (! parentNF.isObsolete()))
          {
            DITStructureRule parentDSR =
                 DirectoryServer.getDITStructureRule(parentNF);
            if ((parentDSR != null) && (! parentDSR.isObsolete()))
            {
              int    msgID   = MSGID_ENTRY_SCHEMA_VIOLATES_PARENT_DSR;
              String message =
                   getMessage(msgID, String.valueOf(dn),
                              String.valueOf(parentEntry.getDN()));

              if (structuralPolicy == AcceptRejectWarn.REJECT)
              {
                invalidReason.append(message);
                return false;
              }
              else if (structuralPolicy == AcceptRejectWarn.WARN)
              {
                logError(ErrorLogCategory.SCHEMA,
                         ErrorLogSeverity.SEVERE_WARNING, message,
                         msgID);
              }
            }
          }
        }
      }
    }


    // If we've gotten here, then the entry is acceptable.
    return true;
  }



  /**
   * Determines whether this entry is in conformance to the provided
   * DIT structure rule.
   *
   * @param  dsr               The DIT structure rule to use in the
   *                           determination.
   * @param  structuralClass   The structural objectclass for this
   *                           entry to use in the determination.
   * @param  parentEntry       The reference to the parent entry to
   *                           check.
   * @param  structuralPolicy  The policy that should be used around
   *                           enforcement of DIT structure rules.
   * @param  invalidReason     The buffer to which the invalid reason
   *                           should be appended if a problem is
   *                           found.
   *
   * @return  <CODE>true</CODE> if this entry conforms to the provided
   *          DIT structure rule, or <CODE>false</CODE> if not.
   */
  private boolean validateDITStructureRule(DITStructureRule dsr,
                       ObjectClass structuralClass, Entry parentEntry,
                       AcceptRejectWarn structuralPolicy,
                       StringBuilder invalidReason)
  {
    ObjectClass oc = parentEntry.getStructuralObjectClass();
    if (oc == null)
    {
      int    msgID   = MSGID_ENTRY_SCHEMA_DSR_NO_PARENT_OC;
      String message = getMessage(msgID, String.valueOf(dn),
                            String.valueOf(parentEntry.getDN()));

      if (structuralPolicy == AcceptRejectWarn.REJECT)
      {
        invalidReason.append(message);
        return false;
      }
      else if (structuralPolicy == AcceptRejectWarn.WARN)
      {
        logError(ErrorLogCategory.SCHEMA,
                 ErrorLogSeverity.SEVERE_WARNING, message,
                 msgID);
      }
    }

    boolean matchFound = false;
    for (DITStructureRule dsr2 : dsr.getSuperiorRules())
    {
      if (dsr2.getStructuralClass().equals(oc))
      {
        matchFound = true;
      }
    }

    if (! matchFound)
    {
      int msgID = MSGID_ENTRY_SCHEMA_DSR_DISALLOWED_SUPERIOR_OC;
      String message = getMessage(msgID, String.valueOf(dn),
                            dsr.getNameOrRuleID(),
                            structuralClass.getNameOrOID(),
                            oc.getNameOrOID());

      if (structuralPolicy == AcceptRejectWarn.REJECT)
      {
        invalidReason.append(message);
        return false;
      }
      else if (structuralPolicy == AcceptRejectWarn.WARN)
      {
        logError(ErrorLogCategory.SCHEMA,
                 ErrorLogSeverity.SEVERE_WARNING, message, msgID);
      }
    }

    return true;
  }



  /**
   * Retrieves the attachment for this entry.
   *
   * @return  The attachment for this entry, or <CODE>null</CODE> if
   *          there is none.
   */
  public Object getAttachment()
  {

    return attachment;
  }



  /**
   * Specifies the attachment for this entry.  This will replace any
   * existing attachment that might be defined.
   *
   * @param  attachment  The attachment for this entry, or
   *                     <CODE>null</CODE> if there should not be an
   *                     attachment.
   */
  public void setAttachment(Object attachment)
  {

    this.attachment = attachment;
  }



  /**
   * Creates a duplicate of this entry that may be altered without
   * impacting the information in this entry.
   *
   * @return  A duplicate of this entry that may be altered without
   *          impacting the information in this entry.
   */
  public Entry duplicate()
  {

    HashMap<ObjectClass,String> objectClassesCopy =
         new HashMap<ObjectClass,String>(objectClasses);

    HashMap<AttributeType,List<Attribute>> userAttrsCopy =
         new HashMap<AttributeType,List<Attribute>>(
              userAttributes.size());
    deepCopy(userAttributes, userAttrsCopy, false);

    HashMap<AttributeType,List<Attribute>> operationalAttrsCopy =
         new HashMap<AttributeType,List<Attribute>>(
                  operationalAttributes.size());
    deepCopy(operationalAttributes, operationalAttrsCopy, false);

    return new Entry(dn, objectClassesCopy, userAttrsCopy,
                     operationalAttrsCopy);
  }



  /**
   * Creates a duplicate of this entry without any operational
   * attributes that may be altered without impacting the information
   * in this entry.
   *
   * @param  typesOnly  Indicates whether to include attribute types
   *                    only without values.
   *
   * @return  A duplicate of this entry that may be altered without
   *          impacting the information in this entry and that does
   *          not contain any operational attributes.
   */
  public Entry duplicateWithoutOperationalAttributes(
                    boolean typesOnly)
  {

    HashMap<ObjectClass,String> objectClassesCopy;
    if (typesOnly)
    {
      objectClassesCopy = new HashMap<ObjectClass,String>(0);
    }
    else
    {
      objectClassesCopy =
           new HashMap<ObjectClass,String>(objectClasses);
    }

    HashMap<AttributeType,List<Attribute>> userAttrsCopy =
         new HashMap<AttributeType,List<Attribute>>(
              userAttributes.size());
    if (typesOnly)
    {
      // Make sure to include the objectClass attribute here because
      // it won't make it in otherwise.
      AttributeType ocType =
           DirectoryServer.getObjectClassAttributeType();
      ArrayList<Attribute> ocList = new ArrayList<Attribute>(1);
      ocList.add(new Attribute(ocType));
      userAttrsCopy.put(ocType, ocList);
    }

    deepCopy(userAttributes, userAttrsCopy, typesOnly);

    HashMap<AttributeType,List<Attribute>> operationalAttrsCopy =
         new HashMap<AttributeType,List<Attribute>>(0);

    return new Entry(dn, objectClassesCopy, userAttrsCopy,
                     operationalAttrsCopy);
  }



  /**
   * Performs a deep copy from the source map to the target map.  In
   * this case, the attributes in the list will be duplicates rather
   * than re-using the same reference.
   *
   * @param  source  The source map from which to obtain the
   *                 information.
   * @param  target  The target map into which to place the copied
   *                 information.
   * @param  omitValues <CODE>true</CODE> if the values should be
   *                    omitted.
   */
  private void deepCopy(Map<AttributeType,List<Attribute>> source,
                        Map<AttributeType,List<Attribute>> target,
                        boolean omitValues)
  {
    for (AttributeType t : source.keySet())
    {
      List<Attribute> sourceList = source.get(t);
      ArrayList<Attribute> targetList =
           new ArrayList<Attribute>(sourceList.size());

      for (Attribute a : sourceList)
      {
        targetList.add(a.duplicate(omitValues));
      }

      target.put(t, targetList);
    }
  }



  /**
   * Creates a duplicate of this entry without any attribute or
   * objectclass information (i.e., it will just contain the DN and
   * placeholders for adding attributes) and objectclasses.
   *
   * @return  A duplicate of this entry that may be altered without
   *          impacting the information in this entry and that does
   *          not contain attribute or objectclass information.
   */
  public Entry duplicateWithoutAttributes()
  {

    HashMap<ObjectClass,String> objectClassesCopy =
         new HashMap<ObjectClass,String>(objectClasses.size());

    HashMap<AttributeType,List<Attribute>> userAttrsCopy =
         new HashMap<AttributeType,List<Attribute>>(
              userAttributes.size());

    HashMap<AttributeType,List<Attribute>> operationalAttrsCopy =
         new HashMap<AttributeType,List<Attribute>>(
                  operationalAttributes.size());

    return new Entry(dn, objectClassesCopy, userAttrsCopy,
                     operationalAttrsCopy);
  }



  /**
   * Indicates whether this entry meets the criteria to consider it a
   * referral (e.g., it contains the "referral" objectclass and a
   * "ref" attribute).
   *
   * @return  <CODE>true</CODE> if this entry meets the criteria to
   *          consider it a referral, or <CODE>false</CODE> if not.
   */
  public boolean isReferral()
  {

    ObjectClass referralOC =
         DirectoryServer.getObjectClass(OC_REFERRAL);
    if (referralOC == null)
    {
      // This should not happen -- The server doesn't have a referral
      // objectclass defined.
      if (debugEnabled())
      {
        debugWarning(
            "No %s objectclass is defined in the server schema.",
                     OC_REFERRAL);
      }

      for (String ocName : objectClasses.values())
      {
        if (ocName.equalsIgnoreCase(OC_REFERRAL))
        {
          return true;
        }
      }

      return false;
    }

    if (! objectClasses.containsKey(referralOC))
    {
      return false;
    }

    AttributeType referralType =
         DirectoryServer.getAttributeType(ATTR_REFERRAL_URL);
    if (referralType == null)
    {
      // This should not happen -- The server doesn't have a ref
      // attribute type defined.
      if (debugEnabled())
      {
        debugWarning(
            "No %s attribute type is defined in the server schema.",
                     ATTR_REFERRAL_URL);
      }
      return false;
    }

    return (userAttributes.containsKey(referralType) ||
            operationalAttributes.containsKey(referralType));
  }



  /**
   * Retrieves the set of referral URLs that are included in this
   * referral entry.  This should only be called if
   * <CODE>isReferral()</CODE> returns <CODE>true</CODE>.
   *
   * @return  The set of referral URLs that are included in this entry
   *          if it is a referral, or <CODE>null</CODE> if it is not a
   *          referral.
   */
  public LinkedHashSet<String> getReferralURLs()
  {

    AttributeType referralType =
         DirectoryServer.getAttributeType(ATTR_REFERRAL_URL);
    if (referralType == null)
    {
      // This should not happen -- The server doesn't have a ref
      // attribute type defined.
      if (debugEnabled())
      {
        debugWarning(
            "No %s attribute type is defined in the server schema.",
                     ATTR_REFERRAL_URL);
      }
      return null;
    }

    List<Attribute> refAttrs = userAttributes.get(referralType);
    if (refAttrs == null)
    {
      refAttrs = operationalAttributes.get(referralType);
      if (refAttrs == null)
      {
        return null;
      }
    }

    LinkedHashSet<String> referralURLs = new LinkedHashSet<String>();
    for (Attribute a : refAttrs)
    {
      for (AttributeValue v : a.getValues())
      {
        referralURLs.add(v.getStringValue());
      }
    }

    return referralURLs;
  }



  /**
   * Indicates whether this entry meets the criteria to consider it an
   * alias (e.g., it contains the "aliasObject" objectclass and a
   * "alias" attribute).
   *
   * @return  <CODE>true</CODE> if this entry meets the criteria to
   *          consider it an alias, or <CODE>false</CODE> if not.
   */
  public boolean isAlias()
  {

    ObjectClass aliasOC = DirectoryServer.getObjectClass(OC_ALIAS);
    if (aliasOC == null)
    {
      // This should not happen -- The server doesn't have an alias
      // objectclass defined.
      if (debugEnabled())
      {
        debugWarning(
            "No %s objectclass is defined in the server schema.",
                     OC_ALIAS);
      }

      for (String ocName : objectClasses.values())
      {
        if (ocName.equalsIgnoreCase(OC_ALIAS))
        {
          return true;
        }
      }

      return false;
    }

    if (! objectClasses.containsKey(aliasOC))
    {
      return false;
    }

    AttributeType aliasType =
         DirectoryServer.getAttributeType(ATTR_ALIAS_DN);
    if (aliasType == null)
    {
      // This should not happen -- The server doesn't have an
      // aliasedObjectName attribute type defined.
      if (debugEnabled())
      {
        debugWarning(
            "No %s attribute type is defined in the server schema.",
                     ATTR_ALIAS_DN);
      }
      return false;
    }

    return (userAttributes.containsKey(aliasType) ||
            operationalAttributes.containsKey(aliasType));
  }



  /**
   * Retrieves the DN of the entry referenced by this alias entry.
   * This should only be called if <CODE>isAlias()</CODE> returns
   * <CODE>true</CODE>.
   *
   * @return  The DN of the entry referenced by this alias entry, or
   *          <CODE>null</CODE> if it is not an alias.
   *
   * @throws  DirectoryException  If there is an aliasedObjectName
   *                              attribute but its value cannot be
   *                              parsed as a DN.
   */
  public DN getAliasedDN()
         throws DirectoryException
  {

    AttributeType aliasType =
         DirectoryServer.getAttributeType(ATTR_REFERRAL_URL);
    if (aliasType == null)
    {
      // This should not happen -- The server doesn't have an
      // aliasedObjectName attribute type defined.
      if (debugEnabled())
      {
        debugWarning(
            "No %s attribute type is defined in the server schema.",
                     ATTR_ALIAS_DN);
      }
      return null;
    }

    List<Attribute> aliasAttrs = userAttributes.get(aliasType);
    if (aliasAttrs == null)
    {
      aliasAttrs = operationalAttributes.get(aliasType);
      if (aliasAttrs == null)
      {
        return null;
      }
    }

    if (aliasAttrs.isEmpty())
    {
      return null;
    }
    else
    {
      // There should only be a single alias attribute in an entry,
      // and we'll skip the check for others for performance reasons.
      // We would just end up taking the first one anyway.  The same
      // is true with the set of values, since it should be a
      // single-valued attribute.
      Attribute aliasAttr = aliasAttrs.get(0);
      LinkedHashSet<AttributeValue> attrValues =
           aliasAttr.getValues();
      if (attrValues.isEmpty())
      {
        return null;
      }
      else
      {
        return
             DN.decode(attrValues.iterator().next().getStringValue());
      }
    }
  }



  /**
   * Indicates whether this entry meets the criteria to consider it an
   * LDAP subentry (i.e., it contains the "ldapSubentry" objectclass).
   *
   * @return  <CODE>true</CODE> if this entry meets the criteria to
   *          consider it an LDAP subentry, or <CODE>false</CODE> if
   *          not.
   */
  public boolean isLDAPSubentry()
  {

    ObjectClass ldapSubentryOC =
         DirectoryServer.getObjectClass(OC_LDAP_SUBENTRY_LC);
    if (ldapSubentryOC == null)
    {
      // This should not happen -- The server doesn't have an
      // ldapsubentry objectclass defined.
      if (debugEnabled())
      {
        debugWarning(
            "No %s objectclass is defined in the server schema.",
                     OC_LDAP_SUBENTRY);
      }

      for (String ocName : objectClasses.values())
      {
        if (ocName.equalsIgnoreCase(OC_LDAP_SUBENTRY))
        {
          return true;
        }
      }

      return false;
    }


    // Make the determination based on whether this entry has the
    // ldapSubentry objectclass.
    return objectClasses.containsKey(ldapSubentryOC);
  }



  /**
   * Indicates whether this entry falls within the range of the
   * provided search base DN and scope.
   *
   * @param  baseDN  The base DN for which to make the determination.
   * @param  scope   The search scope for which to make the
   *                 determination.
   *
   * @return  <CODE>true</CODE> if this entry is within the given
   *          base and scope, or <CODE>false</CODE> if it is not.
   */
  public boolean matchesBaseAndScope(DN baseDN, SearchScope scope)
  {

    switch (scope)
    {
      case BASE_OBJECT:
        // The entry DN must equal the base DN.
        return baseDN.equals(dn);

      case SINGLE_LEVEL:
        // The parent DN for this entry must equal the base DN.
        return baseDN.equals(dn.getParentDNInSuffix());

      case WHOLE_SUBTREE:
        // The base DN must be an ancestor of the entry DN.
        return baseDN.isAncestorOf(dn);

      case SUBORDINATE_SUBTREE:
        // The base DN must be an ancstor of the entry DN, but it
        // must not equal the entry DN.
        return ((! baseDN.equals(dn)) && baseDN.isAncestorOf(dn));

      default:
        // This is a scope that we don't recognize.
        return false;
    }
  }



  /**
   * Retrieves a list of the lines for this entry in LDIF form.  Long
   * lines will not be wrapped automatically.
   *
   * @return  A list of the lines for this entry in LDIF form.
   */
  public List<StringBuilder> toLDIF()
  {

    LinkedList<StringBuilder> ldifLines =
         new LinkedList<StringBuilder>();


    // First, append the DN.
    StringBuilder dnLine = new StringBuilder();
    dnLine.append("dn");
    appendLDIFSeparatorAndValue(dnLine, getBytes(dn.toString()));
    ldifLines.add(dnLine);


    // Next, add the set of objectclasses.
    for (String s : objectClasses.values())
    {
      StringBuilder ocLine = new StringBuilder();
      ocLine.append("objectClass: ");
      ocLine.append(s);
      ldifLines.add(ocLine);
    }


    // Next, add the set of user attributes.
    for (List<Attribute> attrList : userAttributes.values())
    {
      for (Attribute a : attrList)
      {
        StringBuilder attrName = new StringBuilder(a.getName());
        for (String o : a.getOptions())
        {
          attrName.append(";");
          attrName.append(o);
        }

        for (AttributeValue v : a.getValues())
        {
          StringBuilder attrLine = new StringBuilder();
          attrLine.append(attrName);
          appendLDIFSeparatorAndValue(attrLine, v.getValueBytes());
          ldifLines.add(attrLine);
        }
      }
    }


    // Finally, add the set of operational attributes.
    for (List<Attribute> attrList : operationalAttributes.values())
    {
      for (Attribute a : attrList)
      {
        StringBuilder attrName = new StringBuilder(a.getName());
        for (String o : a.getOptions())
        {
          attrName.append(";");
          attrName.append(o);
        }

        for (AttributeValue v : a.getValues())
        {
          StringBuilder attrLine = new StringBuilder();
          attrLine.append(attrName);
          appendLDIFSeparatorAndValue(attrLine, v.getValueBytes());
          ldifLines.add(attrLine);
        }
      }
    }


    return ldifLines;
  }



  /**
   * Writes this entry in LDIF form according to the provided
   * configuration.
   *
   * @param  exportConfig  The configuration that specifies how the
   *                       entry should be written.
   *
   * @return  <CODE>true</CODE> if the entry is actually written, or
   *          <CODE>false</CODE> if it is not for some reason.
   *
   * @throws  IOException  If a problem occurs while writing the
   *                       information.
   *
   * @throws  LDIFException  If a problem occurs while trying to
   *                         determine whether to write the entry.
   */
  public boolean toLDIF(LDIFExportConfig exportConfig)
         throws IOException, LDIFException
  {


    // See if this entry should be included in the export at all.
    try
    {
      if (! exportConfig.includeEntry(this))
      {
        if (debugEnabled())
        {
          debugInfo(
              "Skipping entry %s because of the export " +
                  "configuration.", String.valueOf(dn));
        }
        return false;
      }
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      int msgID = MSGID_LDIF_COULD_NOT_EVALUATE_FILTERS_FOR_EXPORT;
      String message = getMessage(msgID, String.valueOf(dn),
                                  String.valueOf(e));
      throw new LDIFException(msgID, message, e);
    }


    // Invoke LDIF export plugins on the entry if appropriate.
    if (exportConfig.invokeExportPlugins())
    {
      PluginConfigManager pluginConfigManager =
           DirectoryServer.getPluginConfigManager();
      LDIFPluginResult pluginResult =
           pluginConfigManager.invokeLDIFExportPlugins(exportConfig,
                                                    this);
      if (! pluginResult.continueEntryProcessing())
      {
        return false;
      }
    }


    // Get the information necessary to write the LDIF.
    BufferedWriter writer     = exportConfig.getWriter();
    int            wrapColumn = exportConfig.getWrapColumn();
    boolean        wrapLines  = (wrapColumn > 1);


    // First, write the DN.  It will always be included.
    StringBuilder dnLine = new StringBuilder();
    dnLine.append("dn");
    appendLDIFSeparatorAndValue(dnLine, getBytes(dn.toString()));
    writeLDIFLine(dnLine, writer, wrapLines, wrapColumn);


    // Next, the set of objectclasses.
    if (exportConfig.includeObjectClasses())
    {
      if (exportConfig.typesOnly())
      {
        StringBuilder ocLine = new StringBuilder("objectClass:");
        writeLDIFLine(ocLine, writer, wrapLines, wrapColumn);
      }
      else
      {
        for (String s : objectClasses.values())
        {
          StringBuilder ocLine = new StringBuilder();
          ocLine.append("objectClass: ");
          ocLine.append(s);
          writeLDIFLine(ocLine, writer, wrapLines, wrapColumn);
        }
      }
    }
    else
    {
      if (debugEnabled())
      {
        debugVerbose(
            "Skipping objectclasses for entry %s because of " +
            "the export configuration.", String.valueOf(dn));
      }
    }


    // Now the set of user attributes.
    for (AttributeType attrType : userAttributes.keySet())
    {
      if (exportConfig.includeAttribute(attrType))
      {
        List<Attribute> attrList = userAttributes.get(attrType);
        for (Attribute a : attrList)
        {
          if (exportConfig.typesOnly())
          {
            StringBuilder attrName = new StringBuilder(a.getName());
            for (String o : a.getOptions())
            {
              attrName.append(";");
              attrName.append(o);
            }
            attrName.append(":");

            writeLDIFLine(attrName, writer, wrapLines, wrapColumn);
          }
          else
          {
            StringBuilder attrName = new StringBuilder(a.getName());
            for (String o : a.getOptions())
            {
              attrName.append(";");
              attrName.append(o);
            }

            for (AttributeValue v : a.getValues())
            {
              StringBuilder attrLine = new StringBuilder();
              attrLine.append(attrName);
              appendLDIFSeparatorAndValue(attrLine,
                                          v.getValueBytes());
              writeLDIFLine(attrLine, writer, wrapLines, wrapColumn);
            }
          }
        }
      }
      else
      {
        if (debugEnabled())
        {
          debugVerbose(
              "Skipping user attribute %s for entry %s because of " +
                       "the export configuration.",
              attrType.getNameOrOID(), String.valueOf(dn));
        }
      }
    }


    // Finally, the set of operational attributes.
    if (exportConfig.includeOperationalAttributes())
    {
      for (AttributeType attrType : operationalAttributes.keySet())
      {
        if (exportConfig.includeAttribute(attrType))
        {
          List<Attribute> attrList =
               operationalAttributes.get(attrType);
          for (Attribute a : attrList)
          {
            if (exportConfig.typesOnly())
            {
              StringBuilder attrName = new StringBuilder(a.getName());
              for (String o : a.getOptions())
              {
                attrName.append(";");
                attrName.append(o);
              }
              attrName.append(":");

              writeLDIFLine(attrName, writer, wrapLines, wrapColumn);
            }
            else
            {
              StringBuilder attrName = new StringBuilder(a.getName());
              for (String o : a.getOptions())
              {
                attrName.append(";");
                attrName.append(o);
              }

              for (AttributeValue v : a.getValues())
              {
                StringBuilder attrLine = new StringBuilder();
                attrLine.append(attrName);
                appendLDIFSeparatorAndValue(attrLine,
                                            v.getValueBytes());
                writeLDIFLine(attrLine, writer, wrapLines,
                              wrapColumn);
              }
            }
          }
        }
        else
        {
          if (debugEnabled())
          {
            debugVerbose(
                "Skipping operational attribute %s for entry %s " +
                         "because of the export configuration.",
                         attrType.getNameOrOID(), String.valueOf(dn));
          }
        }
      }
    }
    else
    {
      if (debugEnabled())
      {
        debugVerbose(
            "Skipping all operational attributes for entry %s " +
            "because of the export configuration.",
            String.valueOf(dn));
      }
    }

    // Make sure there is a blank line after the entry.
    writer.newLine();


    return true;
  }



  /**
   * Retrieves the name of the protocol associated with this protocol
   * element.
   *
   * @return  The name of the protocol associated with this protocol
   *          element.
   */
  public String getProtocolElementName()
  {

    return "Entry";
  }



  /**
   * Retrieves a string representation of this protocol element.
   *
   * @return  A string representation of this protocol element.
   */
  public String toString()
  {

    StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this protocol element to the
   * provided buffer.
   *
   * @param  buffer  The buffer into which the string representation
   *                 should be written.
   */
  public void toString(StringBuilder buffer)
  {

    buffer.append("Entry(dn=\"");
    dn.toString(buffer);

    buffer.append("\", objectClasses={");
    if (! objectClasses.isEmpty())
    {
      Iterator<String> ocNames = objectClasses.values().iterator();
      buffer.append(ocNames.next());

      while (ocNames.hasNext())
      {
        buffer.append(",");
        buffer.append(ocNames.next());
      }
    }

    buffer.append("}, userAttrs={");
    if (! userAttributes.isEmpty())
    {
      Iterator<AttributeType> attrs =
           userAttributes.keySet().iterator();
      buffer.append(attrs.next().getNameOrOID());

      while (attrs.hasNext())
      {
        buffer.append(",");
        buffer.append(attrs.next().getNameOrOID());
      }
    }

    buffer.append("}, operationalAttrs={");
    if (! operationalAttributes.isEmpty())
    {
      Iterator<AttributeType> attrs =
           operationalAttributes.keySet().iterator();
      buffer.append(attrs.next().getNameOrOID());

      while (attrs.hasNext())
      {
        buffer.append(",");
        buffer.append(attrs.next().getNameOrOID());
      }
    }

    buffer.append("})");
  }



  /**
   * Appends a string representation of this protocol element to the
   * provided buffer.
   *
   * @param  buffer  The buffer into which the string representation
   *                 should be written.
   * @param  indent  The number of spaces that should be used to
   *                 indent the resulting string representation.
   */
  public void toString(StringBuilder buffer, int indent)
  {

    StringBuilder indentBuf = new StringBuilder(indent);
    for (int i=0 ; i < indent; i++)
    {
      indentBuf.append(' ');
    }

    for (StringBuilder b : toLDIF())
    {
      buffer.append(indentBuf);
      buffer.append(b);
      buffer.append(EOL);
    }
  }



  /**
   * Retrieves a one-line representation of this entry.
   *
   * @return  A one-line representation of this entry.
   */
  public String toSingleLineString()
  {

    StringBuilder buffer = new StringBuilder();
    toSingleLineString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a single-line representation of this entry to the
   * provided buffer.
   *
   * @param  buffer  The buffer to which the information should be
   *                 written.
   */
  public void toSingleLineString(StringBuilder buffer)
  {

    buffer.append("Entry(dn=\"");
    dn.toString(buffer);
    buffer.append("\",objectClasses={");

    Iterator<String> iterator = objectClasses.values().iterator();
    if (iterator.hasNext())
    {
      buffer.append(iterator.next());

      while (iterator.hasNext())
      {
        buffer.append(",");
        buffer.append(iterator.next());
      }
    }

    buffer.append("},userAttrs={");

    boolean firstAttr = true;
    for (List<Attribute> attrList : userAttributes.values())
    {
      for (Attribute a : attrList)
      {
        if (firstAttr)
        {
          firstAttr = false;
        }
        else
        {
          buffer.append(",");
        }

        buffer.append(a.getName());

        if (a.hasOptions())
        {
          for (String optionString : a.getOptions())
          {
            buffer.append(";");
            buffer.append(optionString);
          }
        }

        buffer.append("={");
        Iterator<AttributeValue> valueIterator =
             a.getValues().iterator();
        if (valueIterator.hasNext())
        {
          buffer.append(valueIterator.next().getStringValue());

          while (valueIterator.hasNext())
          {
            buffer.append(",");
            buffer.append(valueIterator.next().getStringValue());
          }
        }

        buffer.append("}");
      }
    }

    buffer.append("},operationalAttrs={");
    for (List<Attribute> attrList : operationalAttributes.values())
    {
      for (Attribute a : attrList)
      {
        if (firstAttr)
        {
          firstAttr = false;
        }
        else
        {
          buffer.append(",");
        }

        buffer.append(a.getName());

        if (a.hasOptions())
        {
          for (String optionString : a.getOptions())
          {
            buffer.append(";");
            buffer.append(optionString);
          }
        }

        buffer.append("={");
        Iterator<AttributeValue> valueIterator =
             a.getValues().iterator();
        if (valueIterator.hasNext())
        {
          buffer.append(valueIterator.next().getStringValue());

          while (valueIterator.hasNext())
          {
            buffer.append(",");
            buffer.append(valueIterator.next().getStringValue());
          }
        }

        buffer.append("}");
      }
    }

    buffer.append("})");
  }
}

