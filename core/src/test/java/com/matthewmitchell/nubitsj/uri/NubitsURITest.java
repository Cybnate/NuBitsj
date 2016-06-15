/*
 * Copyright 2012, 2014 the original author or authors.
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
 * 
 */

package com.matthewmitchell.nubitsj.uri;

import com.matthewmitchell.nubitsj.core.Address;
import com.matthewmitchell.nubitsj.params.MainNetParams;
import com.google.common.collect.ImmutableList;
import org.junit.Test;


import static com.matthewmitchell.nubitsj.core.Coin.*;
import static org.junit.Assert.*;

public class NubitsURITest {
    private NubitsURI testObject = null;

    private static final String MAINNET_GOOD_ADDRESS = "BiM5wdu2apVnp17h6uZTWcNbbPE93sDeGs";
    private static final String MAINNET_BAD_ADDRESS = "mranY19RYUjgJjXY4BJNYp88WXXAg7Pr9T";

    @Test
    public void testConvertToNubitsURI() throws Exception {
        Address goodAddress = new Address(MainNetParams.get(), MAINNET_GOOD_ADDRESS);
        
        // simple example
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello&message=AMessage", NubitsURI.convertToNubitsURI(goodAddress, parseCoin("12.34"), "Hello", "AMessage"));
        
        // example with spaces, ampersand and plus
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello%20World&message=Mess%20%26%20age%20%2B%20hope", NubitsURI.convertToNubitsURI(goodAddress, parseCoin("12.34"), "Hello World", "Mess & age + hope"));

        // no amount, label present, message present
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?label=Hello&message=glory", NubitsURI.convertToNubitsURI(goodAddress, null, "Hello", "glory"));
        
        // amount present, no label, message present
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?amount=0.1&message=glory", NubitsURI.convertToNubitsURI(goodAddress, parseCoin("0.1"), null, "glory"));
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?amount=0.1&message=glory", NubitsURI.convertToNubitsURI(goodAddress, parseCoin("0.1"), "", "glory"));

        // amount present, label present, no message
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello", NubitsURI.convertToNubitsURI(goodAddress, parseCoin("12.34"), "Hello", null));
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?amount=12.34&label=Hello", NubitsURI.convertToNubitsURI(goodAddress, parseCoin("12.34"), "Hello", ""));
              
        // amount present, no label, no message
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?amount=1000", NubitsURI.convertToNubitsURI(goodAddress, parseCoin("1000"), null, null));
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?amount=1000", NubitsURI.convertToNubitsURI(goodAddress, parseCoin("1000"), "", ""));
        
        // no amount, label present, no message
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?label=Hello", NubitsURI.convertToNubitsURI(goodAddress, null, "Hello", null));
        
        // no amount, no label, message present
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?message=Agatha", NubitsURI.convertToNubitsURI(goodAddress, null, null, "Agatha"));
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS + "?message=Agatha", NubitsURI.convertToNubitsURI(goodAddress, null, "", "Agatha"));
      
        // no amount, no label, no message
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS, NubitsURI.convertToNubitsURI(goodAddress, null, null, null));
        assertEquals("Nu:" + MAINNET_GOOD_ADDRESS, NubitsURI.convertToNubitsURI(goodAddress, null, "", ""));
    }

    @Test
    public void testGood_Simple() throws NubitsURIParseException {
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS);
        assertNotNull(testObject);
        assertNull("Unexpected amount", testObject.getAmount());
        assertNull("Unexpected label", testObject.getLabel());
        assertEquals("Unexpected label", 20, testObject.getAddress().getHash160().length);
    }

    /**
     * Test a broken URI (bad scheme)
     */
    @Test
    public void testBad_Scheme() {
        try {
            testObject = new NubitsURI(MainNetParams.get(), "blimpcoin:" + MAINNET_GOOD_ADDRESS);
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
        }
    }

    /**
     * Test a broken URI (bad syntax)
     */
    @Test
    public void testBad_BadSyntax() {
        // Various illegal characters
        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + "|" + MAINNET_GOOD_ADDRESS);
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }

        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "\\");
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }

        // Separator without field
        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":");
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
            assertTrue(e.getMessage().contains("Bad URI syntax"));
        }
    }

    /**
     * Test a broken URI (missing address)
     */
    @Test
    public void testBad_Address() {
        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME);
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
        }
    }

    /**
     * Test a broken URI (bad address type)
     */
    @Test
    public void testBad_IncorrectAddressType() {
        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_BAD_ADDRESS);
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
            assertTrue(e.getMessage().contains("Bad address"));
        }
    }

    /**
     * Handles a simple amount
     * 
     * @throws NubitsURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Amount() throws NubitsURIParseException {
        // Test the decimal parsing
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210.1234");
        assertEquals("65432101234", testObject.getAmount().toString());

        // Test the decimal parsing
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=.1234");
        assertEquals("1234", testObject.getAmount().toString());

        // Test the integer parsing
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=65432");
        assertEquals("654320000", testObject.getAmount().toString());
    }

    /**
     * Handles a simple label
     * 
     * @throws NubitsURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Label() throws NubitsURIParseException {
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?label=Hello%20World");
        assertEquals("Hello World", testObject.getLabel());
    }

    /**
     * Handles a simple label with an embedded ampersand and plus
     * 
     * @throws NubitsURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_LabelWithAmpersandAndPlus() throws NubitsURIParseException {
        String testString = "Hello Earth & Mars + Venus";
        String encodedLabel = NubitsURI.encodeURLString(testString);
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "?label="
                + encodedLabel);
        assertEquals(testString, testObject.getLabel());
    }

    /**
     * Handles a Russian label (Unicode test)
     * 
     * @throws NubitsURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_LabelWithRussian() throws NubitsURIParseException {
        // Moscow in Russian in Cyrillic
        String moscowString = "\u041c\u043e\u0441\u043a\u0432\u0430";
        String encodedLabel = NubitsURI.encodeURLString(moscowString); 
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS + "?label="
                + encodedLabel);
        assertEquals(moscowString, testObject.getLabel());
    }

    /**
     * Handles a simple message
     * 
     * @throws NubitsURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Message() throws NubitsURIParseException {
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?message=Hello%20World");
        assertEquals("Hello World", testObject.getMessage());
    }

    /**
     * Handles various well-formed combinations
     * 
     * @throws NubitsURIParseException
     *             If something goes wrong
     */
    @Test
    public void testGood_Combinations() throws NubitsURIParseException {
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=6543210&label=Hello%20World&message=Be%20well");
        assertEquals(
                "NubitsURI['amount'='65432100000','label'='Hello World','message'='Be well','address'='BiM5wdu2apVnp17h6uZTWcNbbPE93sDeGs']",
                testObject.toString());
    }

    /**
     * Handles a badly formatted amount field
     * 
     * @throws NubitsURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Amount() throws NubitsURIParseException {
        // Missing
        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?amount=");
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
            assertTrue(e.getMessage().contains("amount"));
        }

        // Non-decimal (BIP 21)
        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?amount=12X4");
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
            assertTrue(e.getMessage().contains("amount"));
        }
    }

    @Test
    public void testEmpty_Label() throws NubitsURIParseException {
        assertNull(new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?label=").getLabel());
    }

    @Test
    public void testEmpty_Message() throws NubitsURIParseException {
        assertNull(new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?message=").getMessage());
    }

    /**
     * Handles duplicated fields (sneaky address overwrite attack)
     * 
     * @throws NubitsURIParseException
     *             If something goes wrong
     */
    @Test
    public void testBad_Duplicated() throws NubitsURIParseException {
        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?address=aardvark");
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
            assertTrue(e.getMessage().contains("address"));
        }
    }

    @Test
    public void testGood_ManyEquals() throws NubitsURIParseException {
        assertEquals("aardvark=zebra", new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":"
                + MAINNET_GOOD_ADDRESS + "?label=aardvark=zebra").getLabel());
    }
    
    /**
     * Handles unknown fields (required and not required)
     * 
     * @throws NubitsURIParseException
     *             If something goes wrong
     */
    @Test
    public void testUnknown() throws NubitsURIParseException {
        // Unknown not required field
        testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?aardvark=true");
        assertEquals("NubitsURI['aardvark'='true','address'='BiM5wdu2apVnp17h6uZTWcNbbPE93sDeGs']", testObject.toString());

        assertEquals("true", testObject.getParameterByName("aardvark"));

        // Unknown not required field (isolated)
        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?aardvark");
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
            assertTrue(e.getMessage().contains("no separator"));
        }

        // Unknown and required field
        try {
            testObject = new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                    + "?req-aardvark=true");
            fail("Expecting NubitsURIParseException");
        } catch (NubitsURIParseException e) {
            assertTrue(e.getMessage().contains("req-aardvark"));
        }
    }

    @Test
    public void brokenURIs() throws NubitsURIParseException {
        // Check we can parse the incorrectly formatted URIs produced by blockchain.info and its iPhone app.
        String str = "Nu://BiM5wdu2apVnp17h6uZTWcNbbPE93sDeGs?amount=0.01000000";
        NubitsURI uri = new NubitsURI(MainNetParams.get(), str);
        assertEquals("BiM5wdu2apVnp17h6uZTWcNbbPE93sDeGs", uri.getAddress().toString());
        assertEquals(CENT, uri.getAmount());
    }

    @Test(expected = NubitsURIParseException.class)
    public void testBad_AmountTooPrecise() throws NubitsURIParseException {
        new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=0.123456789");
    }

    @Test(expected = NubitsURIParseException.class)
    public void testBad_NegativeAmount() throws NubitsURIParseException {
        new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=-1");
    }

    @Test(expected = NubitsURIParseException.class)
    public void testBad_TooLargeAmount() throws NubitsURIParseException {
        new NubitsURI(MainNetParams.get(), NubitsURI.NUBITS_SCHEME + ":" + MAINNET_GOOD_ADDRESS
                + "?amount=2000000000001");
    }

    @Test
    public void testPaymentProtocolReq() throws Exception {
        // Non-backwards compatible form ...
        NubitsURI uri = new NubitsURI(MainNetParams.get(), "Nu:?r=https%3A%2F%2Fnubitscore.org%2F%7Egavin%2Ff.php%3Fh%3Db0f02e7cea67f168e25ec9b9f9d584f9");
        assertEquals("https://nubitscore.org/~gavin/f.php?h=b0f02e7cea67f168e25ec9b9f9d584f9", uri.getPaymentRequestUrl());
        assertEquals(ImmutableList.of("https://nubitscore.org/~gavin/f.php?h=b0f02e7cea67f168e25ec9b9f9d584f9"),
                uri.getPaymentRequestUrls());
        assertNull(uri.getAddress());
    }

    @Test
    public void testMultiplePaymentProtocolReq() throws Exception {
        NubitsURI uri = new NubitsURI(MainNetParams.get(),
                "Nu:?r=https%3A%2F%2Fnubitscore.org%2F%7Egavin&r1=bt:112233445566");
        assertEquals(ImmutableList.of("bt:112233445566", "https://nubitscore.org/~gavin"), uri.getPaymentRequestUrls());
        assertEquals("https://nubitscore.org/~gavin", uri.getPaymentRequestUrl());
    }

    @Test
    public void testNoPaymentProtocolReq() throws Exception {
        NubitsURI uri = new NubitsURI(MainNetParams.get(), "Nu:" + MAINNET_GOOD_ADDRESS);
        assertNull(uri.getPaymentRequestUrl());
        assertEquals(ImmutableList.of(), uri.getPaymentRequestUrls());
        assertNotNull(uri.getAddress());
    }

    @Test
    public void testUnescapedPaymentProtocolReq() throws Exception {
        NubitsURI uri = new NubitsURI(MainNetParams.get(),
                "Nu:?r=https://merchant.com/pay.php?h%3D2a8628fc2fbe");
        assertEquals("https://merchant.com/pay.php?h=2a8628fc2fbe", uri.getPaymentRequestUrl());
        assertEquals(ImmutableList.of("https://merchant.com/pay.php?h=2a8628fc2fbe"), uri.getPaymentRequestUrls());
        assertNull(uri.getAddress());
    }
}

