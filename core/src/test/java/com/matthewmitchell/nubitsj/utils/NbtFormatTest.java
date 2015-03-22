/*
 * Copyright 2014 Adam Mackler
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

import com.matthewmitchell.nubitsj.core.Coin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.math.BigDecimal;
import java.text.*;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static com.matthewmitchell.nubitsj.core.Coin.*;
import static com.matthewmitchell.nubitsj.core.NetworkParameters.MAX_MONEY;
import static com.matthewmitchell.nubitsj.utils.NbtAutoFormat.Style.CODE;
import static com.matthewmitchell.nubitsj.utils.NbtAutoFormat.Style.SYMBOL;
import static com.matthewmitchell.nubitsj.utils.NbtFixedFormat.REPEATING_DOUBLETS;
import static com.matthewmitchell.nubitsj.utils.NbtFixedFormat.REPEATING_TRIPLETS;
import static java.text.NumberFormat.Field.DECIMAL_SEPARATOR;
import static java.util.Locale.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class NbtFormatTest {

    @Parameters
    public static Set<Locale[]> data() {
        Set<Locale[]> localeSet = new HashSet<Locale[]>();
        for (Locale locale : Locale.getAvailableLocales()) {
            localeSet.add(new Locale[]{locale});
        }
        return localeSet;
    }

    public NbtFormatTest(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);
    }
 
    @Test
    public void prefixTest() { // prefix b/c symbol is prefixed
        NbtFormat usFormat = NbtFormat.getSymbolInstance(Locale.US);
        assertEquals("𝑵1.00", usFormat.format(COIN));
        assertEquals("𝑵1.01", usFormat.format(10100));
    }

    @Test
    public void suffixTest() {
        NbtFormat deFormat = NbtFormat.getSymbolInstance(Locale.GERMANY);
        // int
        assertEquals("1,00 𝑵", deFormat.format(10000));
        assertEquals("1,01 𝑵", deFormat.format(10100));
    }

    @Test
    public void defaultLocaleTest() {
        assertEquals(
             "Default Locale is " + Locale.getDefault().toString(),
             NbtFormat.getInstance().pattern(), NbtFormat.getInstance(Locale.getDefault()).pattern()
        );
        assertEquals(
            "Default Locale is " + Locale.getDefault().toString(),
            NbtFormat.getCodeInstance().pattern(),
            NbtFormat.getCodeInstance(Locale.getDefault()).pattern()
       );
    }

    @Test
    public void symbolCollisionTest() {
        Locale[] locales = NbtFormat.getAvailableLocales();
        for (int i = 0; i < locales.length; ++i) {
            String cs = ((DecimalFormat)NumberFormat.getCurrencyInstance(locales[i])).
                        getDecimalFormatSymbols().getCurrencySymbol();
            if (cs.contains("𝑵")) {
                NbtFormat bf = NbtFormat.getSymbolInstance(locales[i]);
                String coin = bf.format(COIN);
                assertTrue(coin.contains("𝑵"));
                assertFalse(coin.contains("𝑵"));
                String milli = bf.format(valueOf(10000));
                assertFalse(milli.contains("𝑵"));
                String micro = bf.format(valueOf(100));
                assertFalse(micro.contains("𝑵"));
                NbtFormat ff = NbtFormat.builder().scale(0).locale(locales[i]).pattern("¤#.#").build();
                assertEquals("𝑵", ((NbtFixedFormat)ff).symbol());
                assertEquals("𝑵", ff.coinSymbol());
                coin = ff.format(COIN);
                assertTrue(coin.contains("𝑵"));
                assertFalse(coin.contains("𝑵"));
                NbtFormat mlff = NbtFormat.builder().scale(3).locale(locales[i]).pattern("¤#.#").build();
                assertEquals("𝑵", mlff.coinSymbol());
                milli = mlff.format(valueOf(10000));
                assertFalse(milli.contains("𝑵"));
                NbtFormat mcff = NbtFormat.builder().scale(6).locale(locales[i]).pattern("¤#.#").build();
                assertEquals("𝑵", mcff.coinSymbol());
                micro = mcff.format(valueOf(100));
                assertFalse(micro.contains("𝑵"));
            }
            if (cs.contains("𝑵")) {  // NB: We don't know of any such existing locale, but check anyway.
                NbtFormat bf = NbtFormat.getInstance(locales[i]);
                String coin = bf.format(COIN);
                assertTrue(coin.contains("𝑵"));
                assertFalse(coin.contains("𝑵"));
                String milli = bf.format(valueOf(10000));
                assertFalse(milli.contains("𝑵"));
                String micro = bf.format(valueOf(100));
                assertFalse(micro.contains("𝑵"));
            }
        }
    }

    @Test
    public void argumentTypeTest() {
        NbtFormat usFormat = NbtFormat.getSymbolInstance(Locale.US);
        // longs are tested above
        // BigInteger
        assertEquals("𝑵0.00" ,usFormat.format(java.math.BigInteger.ZERO));
        // BigDecimal
        assertEquals("𝑵1.00" ,usFormat.format(java.math.BigDecimal.ONE));
        assertEquals("𝑵0.00" ,usFormat.format(java.math.BigDecimal.ZERO));
        // Bad type
        try {
            usFormat.format("1");
            fail("should not have tried to format a String");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void columnAlignmentTest() {
        NbtFormat germany = NbtFormat.getCoinInstance(2,NbtFixedFormat.REPEATING_PLACES);
        char separator = germany.symbols().getDecimalSeparator();
        Coin[] rows = {MAX_MONEY, MAX_MONEY.subtract(SATOSHI), Coin.parseCoin("1234"),
                       COIN, COIN.add(SATOSHI), COIN.subtract(SATOSHI),
                        COIN.divide(1000).add(SATOSHI), COIN.divide(1000), COIN.divide(1000).subtract(SATOSHI),
                       valueOf(100), valueOf(1000), valueOf(10000),
                       SATOSHI};
        FieldPosition fp = new FieldPosition(DECIMAL_SEPARATOR);
        String[] output = new String[rows.length];
        int[] indexes = new int[rows.length];
        int maxIndex = 0;
        for (int i = 0; i < rows.length; i++) {
            output[i] = germany.format(rows[i], new StringBuffer(), fp).toString();
            indexes[i] = fp.getBeginIndex();
            if (indexes[i] > maxIndex) maxIndex = indexes[i];
        }
        for (int i = 0; i < output.length; i++) {
            // uncomment to watch printout
            // System.out.println(repeat(" ", (maxIndex - indexes[i])) + output[i]);
            assertEquals(output[i].indexOf(separator), indexes[i]);
        }
    }

    @Test
    public void repeatingPlaceTest() {
        NbtFormat mega = NbtFormat.getInstance(-6, US);
        Coin value = MAX_MONEY.subtract(SATOSHI);
        assertEquals("199,999.9999999999", mega.format(value, 0, NbtFixedFormat.REPEATING_PLACES));
        assertEquals("199,999.9999999999", mega.format(value, 0, NbtFixedFormat.REPEATING_PLACES));
        assertEquals("199,999.9999999999", mega.format(value, 1, NbtFixedFormat.REPEATING_PLACES));
        assertEquals("199,999.9999999999", mega.format(value, 2, NbtFixedFormat.REPEATING_PLACES));
        assertEquals("199,999.9999999999", mega.format(value, 3, NbtFixedFormat.REPEATING_PLACES));
        assertEquals("199,999.9999999999", mega.format(value, 0, NbtFixedFormat.REPEATING_DOUBLETS));
        assertEquals("199,999.9999999999", mega.format(value, 1, NbtFixedFormat.REPEATING_DOUBLETS));
        assertEquals("199,999.9999999999", mega.format(value, 2, NbtFixedFormat.REPEATING_DOUBLETS));
        assertEquals("199,999.9999999999", mega.format(value, 3, NbtFixedFormat.REPEATING_DOUBLETS));
        assertEquals("199,999.9999999999", mega.format(value, 0, NbtFixedFormat.REPEATING_TRIPLETS));
        assertEquals("199,999.9999999999", mega.format(value, 1, NbtFixedFormat.REPEATING_TRIPLETS));
        assertEquals("199,999.9999999999", mega.format(value, 2, NbtFixedFormat.REPEATING_TRIPLETS));
        assertEquals("199,999.9999999999", mega.format(value, 3, NbtFixedFormat.REPEATING_TRIPLETS));
        assertEquals("1.0005", NbtFormat.getCoinInstance(US).
                                   format(COIN.add(Coin.valueOf(5)), 0, NbtFixedFormat.REPEATING_PLACES));
    }

    @Test
    public void characterIteratorTest() {
        NbtFormat usFormat = NbtFormat.getInstance(Locale.US);
        AttributedCharacterIterator i = usFormat.formatToCharacterIterator(parseCoin("1234.5"));
        java.util.Set<Attribute> a = i.getAllAttributeKeys();
        assertTrue("Missing currency attribute", a.contains(NumberFormat.Field.CURRENCY));
        assertTrue("Missing integer attribute", a.contains(NumberFormat.Field.INTEGER));
        assertTrue("Missing fraction attribute", a.contains(NumberFormat.Field.FRACTION));
        assertTrue("Missing decimal separator attribute", a.contains(NumberFormat.Field.DECIMAL_SEPARATOR));
        assertTrue("Missing grouping separator attribute", a.contains(NumberFormat.Field.GROUPING_SEPARATOR));
        assertTrue("Missing currency attribute", a.contains(NumberFormat.Field.CURRENCY));

        char c;
        i = NbtFormat.getCodeInstance(Locale.US).formatToCharacterIterator(new BigDecimal("0.19246362747414458"));
        // formatted as "mNBT 192.5"
        assertEquals(0, i.getBeginIndex());
        assertEquals(10, i.getEndIndex());
        int n = 0;
        for(c = i.first(); i.getAttribute(NumberFormat.Field.CURRENCY) != null; c = i.next()) {
            n++;
        }
        assertEquals(4, n);
        n = 0;
        for(i.next(); i.getAttribute(NumberFormat.Field.INTEGER) != null && i.getAttribute(NumberFormat.Field.GROUPING_SEPARATOR) != NumberFormat.Field.GROUPING_SEPARATOR; c = i.next()) {
            n++;
        }
        assertEquals(3, n);
        assertEquals(NumberFormat.Field.DECIMAL_SEPARATOR, i.getAttribute(NumberFormat.Field.DECIMAL_SEPARATOR));
        n = 0;
        for(c = i.next(); i.getAttribute(NumberFormat.Field.FRACTION) != null; c = i.next()) {
            n++;
        }
        assertEquals(1, n);

        // immutability check
        NbtFormat fa = NbtFormat.getSymbolInstance(US);
        NbtFormat fb = NbtFormat.getSymbolInstance(US);
        assertEquals(fa, fb);
        assertEquals(fa.hashCode(), fb.hashCode());
        fa.formatToCharacterIterator(COIN.multiply(1000000));
        assertEquals(fa, fb);
        assertEquals(fa.hashCode(), fb.hashCode());
        fb.formatToCharacterIterator(COIN.divide(1000000));
        assertEquals(fa, fb);
        assertEquals(fa.hashCode(), fb.hashCode());
    }

    @Test
    public void parseTest() throws java.text.ParseException {
        NbtFormat us = NbtFormat.getSymbolInstance(Locale.US);
        NbtFormat usCoded = NbtFormat.getCodeInstance(Locale.US);
        // Coins
        assertEquals(valueOf(20000), us.parseObject("NBT2"));
        assertEquals(valueOf(20000), us.parseObject("NBT2"));
        assertEquals(valueOf(20000), us.parseObject("𝑵2"));
        assertEquals(valueOf(20000), us.parseObject("𝑵2"));
        assertEquals(valueOf(20000), us.parseObject("2"));
        assertEquals(valueOf(20000), usCoded.parseObject("NBT 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("NBT 2"));
        assertEquals(valueOf(20000), us.parseObject("𝑵2.0"));
        assertEquals(valueOf(20000), us.parseObject("𝑵2.0"));
        assertEquals(valueOf(20000), us.parseObject("2.0"));
        assertEquals(valueOf(20000), us.parseObject("NBT2.0"));
        assertEquals(valueOf(20000), us.parseObject("NBT2.0"));
        assertEquals(valueOf(20000), usCoded.parseObject("𝑵 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("𝑵 2"));
        assertEquals(valueOf(20000), usCoded.parseObject(" 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("NBT 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("NBT 2"));
        assertEquals(valueOf(20222242000L), us.parseObject("2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("𝑵2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("𝑵2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("NBT2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("NBT2,022,224.20"));
        assertEquals(valueOf(22020000L), us.parseObject("2,202.0"));
        assertEquals(valueOf(210000000000L), us.parseObject("21000000.0000"));

        // Same thing with addition of custom code, symbol
        us = NbtFormat.builder().locale(US).style(SYMBOL).symbol("£").code("XYZ").build();
        usCoded = NbtFormat.builder().locale(US).scale(0).symbol("£").code("XYZ").
                            pattern("¤ #,##0.00").build();
        // Coins
        assertEquals(valueOf(20000), us.parseObject("XYZ2"));
        assertEquals(valueOf(20000), us.parseObject("NBT2"));
        assertEquals(valueOf(20000), us.parseObject("NBT2"));
        assertEquals(valueOf(20000), us.parseObject("£2"));
        assertEquals(valueOf(20000), us.parseObject("𝑵2"));
        assertEquals(valueOf(20000), us.parseObject("𝑵2"));
        assertEquals(valueOf(20000), us.parseObject("2"));
        assertEquals(valueOf(20000), usCoded.parseObject("XYZ 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("NBT 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("NBT 2"));
        assertEquals(valueOf(20000), us.parseObject("£2.0"));
        assertEquals(valueOf(20000), us.parseObject("𝑵2.0"));
        assertEquals(valueOf(20000), us.parseObject("𝑵2.0"));
        assertEquals(valueOf(20000), us.parseObject("2.0"));
        assertEquals(valueOf(20000), us.parseObject("XYZ2.0"));
        assertEquals(valueOf(20000), us.parseObject("NBT2.0"));
        assertEquals(valueOf(20000), us.parseObject("NBT2.0"));
        assertEquals(valueOf(20000), usCoded.parseObject("£ 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("𝑵 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("𝑵 2"));
        assertEquals(valueOf(20000), usCoded.parseObject(" 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("XYZ 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("NBT 2"));
        assertEquals(valueOf(20000), usCoded.parseObject("NBT 2"));
        assertEquals(valueOf(20222242000L), us.parseObject("2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("£2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("𝑵2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("𝑵2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("XYZ2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("NBT2,022,224.20"));
        assertEquals(valueOf(20222242000L), us.parseObject("NBT2,022,224.20"));
        assertEquals(valueOf(22020000L), us.parseObject("2,202.0"));
        assertEquals(valueOf(210000000000L), us.parseObject("21000000.000000"));

        // parse() method as opposed to parseObject
        try {
            NbtFormat.getInstance().parse("abc");
            fail("bad parse must raise exception");
        } catch (ParseException e) {}
    }

    @Test
    public void parseMetricTest() throws ParseException {
        NbtFormat cp = NbtFormat.getCodeInstance(Locale.US);
        NbtFormat sp = NbtFormat.getSymbolInstance(Locale.US);
        // coin
        assertEquals(parseCoin("1"), cp.parseObject("NBT 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("NBT1.00"));
        assertEquals(parseCoin("1"), cp.parseObject("𝑵 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("𝑵1.00"));
        assertEquals(parseCoin("1"), cp.parseObject("𝑵 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("𝑵1.00"));
        assertEquals(parseCoin("1"), cp.parseObject("𝑵 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("𝑵1.00"));
        // cents
        assertEquals(parseCoin("0.0123"), cp.parseObject("cNBT 1.23"));
        assertEquals(parseCoin("0.0123"), sp.parseObject("cNBT1.23"));
        assertEquals(parseCoin("0.0123"), cp.parseObject("c𝑵 1.23"));
        assertEquals(parseCoin("0.0123"), sp.parseObject("c𝑵1.23"));
        assertEquals(parseCoin("0.0123"), cp.parseObject("c𝑵 1.23"));
        assertEquals(parseCoin("0.0123"), sp.parseObject("c𝑵1.23"));
        assertEquals(parseCoin("0.0123"), cp.parseObject("c𝑵 1.23"));
        assertEquals(parseCoin("0.0123"), sp.parseObject("c𝑵1.23"));
        assertEquals(parseCoin("0.0123"), cp.parseObject("¢NBT 1.23"));
        assertEquals(parseCoin("0.0123"), sp.parseObject("¢NBT1.23"));
        assertEquals(parseCoin("0.0123"), cp.parseObject("¢𝑵 1.23"));
        assertEquals(parseCoin("0.0123"), sp.parseObject("¢𝑵1.23"));
        assertEquals(parseCoin("0.0123"), cp.parseObject("¢𝑵 1.23"));
        assertEquals(parseCoin("0.0123"), sp.parseObject("¢𝑵1.23"));
        assertEquals(parseCoin("0.0123"), cp.parseObject("¢𝑵 1.23"));
        assertEquals(parseCoin("0.0123"), sp.parseObject("¢𝑵1.23"));
        // dekacoins
        assertEquals(parseCoin("12.3456"), cp.parseObject("daNBT 1.23456"));
        assertEquals(parseCoin("12.3456"), sp.parseObject("daNBT1.23456"));
        assertEquals(parseCoin("12.3456"), cp.parseObject("da𝑵 1.23456"));
        assertEquals(parseCoin("12.3456"), sp.parseObject("da𝑵1.23456"));
        assertEquals(parseCoin("12.3456"), cp.parseObject("da𝑵 1.23456"));
        assertEquals(parseCoin("12.3456"), sp.parseObject("da𝑵1.23456"));
        assertEquals(parseCoin("12.3456"), cp.parseObject("da𝑵 1.23456"));
        assertEquals(parseCoin("12.3456"), sp.parseObject("da𝑵1.23456"));
        // hectocoins
        assertEquals(parseCoin("123.4567"), cp.parseObject("hNBT 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("hNBT1.234567"));
        assertEquals(parseCoin("123.4567"), cp.parseObject("h𝑵 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("h𝑵1.234567"));
        assertEquals(parseCoin("123.4567"), cp.parseObject("h𝑵 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("h𝑵1.234567"));
        assertEquals(parseCoin("123.4567"), cp.parseObject("h𝑵 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("h𝑵1.234567"));
        // kilocoins
        assertEquals(parseCoin("1234.567"), cp.parseObject("kNBT 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("kNBT1.234567"));
        assertEquals(parseCoin("1234.567"), cp.parseObject("k𝑵 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("k𝑵1.234567"));
        assertEquals(parseCoin("1234.567"), cp.parseObject("k𝑵 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("k𝑵1.234567"));
        assertEquals(parseCoin("1234.567"), cp.parseObject("k𝑵 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("k𝑵1.234567"));
        // megacoins
        assertEquals(parseCoin("1234567"), cp.parseObject("MNBT 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("MNBT1.234567"));
        assertEquals(parseCoin("1234567"), cp.parseObject("M𝑵 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("M𝑵1.234567"));
        assertEquals(parseCoin("1234567"), cp.parseObject("M𝑵 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("M𝑵1.234567"));
        assertEquals(parseCoin("1234567"), cp.parseObject("M𝑵 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("M𝑵1.234567"));
    }

    @Test
    public void parsePositionTest() {
        NbtFormat usCoded = NbtFormat.getCodeInstance(Locale.US);
        // Test the field constants
        FieldPosition intField = new FieldPosition(NumberFormat.Field.INTEGER);
        assertEquals(
          "987",
          usCoded.format(valueOf(9876500L), new StringBuffer(), intField).
          substring(intField.getBeginIndex(), intField.getEndIndex())
        );
        FieldPosition fracField = new FieldPosition(NumberFormat.Field.FRACTION);
        assertEquals(
          "65",
          usCoded.format(valueOf(9876500L), new StringBuffer(), fracField).
          substring(fracField.getBeginIndex(), fracField.getEndIndex())
        );

        // for currency we use a locale that puts the units at the end
        NbtFormat de = NbtFormat.getSymbolInstance(Locale.GERMANY);
        NbtFormat deCoded = NbtFormat.getCodeInstance(Locale.GERMANY);
        FieldPosition currField = new FieldPosition(NumberFormat.Field.CURRENCY);
        assertEquals(
          "𝑵",
          de.format(valueOf(9870000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "NBT",
          deCoded.format(valueOf(9870000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
    }

    @Test
    public void currencyCodeTest() {
        /* Insert needed space AFTER currency-code */
        NbtFormat usCoded = NbtFormat.getCodeInstance(Locale.US);
        assertEquals("NBT 1.00", usCoded.format(COIN));

        /* Do not insert unneeded space BEFORE currency-code */
        NbtFormat frCoded = NbtFormat.getCodeInstance(Locale.FRANCE);
        assertEquals("1,00 NBT", frCoded.format(COIN));

        /* Insert needed space BEFORE currency-code: no known currency pattern does this? */

        /* Do not insert unneeded space AFTER currency-code */
        NbtFormat deCoded = NbtFormat.getCodeInstance(Locale.ITALY);
        assertEquals("NBT 1,00", deCoded.format(COIN));
    }

    @Test
    public void coinScaleTest() throws Exception {
        NbtFormat coinFormat = NbtFormat.getCoinInstance(Locale.US);
        assertEquals("1.00", coinFormat.format(Coin.COIN));
        assertEquals("-1.00", coinFormat.format(Coin.COIN.negate()));
        assertEquals(Coin.parseCoin("1"), coinFormat.parseObject("1.00"));
        assertEquals(valueOf(100), coinFormat.parseObject("0.01"));
        assertEquals(Coin.parseCoin("1000"), coinFormat.parseObject("1,000.00"));
        assertEquals(Coin.parseCoin("1000"), coinFormat.parseObject("1000"));
    }

    @Test
    public void testGrouping() throws Exception {
        NbtFormat usCoin = NbtFormat.getInstance(0, Locale.US, 1, 2, 3);
        assertEquals("0.1", usCoin.format(Coin.parseCoin("0.1")));
        assertEquals("0.010", usCoin.format(Coin.parseCoin("0.01")));
        assertEquals("0.001", usCoin.format(Coin.parseCoin("0.001")));
        assertEquals("0.0001", usCoin.format(Coin.parseCoin("0.0001")));

        // no more than two fractional decimal places for the default coin-denomination
        assertEquals("0.01", NbtFormat.getCoinInstance(Locale.US).format(Coin.parseCoin("0.005")));

    }


    /* These just make sure factory methods don't raise exceptions.
     * Other tests inspect their return values. */
    @Test
    public void factoryTest() {
        NbtFormat coded = NbtFormat.getInstance(0, 1, 2, 3);
        NbtFormat.getInstance(NbtAutoFormat.Style.CODE);
        NbtAutoFormat symbolic = (NbtAutoFormat)NbtFormat.getInstance(NbtAutoFormat.Style.SYMBOL);
        assertEquals(2, symbolic.fractionPlaces());
        NbtFormat.getInstance(NbtAutoFormat.Style.CODE, 3);
        assertEquals(3, ((NbtAutoFormat)NbtFormat.getInstance(NbtAutoFormat.Style.SYMBOL, 3)).fractionPlaces());
        NbtFormat.getInstance(NbtAutoFormat.Style.SYMBOL, Locale.US, 3);
        NbtFormat.getInstance(NbtAutoFormat.Style.CODE, Locale.US);
        NbtFormat.getInstance(NbtAutoFormat.Style.SYMBOL, Locale.US);
        NbtFormat.getCoinInstance(2, NbtFixedFormat.REPEATING_PLACES);
        NbtFormat.getInstance(2);
        NbtFormat.getInstance(2, Locale.US);
        NbtFormat.getCodeInstance(3);
        NbtFormat.getSymbolInstance(3);
        NbtFormat.getCodeInstance(Locale.US, 3);
        NbtFormat.getSymbolInstance(Locale.US, 3);
        try {
            NbtFormat.getInstance(SMALLEST_UNIT_EXPONENT + 1);
            fail("should not have constructed an instance with denomination less than satoshi");
        } catch (IllegalArgumentException e) {}
    }
    @Test
    public void factoryArgumentsTest() {
        Locale locale;
        if (Locale.getDefault().equals(GERMANY)) locale = FRANCE;
        else locale = GERMANY;
        assertEquals(NbtFormat.getInstance(), NbtFormat.getCodeInstance());
        assertEquals(NbtFormat.getInstance(locale), NbtFormat.getCodeInstance(locale));
        assertEquals(NbtFormat.getInstance(NbtAutoFormat.Style.CODE), NbtFormat.getCodeInstance());
        assertEquals(NbtFormat.getInstance(NbtAutoFormat.Style.SYMBOL), NbtFormat.getSymbolInstance());
        assertEquals(NbtFormat.getInstance(NbtAutoFormat.Style.CODE,3), NbtFormat.getCodeInstance(3));
        assertEquals(NbtFormat.getInstance(NbtAutoFormat.Style.SYMBOL,3), NbtFormat.getSymbolInstance(3));
        assertEquals(NbtFormat.getInstance(NbtAutoFormat.Style.CODE,locale), NbtFormat.getCodeInstance(locale));
        assertEquals(NbtFormat.getInstance(NbtAutoFormat.Style.SYMBOL,locale), NbtFormat.getSymbolInstance(locale));
        assertEquals(NbtFormat.getInstance(NbtAutoFormat.Style.CODE,locale,3), NbtFormat.getCodeInstance(locale,3));
        assertEquals(NbtFormat.getInstance(NbtAutoFormat.Style.SYMBOL,locale,3), NbtFormat.getSymbolInstance(locale,3));
        assertEquals(NbtFormat.getCoinInstance(), NbtFormat.getInstance(0));
        assertEquals(NbtFormat.getCoinInstance(3), NbtFormat.getInstance(0,3));
        assertEquals(NbtFormat.getCoinInstance(3,4,5), NbtFormat.getInstance(0,3,4,5));
        assertEquals(NbtFormat.getCoinInstance(locale), NbtFormat.getInstance(0,locale));
        assertEquals(NbtFormat.getCoinInstance(locale,4,5), NbtFormat.getInstance(0,locale,4,5));
    }

    @Test
    public void autoDecimalTest() {
        NbtFormat codedZero = NbtFormat.getCodeInstance(Locale.US, 0);
        NbtFormat symbolZero = NbtFormat.getSymbolInstance(Locale.US, 0);
        assertEquals("𝑵1", symbolZero.format(COIN));
        assertEquals("NBT 1", codedZero.format(COIN));
        assertEquals("𝑵1,000", symbolZero.format(COIN.multiply(1000)));
        assertEquals("NBT 1,000", codedZero.format(COIN.multiply(1000)));

        NbtFormat codedTwo = NbtFormat.getCodeInstance(Locale.US, 2);
        NbtFormat symbolTwo = NbtFormat.getSymbolInstance(Locale.US, 2);
        assertEquals("𝑵1.00", symbolTwo.format(COIN));
        assertEquals("NBT 1.00", codedTwo.format(COIN));
        assertEquals("𝑵1,000.00", symbolTwo.format(COIN.multiply(1000)));
        assertEquals("NBT 1,000.00", codedTwo.format(COIN.multiply(1000)));

        NbtFormat codedThree = NbtFormat.getCodeInstance(Locale.US, 3);
        NbtFormat symbolThree = NbtFormat.getSymbolInstance(Locale.US, 3);
        assertEquals("𝑵1.000", symbolThree.format(COIN));
        assertEquals("NBT 1.000", codedThree.format(COIN));
        assertEquals("𝑵1,000.000", symbolThree.format(COIN.multiply(1000)));
        assertEquals("NBT 1,000.000", codedThree.format(COIN.multiply(1000)));
    }


    @Test
    public void symbolsCodesTest() {
        NbtFixedFormat coin = (NbtFixedFormat)NbtFormat.getCoinInstance(US);
        assertEquals("NBT", coin.code());
        assertEquals("𝑵", coin.symbol());
        NbtFixedFormat cent = (NbtFixedFormat)NbtFormat.getInstance(2, US);
        assertEquals("cNBT", cent.code());
        assertEquals("¢𝑵", cent.symbol());
        NbtFixedFormat deka = (NbtFixedFormat)NbtFormat.getInstance(-1, US);
        assertEquals("daNBT", deka.code());
        assertEquals("da𝑵", deka.symbol());
        NbtFixedFormat hecto = (NbtFixedFormat)NbtFormat.getInstance(-2, US);
        assertEquals("hNBT", hecto.code());
        assertEquals("h𝑵", hecto.symbol());
        NbtFixedFormat kilo = (NbtFixedFormat)NbtFormat.getInstance(-3, US);
        assertEquals("kNBT", kilo.code());
        assertEquals("k𝑵", kilo.symbol());
        NbtFixedFormat mega = (NbtFixedFormat)NbtFormat.getInstance(-6, US);
        assertEquals("MNBT", mega.code());
        assertEquals("M𝑵", mega.symbol());
        NbtFixedFormat noSymbol = (NbtFixedFormat)NbtFormat.getInstance(4, US);
        try {
            noSymbol.symbol();
            fail("non-standard denomination has no symbol()");
        } catch (IllegalStateException e) {}
        try {
            noSymbol.code();
            fail("non-standard denomination has no code()");
        } catch (IllegalStateException e) {}

        NbtFixedFormat symbolCoin = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(0).
                                                              symbol("\uD835\uDC75").build();
        assertEquals("NBT", symbolCoin.code());
        assertEquals("𝑵", symbolCoin.symbol());
        NbtFixedFormat symbolCent = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(2).
                                                              symbol("\uD835\uDC75").build();
        assertEquals("cNBT", symbolCent.code());
        assertEquals("¢𝑵", symbolCent.symbol());
        NbtFixedFormat symbolDeka = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-1).
		                                                       symbol("\uD835\uDC75").build();
        assertEquals("daNBT", symbolDeka.code());
        assertEquals("da𝑵", symbolDeka.symbol());
        NbtFixedFormat symbolHecto = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-2).
                                                               symbol("\uD835\uDC75").build();
        assertEquals("hNBT", symbolHecto.code());
        assertEquals("h𝑵", symbolHecto.symbol());
        NbtFixedFormat symbolKilo = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-3).
                                                              symbol("\uD835\uDC75").build();
        assertEquals("kNBT", symbolKilo.code());
        assertEquals("k𝑵", symbolKilo.symbol());
        NbtFixedFormat symbolMega = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-6).
                                                              symbol("\uD835\uDC75").build();
        assertEquals("MNBT", symbolMega.code());
        assertEquals("M𝑵", symbolMega.symbol());

        NbtFixedFormat codeCoin = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(0).
                                                            code("Nbt").build();
        assertEquals("Nbt", codeCoin.code());
        assertEquals("𝑵", codeCoin.symbol());
        NbtFixedFormat codeCent = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(2).
                                                            code("Nbt").build();
        assertEquals("cNbt", codeCent.code());
        assertEquals("¢𝑵", codeCent.symbol());
        NbtFixedFormat codeDeka = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-1).
                                                            code("Nbt").build();
        assertEquals("daNbt", codeDeka.code());
        assertEquals("da𝑵", codeDeka.symbol());
        NbtFixedFormat codeHecto = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-2).
                                                             code("Nbt").build();
        assertEquals("hNbt", codeHecto.code());
        assertEquals("h𝑵", codeHecto.symbol());
        NbtFixedFormat codeKilo = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-3).
                                                            code("Nbt").build();
        assertEquals("kNbt", codeKilo.code());
        assertEquals("k𝑵", codeKilo.symbol());
        NbtFixedFormat codeMega = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-6).
                                                            code("Nbt").build();
        assertEquals("MNbt", codeMega.code());
        assertEquals("M𝑵", codeMega.symbol());

        NbtFixedFormat symbolCodeCoin = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(0).
                                                                  symbol("\uD835\uDC75").code("Nbt").build();
        assertEquals("Nbt", symbolCodeCoin.code());
        assertEquals("𝑵", symbolCodeCoin.symbol());
        NbtFixedFormat symbolCodeCent = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(2).
                                                                  symbol("\uD835\uDC75").code("Nbt").build();
        assertEquals("cNbt", symbolCodeCent.code());
        assertEquals("¢𝑵", symbolCodeCent.symbol());
		NbtFixedFormat symbolCodeDeka = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-1).
		                                                       symbol("\uD835\uDC75").code("Nbt").build();
        assertEquals("daNbt", symbolCodeDeka.code());
        assertEquals("da𝑵", symbolCodeDeka.symbol());
        NbtFixedFormat symbolCodeHecto = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-2).
                                                                   symbol("\uD835\uDC75").code("Nbt").build();
        assertEquals("hNbt", symbolCodeHecto.code());
        assertEquals("h𝑵", symbolCodeHecto.symbol());
        NbtFixedFormat symbolCodeKilo = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-3).
                                                                  symbol("\uD835\uDC75").code("Nbt").build();
        assertEquals("kNbt", symbolCodeKilo.code());
        assertEquals("k𝑵", symbolCodeKilo.symbol());
        NbtFixedFormat symbolCodeMega = (NbtFixedFormat)NbtFormat.builder().locale(US).scale(-6).
                                                                  symbol("\uD835\uDC75").code("Nbt").build();
        assertEquals("MNbt", symbolCodeMega.code());
        assertEquals("M𝑵", symbolCodeMega.symbol());
    }

    /* copied from CoinFormatTest.java and modified */
    @Test
    public void parse() throws Exception {
        NbtFormat coin = NbtFormat.getCoinInstance(Locale.US);
        assertEquals(Coin.COIN, coin.parseObject("1"));
        assertEquals(Coin.COIN, coin.parseObject("1."));
        assertEquals(Coin.COIN, coin.parseObject("1.0"));
        assertEquals(Coin.COIN, NbtFormat.getCoinInstance(Locale.GERMANY).parseObject("1,0"));
        assertEquals(Coin.COIN, coin.parseObject("01.0000000000"));
        // TODO work with express positive sign
        // assertEquals(Coin.COIN, coin.parseObject("+1.0"));
        assertEquals(Coin.COIN.negate(), coin.parseObject("-1"));
        assertEquals(Coin.COIN.negate(), coin.parseObject("-1.0"));

        assertEquals(Coin.CENT, coin.parseObject(".01"));
		
    }

    /* Copied (and modified) from CoinFormatTest.java */
    @Test
    public void nbtRounding() throws Exception {
        NbtFormat coinFormat = NbtFormat.getCoinInstance(Locale.US);
        assertEquals("0", NbtFormat.getCoinInstance(Locale.US, 0).format(ZERO));
        assertEquals("0", coinFormat.format(ZERO, 0));
        assertEquals("0.00", NbtFormat.getCoinInstance(Locale.US, 2).format(ZERO));
        assertEquals("0.00", coinFormat.format(ZERO, 2));

        assertEquals("1", NbtFormat.getCoinInstance(Locale.US, 0).format(COIN));
        assertEquals("1", coinFormat.format(COIN, 0));
        assertEquals("1.0", NbtFormat.getCoinInstance(Locale.US, 1).format(COIN));
        assertEquals("1.0", coinFormat.format(COIN, 1));
        assertEquals("1.00", NbtFormat.getCoinInstance(Locale.US, 2, 2).format(COIN));
        assertEquals("1.00", coinFormat.format(COIN, 2, 2));
        assertEquals("1.00", NbtFormat.getCoinInstance(Locale.US, 2, 2, 2).format(COIN));
        assertEquals("1.00", coinFormat.format(COIN, 2, 2, 2));
        assertEquals("1.00", NbtFormat.getCoinInstance(Locale.US, 2, 2, 2, 2).format(COIN));
        assertEquals("1.00", coinFormat.format(COIN, 2, 2, 2, 2));
        assertEquals("1.000", NbtFormat.getCoinInstance(Locale.US, 3).format(COIN));
        assertEquals("1.000", coinFormat.format(COIN, 3));
        assertEquals("1.0000", NbtFormat.getCoinInstance(US, 4).format(COIN));
        assertEquals("1.0000", coinFormat.format(COIN, 4));

        final Coin justNot = COIN.subtract(SATOSHI);
        assertEquals("1", NbtFormat.getCoinInstance(US, 0).format(justNot));
        assertEquals("1", coinFormat.format(justNot, 0));
        assertEquals("1.0", NbtFormat.getCoinInstance(US, 1).format(justNot));
        assertEquals("1.0", coinFormat.format(justNot, 1));
        final Coin justNotUnder = Coin.valueOf(9950);
        assertEquals("1.00", NbtFormat.getCoinInstance(US, 2).format(justNot));
        assertEquals("1.00", coinFormat.format(justNot, 2));
        assertEquals("1.00", NbtFormat.getCoinInstance(US, 2).format(justNotUnder));
        assertEquals("1.00", coinFormat.format(justNotUnder, 2));
        assertEquals("1.00", NbtFormat.getCoinInstance(US, 2).format(justNot));
        assertEquals("1.00", coinFormat.format(justNot, 2));
        assertEquals("0.9950", NbtFormat.getCoinInstance(US, 2, 2).format(justNotUnder));
        assertEquals("0.9950", coinFormat.format(justNotUnder, 2, 2));
        assertEquals("0.9999", NbtFormat.getCoinInstance(US, 2, 2).format(justNot));
        assertEquals("0.9999", coinFormat.format(justNot, 2, 2));
        assertEquals("0.9999", NbtFormat.getCoinInstance(US, 2, REPEATING_DOUBLETS).format(justNot));
        assertEquals("0.9999", coinFormat.format(justNot, 2, REPEATING_DOUBLETS));
        assertEquals("0.9950", NbtFormat.getCoinInstance(US, 2, 2).format(justNotUnder));
        assertEquals("0.9950", coinFormat.format(justNotUnder, 2, 2));
        assertEquals("0.9950", NbtFormat.getCoinInstance(US, 2, REPEATING_DOUBLETS).format(justNotUnder));
        assertEquals("0.9950", coinFormat.format(justNotUnder, 2, REPEATING_DOUBLETS));
        assertEquals("1.000", NbtFormat.getCoinInstance(US, 3).format(justNot));
        assertEquals("1.000", coinFormat.format(justNot, 3));
        assertEquals("0.9999", NbtFormat.getCoinInstance(US, 4).format(justNot));
        assertEquals("0.9999", coinFormat.format(justNot, 4));

        final Coin slightlyMore = COIN.add(SATOSHI);
        assertEquals("1", NbtFormat.getCoinInstance(US, 0).format(slightlyMore));
        assertEquals("1", coinFormat.format(slightlyMore, 0));
        assertEquals("1.0", NbtFormat.getCoinInstance(US, 1).format(slightlyMore));
        assertEquals("1.0", coinFormat.format(slightlyMore, 1));
        assertEquals("1.00", NbtFormat.getCoinInstance(US, 2).format(slightlyMore));
        assertEquals("1.00", coinFormat.format(slightlyMore, 2));
        assertEquals("1.0001", NbtFormat.getCoinInstance(US, 2, 2).format(slightlyMore));
        assertEquals("1.0001", coinFormat.format(slightlyMore, 2, 2));
        assertEquals("1.000", NbtFormat.getCoinInstance(US, 3).format(slightlyMore));
        assertEquals("1.000", coinFormat.format(slightlyMore, 3));
        assertEquals("1.0001", NbtFormat.getCoinInstance(US, 4).format(slightlyMore));
        assertEquals("1.0001", coinFormat.format(slightlyMore, 4));

        final Coin pivot = COIN.add(SATOSHI.multiply(5));
        assertEquals("1.0005", NbtFormat.getCoinInstance(US, 4).format(pivot));
        assertEquals("1.0005", coinFormat.format(pivot, 4));
        assertEquals("1.0005", NbtFormat.getCoinInstance(US, 3, 1).format(pivot));
        assertEquals("1.0005", coinFormat.format(pivot, 3, 1));
        assertEquals("1.001", NbtFormat.getCoinInstance(US, 3).format(pivot));
        assertEquals("1.001", coinFormat.format(pivot, 3));

        final Coin value = Coin.valueOf(112233445566l);
        assertEquals("11,223,345", NbtFormat.getCoinInstance(US, 0).format(value));
        assertEquals("11,223,345", coinFormat.format(value, 0));
        assertEquals("11,223,344.6", NbtFormat.getCoinInstance(US, 1).format(value));
        assertEquals("11,223,344.6", coinFormat.format(value, 1));
		assertEquals("11,223,344.557", NbtFormat.getCoinInstance(US, 2, 1).format(value));
        assertEquals("11,223,344.557", coinFormat.format(value, 2, 1));
        assertEquals("11,223,344.5566", NbtFormat.getCoinInstance(US, 2, 2).format(value));
        assertEquals("11,223,344.5566", coinFormat.format(value, 2, 2));
        assertEquals("11,223,344.557", NbtFormat.getCoinInstance(US, 3).format(value));
        assertEquals("11,223,344.557", coinFormat.format(value, 3));
        assertEquals("11,223,344.5566", NbtFormat.getCoinInstance(US, 4).format(value));
        assertEquals("11,223,344.5566", coinFormat.format(value, 4));

        NbtFormat megaFormat = NbtFormat.getInstance(-6, US);
        assertEquals("200,000.00", megaFormat.format(MAX_MONEY));
        assertEquals("200,000", megaFormat.format(MAX_MONEY, 0));
        assertEquals("11.2233445566", megaFormat.format(value, 0, REPEATING_DOUBLETS));
    }

    @Test
    public void negativeTest() throws Exception {
        assertEquals("-1,00 NBT", NbtFormat.getInstance(FRANCE).format(COIN.multiply(-1)));
        assertEquals("NBT -1,00", NbtFormat.getInstance(ITALY).format(COIN.multiply(-1)));
        assertEquals("𝑵 -1,00", NbtFormat.getSymbolInstance(ITALY).format(COIN.multiply(-1)));
        assertEquals("NBT -1.00", NbtFormat.getInstance(JAPAN).format(COIN.multiply(-1)));
        assertEquals("𝑵-1.00", NbtFormat.getSymbolInstance(JAPAN).format(COIN.multiply(-1)));
        assertEquals("(NBT 1.00)", NbtFormat.getInstance(US).format(COIN.multiply(-1)));
        assertEquals("(𝑵1.00)", NbtFormat.getSymbolInstance(US).format(COIN.multiply(-1)));
        // assertEquals("NBT -१.००", NbtFormat.getInstance(Locale.forLanguageTag("hi-IN")).format(COIN.multiply(-1)));
        assertEquals("NBT -๑.๐๐", NbtFormat.getInstance(new Locale("th","TH","TH")).format(COIN.multiply(-1)));
        assertEquals("𝑵-๑.๐๐", NbtFormat.getSymbolInstance(new Locale("th","TH","TH")).format(COIN.multiply(-1)));
    }

    /* Warning: these tests assume the state of Locale data extant on the platform on which
     * they were written: openjdk 7u21-2.3.9-5 */
    @Test
    public void equalityTest() throws Exception {
        // First, autodenominator
        assertEquals(NbtFormat.getInstance(), NbtFormat.getInstance());
        assertEquals(NbtFormat.getInstance().hashCode(), NbtFormat.getInstance().hashCode());

        assertNotEquals(NbtFormat.getCodeInstance(), NbtFormat.getSymbolInstance());
        assertNotEquals(NbtFormat.getCodeInstance().hashCode(), NbtFormat.getSymbolInstance().hashCode());

        assertEquals(NbtFormat.getSymbolInstance(5), NbtFormat.getSymbolInstance(5));
        assertEquals(NbtFormat.getSymbolInstance(5).hashCode(), NbtFormat.getSymbolInstance(5).hashCode());

        assertNotEquals(NbtFormat.getSymbolInstance(5), NbtFormat.getSymbolInstance(4));
        assertNotEquals(NbtFormat.getSymbolInstance(5).hashCode(), NbtFormat.getSymbolInstance(4).hashCode());

        /* The underlying formatter is mutable, and its currency code
         * and symbol may be reset each time a number is
         * formatted or parsed.  Here we check to make sure that state is
         * ignored when comparing for equality */
        // when formatting
        NbtAutoFormat a = (NbtAutoFormat)NbtFormat.getSymbolInstance(US);
        NbtAutoFormat b = (NbtAutoFormat)NbtFormat.getSymbolInstance(US);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.format(COIN.multiply(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.format(COIN.divide(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // FRANCE and GERMANY have different pattterns
        assertNotEquals(NbtFormat.getInstance(FRANCE).hashCode(), NbtFormat.getInstance(GERMANY).hashCode());
        // TAIWAN and CHINA differ only in the Locale and Currency, i.e. the patterns and symbols are
        // all the same (after setting the currency symbols to nubitss)
        assertNotEquals(NbtFormat.getInstance(TAIWAN), NbtFormat.getInstance(CHINA));
        // but they hash the same because of the DecimalFormatSymbols.hashCode() implementation

        assertEquals(NbtFormat.getSymbolInstance(4), NbtFormat.getSymbolInstance(4));
        assertEquals(NbtFormat.getSymbolInstance(4).hashCode(), NbtFormat.getSymbolInstance(4).hashCode());

        assertNotEquals(NbtFormat.getSymbolInstance(4), NbtFormat.getSymbolInstance(5));
        assertNotEquals(NbtFormat.getSymbolInstance(4).hashCode(), NbtFormat.getSymbolInstance(5).hashCode());

        // Fixed-denomination
        assertEquals(NbtFormat.getCoinInstance(), NbtFormat.getCoinInstance());
        assertEquals(NbtFormat.getCoinInstance().hashCode(), NbtFormat.getCoinInstance().hashCode());

        assertEquals(NbtFormat.getInstance(-6), NbtFormat.getInstance(-6));
        assertEquals(NbtFormat.getInstance(-6).hashCode(), NbtFormat.getInstance(-6).hashCode());

        assertNotEquals(NbtFormat.getInstance(SMALLEST_UNIT_EXPONENT),
                        NbtFormat.getInstance(SMALLEST_UNIT_EXPONENT - 1));
        assertNotEquals(NbtFormat.getInstance(SMALLEST_UNIT_EXPONENT).hashCode(),
                        NbtFormat.getInstance(SMALLEST_UNIT_EXPONENT - 1).hashCode());

        assertNotEquals(NbtFormat.getCoinInstance(TAIWAN), NbtFormat.getCoinInstance(CHINA));

        assertNotEquals(NbtFormat.getCoinInstance(2,3), NbtFormat.getCoinInstance(2,4));
        assertNotEquals(NbtFormat.getCoinInstance(2,3).hashCode(), NbtFormat.getCoinInstance(2,4).hashCode());

        assertNotEquals(NbtFormat.getCoinInstance(2,3), NbtFormat.getCoinInstance(2,3,3));
        assertNotEquals(NbtFormat.getCoinInstance(2,3).hashCode(), NbtFormat.getCoinInstance(2,3,3).hashCode());


    }

    @Test
    public void attributeTest() throws Exception {
        String codePat = NbtFormat.getCodeInstance(Locale.US).pattern();
        assertTrue(codePat.contains("NBT") && ! codePat.contains("(^|[^𝑵])𝑵([^𝑵]|$)") && ! codePat.contains("(^|[^¤])¤([^¤]|$)"));
        String symPat = NbtFormat.getSymbolInstance(Locale.US).pattern();
        assertTrue(symPat.contains("𝑵") && !symPat.contains("NBT") && !symPat.contains("¤¤"));

        assertEquals("NBT #,##0.00;(NBT #,##0.00)", NbtFormat.getCodeInstance(Locale.US).pattern());
        assertEquals("𝑵#,##0.00;(𝑵#,##0.00)", NbtFormat.getSymbolInstance(Locale.US).pattern());
        assertEquals('0', NbtFormat.getInstance(Locale.US).symbols().getZeroDigit());
        // assertEquals('०', NbtFormat.getInstance(Locale.forLanguageTag("hi-IN")).symbols().getZeroDigit());
        // TODO will this next line work with other JREs?
        assertEquals('๐', NbtFormat.getInstance(new Locale("th","TH","TH")).symbols().getZeroDigit());
    }

    @Test
    public void toStringTest() {
        assertEquals("Auto-format 𝑵#,##0.00;(𝑵#,##0.00)", NbtFormat.getSymbolInstance(Locale.US).toString());
        assertEquals("Auto-format 𝑵#,##0.0000;(𝑵#,##0.0000)", NbtFormat.getSymbolInstance(Locale.US, 4).toString());
        assertEquals("Auto-format NBT #,##0.00;(NBT #,##0.00)", NbtFormat.getCodeInstance(Locale.US).toString());
        assertEquals("Auto-format NBT #,##0.0000;(NBT #,##0.0000)", NbtFormat.getCodeInstance(Locale.US, 4).toString());
        assertEquals("Coin-format #,##0.00", NbtFormat.getCoinInstance(Locale.US).toString());
        assertEquals("Coin-format #,##0.000", NbtFormat.getCoinInstance(Locale.US,3).toString());
        assertEquals("Coin-format #,##0.000(####)(#######)", NbtFormat.getCoinInstance(Locale.US,3,4,7).toString());
        assertEquals("Kilocoin-format #,##0.000", NbtFormat.getInstance(-3,Locale.US,3).toString());
        assertEquals("Kilocoin-format #,##0.000(####)(#######)", NbtFormat.getInstance(-3,Locale.US,3,4,7).toString());
        assertEquals("Decicoin-format #,##0.000", NbtFormat.getInstance(1,Locale.US,3).toString());
        assertEquals("Decicoin-format #,##0.000(####)(#######)", NbtFormat.getInstance(1,Locale.US,3,4,7).toString());
        assertEquals("Dekacoin-format #,##0.000", NbtFormat.getInstance(-1,Locale.US,3).toString());
        assertEquals("Dekacoin-format #,##0.000(####)(#######)", NbtFormat.getInstance(-1,Locale.US,3,4,7).toString());
        assertEquals("Hectocoin-format #,##0.000", NbtFormat.getInstance(-2,Locale.US,3).toString());
        assertEquals("Hectocoin-format #,##0.000(####)(#######)", NbtFormat.getInstance(-2,Locale.US,3,4,7).toString());
        assertEquals("Megacoin-format #,##0.000", NbtFormat.getInstance(-6,Locale.US,3).toString());
        assertEquals("Megacoin-format #,##0.000(####)(#######)", NbtFormat.getInstance(-6,Locale.US,3,4,7).toString());
        assertEquals("Fixed (-4) format #,##0.000", NbtFormat.getInstance(-4,Locale.US,3).toString());
        assertEquals("Fixed (-4) format #,##0.000(####)", NbtFormat.getInstance(-4,Locale.US,3,4).toString());
        assertEquals("Fixed (-4) format #,##0.000(####)(#######)",
                     NbtFormat.getInstance(-4, Locale.US, 3, 4, 7).toString());

        assertEquals("Auto-format 𝑵#,##0.00;(𝑵#,##0.00)",
                     NbtFormat.builder().style(SYMBOL).code("USD").locale(US).build().toString());
        assertEquals("Auto-format #.##0,00 $",
                     NbtFormat.builder().style(SYMBOL).symbol("$").locale(GERMANY).build().toString());
        assertEquals("Auto-format #.##0,0000 $",
                     NbtFormat.builder().style(SYMBOL).symbol("$").fractionDigits(4).locale(GERMANY).build().toString());
        assertEquals("Auto-format NBT#,00𝑵;NBT-#,00𝑵",
                     NbtFormat.builder().style(SYMBOL).locale(GERMANY).pattern("¤¤#¤").build().toString());
        assertEquals("Coin-format NBT#,00𝑵;NBT-#,00𝑵",
                     NbtFormat.builder().scale(0).locale(GERMANY).pattern("¤¤#¤").build().toString());
        assertEquals("Millicoin-format NBT#.00𝑵;NBT-#.00𝑵",
                     NbtFormat.builder().scale(3).locale(US).pattern("¤¤#¤").build().toString());
    }

    @Test
    public void patternDecimalPlaces() {
        /* The pattern format provided by DecimalFormat includes specification of fractional digits,
         * but we ignore that because we have alternative mechanism for specifying that.. */
        NbtFormat f = NbtFormat.builder().locale(US).scale(3).pattern("¤¤ #.0").fractionDigits(3).build();
        assertEquals("Millicoin-format NBT #.000;NBT -#.000", f.toString());
    }

    @Test
    public void builderTest() {
        Locale locale;
        if (Locale.getDefault().equals(GERMANY)) locale = FRANCE;
        else locale = GERMANY;

        assertEquals(NbtFormat.builder().build(), NbtFormat.getCoinInstance());
        try {
            NbtFormat.builder().scale(0).style(CODE);
            fail("Invoking both scale() and style() on a Builder should raise exception");
        } catch (IllegalStateException e) {}
        try {
            NbtFormat.builder().style(CODE).scale(0);
            fail("Invoking both style() and scale() on a Builder should raise exception");
        } catch (IllegalStateException e) {}

        NbtFormat built = NbtFormat.builder().style(NbtAutoFormat.Style.CODE).fractionDigits(4).build();
        assertEquals(built, NbtFormat.getCodeInstance(4));
        built = NbtFormat.builder().style(NbtAutoFormat.Style.SYMBOL).fractionDigits(4).build();
        assertEquals(built, NbtFormat.getSymbolInstance(4));

        built = NbtFormat.builder().scale(0).build();
        assertEquals(built, NbtFormat.getCoinInstance());

        built = NbtFormat.builder().locale(locale).scale(0).build();
        assertEquals(built, NbtFormat.getCoinInstance(locale));

        built = NbtFormat.builder().minimumFractionDigits(3).scale(0).build();
        assertEquals(built, NbtFormat.getCoinInstance(3));

        built = NbtFormat.builder().fractionGroups(3,4).scale(0).build();
        assertEquals(built, NbtFormat.getCoinInstance(2,3,4));

        built = NbtFormat.builder().pattern("#,###.#").scale(3).locale(GERMANY).build();
        assertEquals("1.000,0", built.format(COIN));
        built = NbtFormat.builder().pattern("#,###.#").scale(3).locale(GERMANY).build();
        assertEquals("-1.000,0", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().localizedPattern("#.###,#").scale(3).locale(GERMANY).build();
        assertEquals("1.000,0", built.format(COIN));

        built = NbtFormat.builder().pattern("¤#,###.#").style(CODE).locale(GERMANY).build();
        assertEquals("𝑵-1,00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("¤¤ #,###.#").style(SYMBOL).locale(GERMANY).build();
        assertEquals("NBT -1,00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("¤¤##,###.#").scale(3).locale(US).build();

        try {
            NbtFormat.builder().pattern("¤¤##,###.#").scale(4).locale(US).build().format(COIN);
            fail("Pattern with currency sign and non-standard denomination should raise exception");
        } catch (IllegalStateException e) {}

        try {
            NbtFormat.builder().localizedPattern("¤¤##,###.#").scale(4).locale(US).build().format(COIN);
            fail("Localized pattern with currency sign and non-standard denomination should raise exception");
        } catch (IllegalStateException e) {}

        built = NbtFormat.builder().style(SYMBOL).symbol("\uD835\uDC75").locale(US).build();
        assertEquals("𝑵1.00", built.format(COIN));
        built = NbtFormat.builder().style(CODE).code("NBT").locale(US).build();
        assertEquals("NBT 1.00", built.format(COIN));
        built = NbtFormat.builder().style(SYMBOL).symbol("$").locale(GERMANY).build();
        assertEquals("1,00 $", built.format(COIN));
        // Setting the currency code on a DecimalFormatSymbols object can affect the currency symbol.
        built = NbtFormat.builder().style(SYMBOL).code("USD").locale(US).build();
        assertEquals("𝑵1.00", built.format(COIN));

        /* The prefix of a pattern can have number symbols in quotes.
         * Make sure our custom negative-subpattern creator handles this. */
        built = NbtFormat.builder().pattern("'#'¤#0").scale(0).locale(US).build();
        assertEquals("#𝑵-1.00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("'#0'¤#0").scale(0).locale(US).build();
        assertEquals("#0𝑵-1.00", built.format(COIN.multiply(-1)));
        // this is an escaped quote between two hash marks in one set of quotes, not
        // two adjacent quote-enclosed hash-marks:
        built = NbtFormat.builder().pattern("'#''#'¤#0").scale(0).locale(US).build();
        assertEquals("#'#𝑵-1.00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("'#0''#'¤#0").scale(0).locale(US).build();
        assertEquals("#0'#𝑵-1.00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("'#0#'¤#0").scale(0).locale(US).build();
        assertEquals("#0#𝑵-1.00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("'#0'E'#'¤#0").scale(0).locale(US).build();
        assertEquals("#0E#𝑵-1.00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("E'#0''#'¤#0").scale(0).locale(US).build();
        assertEquals("E#0'#𝑵-1.00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("E'#0#'¤#0").scale(0).locale(US).build();
        assertEquals("E#0#𝑵-1.00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("E'#0''''#'¤#0").scale(0).locale(US).build();
        assertEquals("E#0''#𝑵-1.00", built.format(COIN.multiply(-1)));
        built = NbtFormat.builder().pattern("''#0").scale(0).locale(US).build();
        assertEquals("'-1.00", built.format(COIN.multiply(-1)));

        // immutability check for fixed-denomination formatters, w/ & w/o custom pattern
        NbtFormat a = NbtFormat.builder().scale(3).build();
        NbtFormat b = NbtFormat.builder().scale(3).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.format(COIN.multiply(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.format(COIN.divide(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a = NbtFormat.builder().scale(3).pattern("¤#.#").build();
        b = NbtFormat.builder().scale(3).pattern("¤#.#").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.format(COIN.multiply(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.format(COIN.divide(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

    }

}

