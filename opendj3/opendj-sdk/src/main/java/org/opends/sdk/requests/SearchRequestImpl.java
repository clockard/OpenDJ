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
 *      Copyright 2010 Sun Microsystems, Inc.
 */

package org.opends.sdk.requests;



import java.util.LinkedList;
import java.util.List;

import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.opends.sdk.DN;
import org.opends.sdk.DereferenceAliasesPolicy;
import org.opends.sdk.Filter;
import org.opends.sdk.SearchScope;

import com.sun.opends.sdk.util.Validator;



/**
 * Search request implementation.
 */
final class SearchRequestImpl extends AbstractRequestImpl<SearchRequest>
    implements SearchRequest
{

  private final List<String> attributes = new LinkedList<String>();

  private DN name;

  private DereferenceAliasesPolicy dereferenceAliasesPolicy =
      DereferenceAliasesPolicy.NEVER;

  private Filter filter;

  private SearchScope scope;

  private int sizeLimit = 0;

  private int timeLimit = 0;

  private boolean typesOnly = false;



  /**
   * Creates a new search request using the provided distinguished name, scope,
   * and filter, decoded using the default schema.
   *
   * @param name
   *          The distinguished name of the base entry relative to which the
   *          search is to be performed.
   * @param scope
   *          The scope of the search.
   * @param filter
   *          The filter that defines the conditions that must be fulfilled in
   *          order for an entry to be returned.
   * @throws NullPointerException
   *           If the {@code name}, {@code scope}, or {@code filter} were
   *           {@code null}.
   */
  SearchRequestImpl(final DN name, final SearchScope scope, final Filter filter)
      throws NullPointerException
  {
    this.name = name;
    this.scope = scope;
    this.filter = filter;
  }



  /**
   * Creates a new search request that is an exact copy of the provided
   * request.
   *
   * @param searchRequest
   *          The search request to be copied.
   * @throws NullPointerException
   *           If {@code searchRequest} was {@code null} .
   */
  SearchRequestImpl(final SearchRequest searchRequest)
      throws NullPointerException
  {
    super(searchRequest);
    this.attributes.addAll(searchRequest.getAttributes());
    this.name = searchRequest.getName();
    this.dereferenceAliasesPolicy = searchRequest.getDereferenceAliasesPolicy();
    this.filter = searchRequest.getFilter();
    this.scope = searchRequest.getScope();
    this.sizeLimit = searchRequest.getSizeLimit();
    this.timeLimit = searchRequest.getTimeLimit();
    this.typesOnly = searchRequest.isTypesOnly();
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest addAttribute(final String... attributeDescriptions)
      throws NullPointerException
  {
    for (String attributeDescription : attributeDescriptions)
    {
      attributes.add(Validator.ensureNotNull(attributeDescription));
    }
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public List<String> getAttributes()
  {
    return attributes;
  }



  /**
   * {@inheritDoc}
   */
  public DereferenceAliasesPolicy getDereferenceAliasesPolicy()
  {
    return dereferenceAliasesPolicy;
  }



  /**
   * {@inheritDoc}
   */
  public Filter getFilter()
  {
    return filter;
  }



  /**
   * {@inheritDoc}
   */
  public DN getName()
  {
    return name;
  }



  /**
   * {@inheritDoc}
   */
  public SearchScope getScope()
  {
    return scope;
  }



  /**
   * {@inheritDoc}
   */
  public int getSizeLimit()
  {
    return sizeLimit;
  }



  /**
   * {@inheritDoc}
   */
  public int getTimeLimit()
  {
    return timeLimit;
  }



  /**
   * {@inheritDoc}
   */
  public boolean isTypesOnly()
  {
    return typesOnly;
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest setDereferenceAliasesPolicy(
      final DereferenceAliasesPolicy policy) throws NullPointerException
  {
    Validator.ensureNotNull(policy);

    this.dereferenceAliasesPolicy = policy;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest setFilter(final Filter filter)
      throws NullPointerException
  {
    Validator.ensureNotNull(filter);

    this.filter = filter;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest setFilter(final String filter)
      throws LocalizedIllegalArgumentException, NullPointerException
  {
    this.filter = Filter.valueOf(filter);
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest setName(final DN dn) throws NullPointerException
  {
    Validator.ensureNotNull(dn);

    this.name = dn;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest setName(final String dn)
      throws LocalizedIllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(dn);

    this.name = DN.valueOf(dn);
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest setScope(final SearchScope scope)
      throws NullPointerException
  {
    Validator.ensureNotNull(scope);

    this.scope = scope;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest setSizeLimit(final int limit)
      throws LocalizedIllegalArgumentException
  {
    // FIXME: I18N error message.
    Validator.ensureTrue(limit >= 0, "negative size limit");

    this.sizeLimit = limit;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest setTimeLimit(final int limit)
      throws LocalizedIllegalArgumentException
  {
    // FIXME: I18N error message.
    Validator.ensureTrue(limit >= 0, "negative time limit");

    this.timeLimit = limit;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  public SearchRequest setTypesOnly(final boolean typesOnly)
  {
    this.typesOnly = typesOnly;
    return this;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("SearchRequest(name=");
    builder.append(getName());
    builder.append(", scope=");
    builder.append(getScope());
    builder.append(", dereferenceAliasesPolicy=");
    builder.append(getDereferenceAliasesPolicy());
    builder.append(", sizeLimit=");
    builder.append(getSizeLimit());
    builder.append(", timeLimit=");
    builder.append(getTimeLimit());
    builder.append(", typesOnly=");
    builder.append(isTypesOnly());
    builder.append(", filter=");
    builder.append(getFilter());
    builder.append(", attributes=");
    builder.append(getAttributes());
    builder.append(", controls=");
    builder.append(getControls());
    builder.append(")");
    return builder.toString();
  }



  @Override
  SearchRequest getThis()
  {
    return this;
  }
}
