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

package com.sun.opends.sdk.ldap;



import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.opends.sdk.ConnectionSecurityLayer;
import org.opends.sdk.ErrorResultException;



/**
 * <tt>Transformer</tt>, which decodes SASL encrypted data, contained in the
 * input Buffer, to the output Buffer.
 */
final class SASLDecoderTransformer extends AbstractTransformer<Buffer, Buffer>
{
  private static final int BUFFER_SIZE = 4096;
  private final byte[] buffer = new byte[BUFFER_SIZE];
  private final ConnectionSecurityLayer bindContext;

  private final MemoryManager<?> memoryManager;



  public SASLDecoderTransformer(final ConnectionSecurityLayer bindContext,
      final MemoryManager<?> memoryManager)
  {
    this.bindContext = bindContext;
    this.memoryManager = memoryManager;
  }



  public String getName()
  {
    return this.getClass().getName();
  }



  public boolean hasInputRemaining(final AttributeStorage storage,
      final Buffer input)
  {
    return input != null && input.hasRemaining();
  }



  @Override
  public TransformationResult<Buffer, Buffer> transformImpl(
      final AttributeStorage storage, final Buffer input)
      throws TransformationException
  {

    final int len = Math.min(buffer.length, input.remaining());
    input.get(buffer, 0, len);

    try
    {
      final Buffer output = Buffers.wrap(memoryManager, bindContext.unwrap(
          buffer, 0, len));
      return TransformationResult.createCompletedResult(output, input);
    }
    catch (final ErrorResultException e)
    {
      return TransformationResult.createErrorResult(e.getResult()
          .getResultCode().intValue(), e.getMessage());
    }
  }
}
