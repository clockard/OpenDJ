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
 *      Copyright 2010 Sun Microsystems, Inc.
 *      Portions copyright 2011-2014 ForgeRock AS
 */
package org.forgerock.opendj.ldap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import org.fest.assertions.Assertions;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test case for ByteStringBuilder.
 */
@SuppressWarnings("javadoc")
public class ByteStringBuilderTestCase extends ByteSequenceTestCase {

    //@Checkstyle:off
    private static byte _(int i) {
        return (byte) i;
    }
    //@Checkstyle:on

    private static final byte[] EIGHT_BYTES = new byte[] { _(0x01), _(0x02), _(0x03),
        _(0x04), _(0x05), _(0x06), _(0x07), _(0x08) };

    /**
     * ByteSequence data provider.
     *
     * @return The array of ByteStrings and the bytes it should contain.
     */
    @DataProvider(name = "byteSequenceProvider")
    public Object[][] byteSequenceProvider() throws Exception {
        final Object[][] builders = byteStringBuilderProvider();
        final Object[][] addlSequences = new Object[builders.length + 1][];
        System.arraycopy(builders, 0, addlSequences, 0, builders.length);
        addlSequences[builders.length] =
                new Object[] { new ByteStringBuilder().append(EIGHT_BYTES).subSequence(2, 6),
                    new byte[] { _(0x03), _(0x04), _(0x05), _(0x06) } };

        return addlSequences;
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAppendBadByteBufferLength1() {
        new ByteStringBuilder().append(ByteBuffer.wrap(new byte[5]), -1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAppendBadByteBufferLength2() {
        new ByteStringBuilder().append(ByteBuffer.wrap(new byte[5]), 6);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAppendBadByteSequenceReaderLength1() {
        new ByteStringBuilder().append(ByteString.wrap(new byte[5]).asReader(), -1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAppendBadByteSequenceReaderLength2() {
        new ByteStringBuilder().append(ByteString.wrap(new byte[5]).asReader(), 6);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAppendBadInputStreamLength() throws Exception {
        final ByteArrayInputStream stream = new ByteArrayInputStream(new byte[5]);
        new ByteStringBuilder().append(stream, -1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAppendBadLength1() {
        new ByteStringBuilder().append(new byte[5], 0, 6);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAppendBadLength2() {
        new ByteStringBuilder().append(new byte[5], 0, -1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAppendBadOffset1() {
        new ByteStringBuilder().append(new byte[5], -1, 3);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testAppendBadOffset2() {
        new ByteStringBuilder().append(new byte[5], 6, 0);
    }

    @Test
    public void testAppendInputStream() throws Exception {
        final ByteStringBuilder bsb = new ByteStringBuilder();
        final ByteArrayInputStream stream = new ByteArrayInputStream(new byte[5]);
        Assert.assertEquals(bsb.append(stream, 10), 5);
    }

    @Test(dataProvider = "builderProvider", expectedExceptions = IndexOutOfBoundsException.class)
    public void testClear(final ByteStringBuilder bs, final byte[] ba) {
        bs.clear();
        Assert.assertEquals(bs.length(), 0);
        bs.byteAt(0);
    }

    @DataProvider
    public Object[][] clearAndTruncateProvider() throws Exception {
        return new Object[][] {
            { builder(0), 42, 42 },
            { builder(42), 42, 42 },
            { builder(43), 42, 42 },
        };
    }

    private ByteStringBuilder builder(int length) {
        final ByteStringBuilder builder = new ByteStringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(42);
        }
        return builder;
    }

    @Test(dataProvider = "clearAndTruncateProvider")
    public void testClearAndTruncate(ByteStringBuilder bs, int thresholdCapacity, int newCapacity) {
        bs.clearAndTruncate(thresholdCapacity, newCapacity);
        Assertions.assertThat(bs.length()).isEqualTo(0);
        Assertions.assertThat(bs.capacity()).isLessThanOrEqualTo(thresholdCapacity);
        Assertions.assertThat(bs.capacity()).isLessThanOrEqualTo(newCapacity);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void clearAndTruncateThrowsWithNegativeNewCapacity() {
        new ByteStringBuilder().clearAndTruncate(42, -1);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void clearAndTruncateThrowsWithNewCapacityAboveThreshold() {
        new ByteStringBuilder().clearAndTruncate(42, 42 + 1);
    }

    @Test
    public void testEnsureAdditionalCapacity() {
        final ByteStringBuilder bsb = new ByteStringBuilder(8);
        Assert.assertEquals(bsb.getBackingArray().length, 8);
        bsb.ensureAdditionalCapacity(43);
        bsb.ensureAdditionalCapacity(2);
        Assert.assertTrue(bsb.getBackingArray().length >= 43);
    }

    @Test(dataProvider = "builderProvider")
    public void testGetBackingArray(final ByteStringBuilder bs, final byte[] ba) {
        final byte[] trimmedArray = new byte[bs.length()];
        System.arraycopy(bs.getBackingArray(), 0, trimmedArray, 0, bs.length());
        Assert.assertTrue(Arrays.equals(trimmedArray, ba));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidCapacity() {
        new ByteStringBuilder(-1);
    }

    @Test
    public void testTrimToSize() {
        final ByteStringBuilder bsb = new ByteStringBuilder();
        bsb.append(EIGHT_BYTES);
        Assert.assertTrue(bsb.getBackingArray().length > 8);
        bsb.trimToSize();
        Assert.assertEquals(bsb.getBackingArray().length, 8);
    }

    @Test
    public void testAsOutputStream() throws Exception {
        final ByteStringBuilder bsb = new ByteStringBuilder();
        final OutputStream os = bsb.asOutputStream();
        os.write(_(0x01));
        os.write(2);
        os.write(new byte[] { 2, 3, 4, 5 }, 1, 2);
        os.close();
        Assert.assertEquals(bsb.length(), 4);
        Assert.assertEquals(bsb.toByteArray(), new byte[] { 1, 2, 3, 4 });
    }

    @Test
    public void testAsOutputStreamCompress() throws Exception {
        final ByteString data = ByteString.wrap(new byte[4000]);
        final ByteStringBuilder compressedData = new ByteStringBuilder();
        final OutputStream compressor = new DeflaterOutputStream(compressedData.asOutputStream());
        data.copyTo(compressor);
        compressor.close();
        Assert.assertTrue(compressedData.length() > 0 && compressedData.length() < 4000);

        final ByteStringBuilder decompressedData = new ByteStringBuilder();
        final OutputStream decompressor = new InflaterOutputStream(decompressedData.asOutputStream());
        compressedData.copyTo(decompressor);
        decompressor.close();
        Assert.assertEquals(decompressedData.toByteString(), data);
    }

    @DataProvider(name = "builderProvider")
    private Object[][] byteStringBuilderProvider() throws Exception {
        final ByteBuffer testBuffer = ByteBuffer.wrap(EIGHT_BYTES);
        final ByteString testByteString = ByteString.wrap(EIGHT_BYTES);
        final ByteSequenceReader testByteReader = testByteString.asReader();
        final InputStream testStream = new ByteArrayInputStream(EIGHT_BYTES);
        final ByteStringBuilder testBuilderFromStream = new ByteStringBuilder(8);
        testBuilderFromStream.append(testStream, 8);

        return new Object[][] {
            { new ByteStringBuilder().append(_(0x00)).append(_(0x01)),
                new byte[] { _(0x00), _(0x01) } },
            { new ByteStringBuilder(5)
                      .append(new byte[] { _(0x01), _(0x02), _(0x03), _(0x04) })
                      .append(new byte[] { _(0x05), _(0x06), _(0x07), _(0x08) }),
                EIGHT_BYTES },
            { new ByteStringBuilder(3).append(EIGHT_BYTES, 0, 3).append(EIGHT_BYTES, 3, 5),
                EIGHT_BYTES },
            { new ByteStringBuilder().append(testBuffer, 3).append(testBuffer, 5), EIGHT_BYTES },
            { new ByteStringBuilder(2).append(testByteString), EIGHT_BYTES },
            { new ByteStringBuilder().append(testByteReader, 5).append(testByteReader, 3),
                EIGHT_BYTES },
            { testBuilderFromStream, EIGHT_BYTES },
            { new ByteStringBuilder().append(Short.MIN_VALUE).append(Short.MAX_VALUE),
                new byte[] { _(0x80), _(0x00), _(0x7F), _(0xFF) } },
            {
                new ByteStringBuilder(5).append(Integer.MIN_VALUE).append(Integer.MAX_VALUE),
                new byte[] { _(0x80), _(0x00), _(0x00), _(0x00), _(0x7F),
                    _(0xFF), _(0xFF), _(0xFF) } },
            {
                new ByteStringBuilder().append(Long.MIN_VALUE).append(Long.MAX_VALUE),
                new byte[] { _(0x80), _(0x00), _(0x00), _(0x00), _(0x00),
                    _(0x00), _(0x00), _(0x00), _(0x7F), _(0xFF), _(0xFF),
                    _(0xFF), _(0xFF), _(0xFF), _(0xFF), _(0xFF) } },
            { new ByteStringBuilder(11).append("this is a").append(" test"),
                "this is a test".getBytes("UTF-8") },
            { new ByteStringBuilder().append((Object) "this is a").append((Object) " test"),
                "this is a test".getBytes("UTF-8") },
            {
                new ByteStringBuilder().append("this is a".toCharArray()).append(
                        " test".toCharArray()), "this is a test".getBytes("UTF-8") },
            {
                new ByteStringBuilder().append((Object) "this is a".toCharArray()).append(
                        (Object) " test".toCharArray()), "this is a test".getBytes("UTF-8") },
            {
                new ByteStringBuilder().append((Object) EIGHT_BYTES).append((Object) EIGHT_BYTES),
                new byte[] { _(0x01), _(0x02), _(0x03), _(0x04), _(0x05),
                    _(0x06), _(0x07), _(0x08), _(0x01), _(0x02), _(0x03),
                    _(0x04), _(0x05), _(0x06), _(0x07), _(0x08) } },
            {
                new ByteStringBuilder().appendBERLength(0x00000000).appendBERLength(0x00000001)
                        .appendBERLength(0x0000000F).appendBERLength(0x00000010).appendBERLength(
                                0x0000007F).appendBERLength(0x000000FF).appendBERLength(0x00000100)
                        .appendBERLength(0x00000FFF).appendBERLength(0x00001000).appendBERLength(
                                0x0000FFFF).appendBERLength(0x00010000).appendBERLength(0x000FFFFF)
                        .appendBERLength(0x00100000).appendBERLength(0x00FFFFFF).appendBERLength(
                                0x01000000).appendBERLength(0x0FFFFFFF).appendBERLength(0x10000000)
                        .appendBERLength(0xFFFFFFFF),
                new byte[] { _(0x00), _(0x01), _(0x0F), _(0x10), _(0x7F),
                    _(0x81), _(0xFF), _(0x82), _(0x01), _(0x00), _(0x82),
                    _(0x0F), _(0xFF), _(0x82), _(0x10), _(0x00), _(0x82),
                    _(0xFF), _(0xFF), _(0x83), _(0x01), _(0x00), _(0x00),
                    _(0x83), _(0x0F), _(0xFF), _(0xFF), _(0x83), _(0x10),
                    _(0x00), _(0x00), _(0x83), _(0xFF), _(0xFF), _(0xFF),
                    _(0x84), _(0x01), _(0x00), _(0x00), _(0x00), _(0x84),
                    _(0x0F), _(0xFF), _(0xFF), _(0xFF), _(0x84), _(0x10),
                    _(0x00), _(0x00), _(0x00), _(0x84), _(0xFF), _(0xFF),
                    _(0xFF), _(0xFF) } }, };
    }

    @Test
    public void testCopyCtor() {
        final ByteStringBuilder builder = new ByteStringBuilder(400);
        builder.append("this is a ByteString");
        final ByteString orig = builder.toByteString();
        final ByteString copy = new ByteStringBuilder(orig).toByteString();
        Assert.assertEquals(copy, orig);
        Assert.assertEquals(copy.length(), builder.length());
    }

    @Test
    public void testSetByte() {
        final ByteStringBuilder builder = new ByteStringBuilder();
        builder.append("this is a ByteString");
        builder.setByte(2, _('a'));
        builder.setByte(3, _('t'));
        Assert.assertEquals(builder.toByteString().toString(), "that is a ByteString");
    }

    @Test(expectedExceptions = { IndexOutOfBoundsException.class })
    public void testSetByteAtInvalidLowerIndex() {
        final ByteStringBuilder builder = new ByteStringBuilder();
        builder.setByte(-1, _(0));
    }

    @Test(expectedExceptions = { IndexOutOfBoundsException.class })
    public void testSetByteAtInvalidUpperIndex() {
        final ByteStringBuilder builder = new ByteStringBuilder();
        builder.setByte(builder.length(), _(0));
    }

    @Test
    public void testSetLength() {
        final ByteStringBuilder builder = new ByteStringBuilder();
        builder.append("this is a ByteString");
        builder.setLength(builder.length() - 16);
        Assert.assertEquals(builder.toString(), "this");
        builder.setLength(builder.length() + 1);
        Assert.assertEquals(builder.toString(), "this\u0000");
    }

    @Test(expectedExceptions = { IndexOutOfBoundsException.class })
    public void testSetInvalidLength() {
        new ByteStringBuilder().setLength(-1);
    }

    @Test
    public void testAppendNullCharArray() {
        final ByteStringBuilder builder = new ByteStringBuilder();
        builder.append((char[]) null);
        Assert.assertTrue(builder.isEmpty());
    }

    @Test
    public void testAppendNullString() {
        final ByteStringBuilder builder = new ByteStringBuilder();
        builder.append((String) null);
        Assert.assertTrue(builder.isEmpty());
    }

    @Test
    public void testAppendNonAsciiCharArray() {
        final ByteStringBuilder builder = new ByteStringBuilder();
        builder.append(new char[] { 'œ', 'Œ' });
        Assert.assertEquals(builder.toString(), "œŒ");
    }

    @Test
    public void testAppendNonAsciiString() {
        final ByteStringBuilder builder = new ByteStringBuilder();
        builder.append("œŒ");
        Assert.assertEquals(builder.toString(), "œŒ");
    }

    @Test
    public void testByteStringBuilderCompareTo() {
        final ByteString orig = ByteString.valueOf("this is a ByteString");
        final ByteStringBuilder builder = new ByteStringBuilder(orig);
        Assert.assertEquals(builder.compareTo(builder), 0);
        Assert.assertEquals(builder.compareTo(orig), 0);
        Assert.assertEquals(orig.compareTo(builder), 0);
    }

    @Test
    public void testSubSequenceCompareTo() {
        final ByteString orig = ByteString.valueOf("this is a ByteString");
        final ByteStringBuilder builder = new ByteStringBuilder(orig);
        final ByteSequence subSequence = builder.subSequence(0, 4);
        Assert.assertEquals(subSequence.compareTo(subSequence), 0);
        Assert.assertTrue(subSequence.compareTo(orig) < 0);
        Assert.assertTrue(orig.compareTo(subSequence) > 0);
    }

    @Test
    public void testSubSequenceEqualsAndHashCode() {
        final ByteString orig = ByteString.valueOf("this is a ByteString");
        final ByteStringBuilder builder = new ByteStringBuilder(orig);
        final ByteSequence subSequence = builder.subSequence(0, builder.length());
        final ByteSequence subSequence2 = builder.subSequence(0, builder.length());
        Assert.assertTrue(subSequence.hashCode() != 0);
        Assert.assertTrue(subSequence.equals(subSequence));
        Assert.assertTrue(subSequence.equals(subSequence2));
        Assert.assertTrue(subSequence.equals(builder));
        Assert.assertFalse(subSequence.equals(null));
    }

    @Test
    public void testSubSequenceIsEmpty() {
        final ByteStringBuilder builder = new ByteStringBuilder();
        Assert.assertTrue(builder.subSequence(0, builder.length()).isEmpty());
        builder.append("This is a ByteString");
        Assert.assertFalse(builder.subSequence(0, builder.length()).isEmpty());
    }
}