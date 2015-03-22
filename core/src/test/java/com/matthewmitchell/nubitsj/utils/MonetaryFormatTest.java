/*
 * Copyright 2014 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.matthewmitchell.nubitsj.utils;

import static com.matthewmitchell.nubitsj.core.Coin.CENT;
import static com.matthewmitchell.nubitsj.core.Coin.COIN;
import static com.matthewmitchell.nubitsj.core.Coin.SATOSHI;
import static com.matthewmitchell.nubitsj.core.Coin.ZERO;
import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

import com.matthewmitchell.nubitsj.core.Coin;

public class MonetaryFormatTest {

    private static final MonetaryFormat NO_CODE = MonetaryFormat.NBT.noCode();

    @Test
    public void testSigns() throws Exception {
        assertEquals("-1.00", NO_CODE.format(Coin.COIN.negate()).toString());
        assertEquals("@1.00", NO_CODE.negativeSign('@').format(Coin.COIN.negate()).toString());
        assertEquals("1.00", NO_CODE.format(Coin.COIN).toString());
        assertEquals("+1.00", NO_CODE.positiveSign('+').format(Coin.COIN).toString());
    }

    @Test
    public void testDigits() throws Exception {
        assertEquals("١٢٣٤٥٦.٧٨٩٠", NO_CODE.digits('\u0660').format(Coin.valueOf(1234567890l)).toString());
    }

    @Test
    public void testDecimalMark() throws Exception {
        assertEquals("1.00", NO_CODE.format(Coin.COIN).toString());
        assertEquals("1,00", NO_CODE.decimalMark(',').format(Coin.COIN).toString());
    }

    @Test
    public void testGrouping() throws Exception {
        assertEquals("0.1", format(Coin.parseCoin("0.1"), 0, 1, 2, 1));
        assertEquals("0.010", format(Coin.parseCoin("0.01"), 0, 1, 2, 1));
        assertEquals("0.001", format(Coin.parseCoin("0.001"), 0, 1, 2, 1));
        assertEquals("0.0001", format(Coin.parseCoin("0.0001"), 0, 1, 2, 1));
    }

    @Test
    public void nbtRounding() throws Exception {
        assertEquals("0", format(ZERO, 0, 0));
        assertEquals("0.00", format(ZERO, 0, 2));

        assertEquals("1", format(COIN, 0, 0));
        assertEquals("1.0", format(COIN, 0, 1));
        assertEquals("1.00", format(COIN, 0, 2, 2));
        assertEquals("1.000", format(COIN, 0, 3));
        assertEquals("1.0000", format(COIN, 0, 4));

        final Coin justNot = COIN.subtract(SATOSHI);
        assertEquals("1", format(justNot, 0, 0));
        assertEquals("1.0", format(justNot, 0, 1));
        assertEquals("0.9999", format(justNot, 0, 2, 2));
        assertEquals("1.000", format(justNot, 0, 3));
        assertEquals("0.9999", format(justNot, 0, 4));

        final Coin slightlyMore = COIN.add(SATOSHI);
        assertEquals("1", format(slightlyMore, 0, 0));
        assertEquals("1.0", format(slightlyMore, 0, 1));
        assertEquals("1.0001", format(slightlyMore, 0, 2, 2));
        assertEquals("1.000", format(slightlyMore, 0, 3));
        assertEquals("1.0001", format(slightlyMore, 0, 4));

        final Coin pivot = COIN.add(SATOSHI.multiply(5));
        assertEquals("1.0005", format(pivot, 0, 4));
        assertEquals("1.0005", format(pivot, 0, 3, 1));
        assertEquals("1.001", format(pivot, 0, 3));

        final Coin value = Coin.valueOf(112233445566l);
        assertEquals("11223345", format(value, 0, 0));
        assertEquals("11223344.6", format(value, 0, 1));
        assertEquals("11223344.557", format(value, 0, 2, 1));
        assertEquals("11223344.557", format(value, 0, 3));
        assertEquals("11223344.5566", format(value, 0, 4));
    }

    private String format(Coin coin, int shift, int minDecimals, int... decimalGroups) {
        return NO_CODE.shift(shift).minDecimals(minDecimals).optionalDecimals(decimalGroups).format(coin).toString();
    }

    @Test
    public void repeatOptionalDecimals() {
        assertEquals("0.0001", formatRepeat(SATOSHI, 2, 2));
        assertEquals("0.0010", formatRepeat(SATOSHI.multiply(10), 2, 2));
        assertEquals("0.01", formatRepeat(CENT, 2, 2));
        assertEquals("0.10", formatRepeat(CENT.multiply(10), 2, 2));

        assertEquals("0", formatRepeat(SATOSHI, 2, 1));
        assertEquals("0", formatRepeat(SATOSHI.multiply(10), 2, 1));
        assertEquals("0.01", formatRepeat(CENT, 2, 1));
        assertEquals("0.10", formatRepeat(CENT.multiply(10), 2, 1));

        assertEquals("0", formatRepeat(CENT, 2, 0));
        assertEquals("0", formatRepeat(CENT.multiply(10), 2, 0));
    }

    private String formatRepeat(Coin coin, int decimals, int repetitions) {
        return NO_CODE.minDecimals(0).repeatOptionalDecimals(decimals, repetitions).format(coin).toString();
    }

    @Test
    public void standardCodes() throws Exception {
        assertEquals("NBT 0.00", MonetaryFormat.NBT.format(Coin.ZERO).toString());
    }

    @Test
    public void codeOrientation() throws Exception {
        assertEquals("NBT 0.00", MonetaryFormat.NBT.prefixCode().format(Coin.ZERO).toString());
        assertEquals("0.00 NBT", MonetaryFormat.NBT.postfixCode().format(Coin.ZERO).toString());
    }

    @Test
    public void codeSeparator() throws Exception {
        assertEquals("NBT@0.00", MonetaryFormat.NBT.codeSeparator('@').format(Coin.ZERO).toString());
    }

    @Test
    public void withLocale() throws Exception {
        final Coin value = Coin.valueOf(-12345678l);
        assertEquals("-1234.5678", NO_CODE.withLocale(Locale.US).format(value).toString());
        assertEquals("-1234,5678", NO_CODE.withLocale(Locale.GERMANY).format(value).toString());
        assertEquals("-१२३४.५६७८", NO_CODE.withLocale(new Locale("hi", "IN")).format(value).toString()); // Devanagari
    }

    @Test
    public void parse() throws Exception {
        assertEquals(Coin.COIN, NO_CODE.parse("1"));
        assertEquals(Coin.COIN, NO_CODE.parse("1."));
        assertEquals(Coin.COIN, NO_CODE.parse("1.0"));
        assertEquals(Coin.COIN, NO_CODE.decimalMark(',').parse("1,0"));
        assertEquals(Coin.COIN, NO_CODE.parse("01.0000000000"));
        assertEquals(Coin.COIN, NO_CODE.positiveSign('+').parse("+1.0"));
        assertEquals(Coin.COIN.negate(), NO_CODE.parse("-1"));
        assertEquals(Coin.COIN.negate(), NO_CODE.parse("-1.0"));

        assertEquals(Coin.CENT, NO_CODE.parse(".01"));

        assertEquals(Coin.CENT, NO_CODE.withLocale(new Locale("hi", "IN")).parse(".०१")); // Devanagari
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidEmpty() throws Exception {
        NO_CODE.parse("");
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidWhitespaceBefore() throws Exception {
        NO_CODE.parse(" 1");
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidWhitespaceSign() throws Exception {
        NO_CODE.parse("- 1");
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidWhitespaceAfter() throws Exception {
        NO_CODE.parse("1 ");
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidMultipleDecimalMarks() throws Exception {
        NO_CODE.parse("1.0.0");
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidDecimalMark() throws Exception {
        NO_CODE.decimalMark(',').parse("1.0");
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidPositiveSign() throws Exception {
        NO_CODE.positiveSign('@').parse("+1.0");
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidNegativeSign() throws Exception {
        NO_CODE.negativeSign('@').parse("-1.0");
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidHugeNumber() throws Exception {
        NO_CODE.parse("99999999999999999999");
    }

    @Test(expected = NumberFormatException.class)
    public void parseInvalidHugeNegativeNumber() throws Exception {
        NO_CODE.parse("-99999999999999999999");
    }

    private static final Fiat ONE_EURO = Fiat.parseFiat("EUR", "1");

    @Test
    public void fiat() throws Exception {
        assertEquals(ONE_EURO, NO_CODE.parseFiat("EUR", "1"));
    }
}

