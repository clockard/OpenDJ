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

package com.forgerock.opendj.ldap;



import java.io.IOException;
import java.nio.ByteBuffer;

import org.forgerock.opendj.asn1.ASN1Reader;
import org.forgerock.opendj.asn1.ASN1Writer;
import org.forgerock.opendj.asn1.ASN1WriterTestCase;
import org.forgerock.opendj.ldap.DecodeException;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransportFactory;
import org.glassfish.grizzly.memory.ByteBufferWrapper;

import com.forgerock.opendj.ldap.ASN1BufferReader;
import com.forgerock.opendj.ldap.ASN1BufferWriter;



/**
 * This class provides testcases for ASN1BufferWriter.
 */
public class ASN1BufferWriterTestCase extends ASN1WriterTestCase
{

  private final ASN1BufferWriter writer = ASN1BufferWriter.getWriter();



  @Override
  protected byte[] getEncodedBytes() throws IOException, DecodeException
  {
    final Buffer buffer = writer.getBuffer();
    final byte[] byteArray = new byte[buffer.remaining()];
    buffer.get(byteArray);
    return byteArray;
  }



  @Override
  protected ASN1Reader getReader(final byte[] encodedBytes)
      throws DecodeException, IOException
  {
    final ByteBufferWrapper buffer = new ByteBufferWrapper(ByteBuffer.wrap(encodedBytes));
    final ASN1BufferReader reader = new ASN1BufferReader(0, TransportFactory
        .getInstance().getDefaultMemoryManager());
    reader.appendBytesRead(buffer);
    return reader;
  }



  @Override
  protected ASN1Writer getWriter() throws IOException
  {
    writer.flush();
    writer.recycle();
    return writer;
  }
}
