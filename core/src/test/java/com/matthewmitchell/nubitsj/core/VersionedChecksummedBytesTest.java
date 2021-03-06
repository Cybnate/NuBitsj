/**
 * Copyright 2014 BitcoinJ Project
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

package com.matthewmitchell.nubitsj.core;

import com.matthewmitchell.nubitsj.params.MainNetParams;
import com.matthewmitchell.nubitsj.params.UnitTestParams;
import org.junit.Test;

import static com.matthewmitchell.nubitsj.core.Utils.HEX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class VersionedChecksummedBytesTest {
    static final NetworkParameters testParams = UnitTestParams.get();
    static final NetworkParameters mainParams = MainNetParams.get();

    @Test
    public void stringification() throws Exception {
        // Test a testnet address.
        VersionedChecksummedBytes a = new VersionedChecksummedBytes(testParams.getAddressHeader(), HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        assertEquals("BTaHMSxmmj1Tn17HYWyJFNxR1qcFgM1yq8", a.toString());

        VersionedChecksummedBytes b = new VersionedChecksummedBytes(mainParams.getAddressHeader(), HEX.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));
        assertEquals("BBD5GQWZtBe3i6QnqUmdFXYF8HRNsow6G1", b.toString());
    }

    @Test
    public void cloning() throws Exception {
        VersionedChecksummedBytes a = new VersionedChecksummedBytes(testParams.getAddressHeader(), HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        VersionedChecksummedBytes b = a.clone();

        assertEquals(a, b);
        assertNotSame(a, b);
    }

    @Test
    public void comparisonCloneEqualTo() throws Exception {
        VersionedChecksummedBytes a = new VersionedChecksummedBytes(testParams.getAddressHeader(), HEX.decode("fda79a24e50ff70ff42f7d89585da5bd19d9e5cc"));
        VersionedChecksummedBytes b = a.clone();

        assertTrue(a.compareTo(b) == 0);
    }
}
