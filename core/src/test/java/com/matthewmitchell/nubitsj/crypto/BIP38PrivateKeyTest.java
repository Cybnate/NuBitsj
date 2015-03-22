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

package com.matthewmitchell.nubitsj.crypto;

import com.matthewmitchell.nubitsj.core.ECKey;
import com.matthewmitchell.nubitsj.crypto.BIP38PrivateKey.BadPassphraseException;
import com.matthewmitchell.nubitsj.params.MainNetParams;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;

public class BIP38PrivateKeyTest {

    private static final MainNetParams MAINNET = MainNetParams.get();

    @Test
    public void bip38testvector_test1() throws Exception {
        BIP38PrivateKey encryptedKey = new BIP38PrivateKey(MAINNET,
                "6PRNhgoezXcrzkoSwugoo5m9DJvLiGj48Y27RsDnQpMGLH2WGtcsoZM4J6");
        ECKey key = encryptedKey.decrypt("TestingOneTwoThree");
        assertEquals("7RKVWf9kXjwZMJKrGpBsFwthpukyW6taTonWsmVbCoRQ1LUy4xC", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test
    public void bip38testvector_test2() throws Exception {
        BIP38PrivateKey encryptedKey = new BIP38PrivateKey(MAINNET,
                "6PRJ89RjK3SmsGbFgMC3N9BoyHhEMsCSprV3HPKJWUyT7spHFGawZ7XxB1");
        ECKey key = encryptedKey.decrypt("Satoshi");
        assertEquals("7RsAToJGRkjdn8m7WuQzyRbNtcrpH3MidmCqo3EeoXkLveiAq4C", key.getPrivateKeyEncoded(MAINNET)
                .toString());
    }

    @Test(expected = BadPassphraseException.class)
    public void badPassphrase() throws Exception {
        BIP38PrivateKey encryptedKey = new BIP38PrivateKey(MAINNET,
                "6PfRBdq72gua9HRkZS1X8DJLd8vRi7hbNjUkiauLkThWvd52eMyGmeS7vc");
        encryptedKey.decrypt("BAD");
    }

}

