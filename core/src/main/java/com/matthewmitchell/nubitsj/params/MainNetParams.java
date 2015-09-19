/*
 * Copyright 2013 Google Inc.
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

package com.matthewmitchell.nubitsj.params;

import com.matthewmitchell.nubitsj.core.NetworkParameters;
import com.matthewmitchell.nubitsj.core.Sha256Hash;
import com.matthewmitchell.nubitsj.core.Utils;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends NetworkParameters {
    public MainNetParams() {
        super();
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;
        dumpedPrivateKeyHeader = 191;
        addressHeader = 25;
        p2shHeader = 26;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
        port = 7890;
        packetMagic= 0xe6e8e9e5L;
        genesisBlock.setDifficultyTarget(0x1e0fffffL);
        genesisBlock.setTime(1407023435);
        genesisBlock.setNonce(1542387L);
        id = ID_MAINNET;
        spendableCoinbaseDepth = 500;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("000003cc2da5a0a289ad0a590c20a8b975219ddc1204efd169e947dd4cbad73f"), genesisHash);

        checkpoints.put(40987, new Sha256Hash("3a80731d3c6c278b3e4c6dd853a108ef9cd2486fa8141f23fa9bdabe3aa8e33c"));
        checkpoints.put(80302, new Sha256Hash("675a4c5b8cf878f203d3a4866d595cb2421859b2bddda4f2d4ba643446e4be41"));
        checkpoints.put(120320, new Sha256Hash("d6ed61ffe1ba2b63fbbe54b8aab43763467b406fa9d0374ede4d8397d880ffaf"));
        checkpoints.put(160000, new Sha256Hash("da14f6ce38bddb33af0f2f448a8d3f7b99b16157a06db7a3706b8d37bb9d2b42"));
        checkpoints.put(200000, new Sha256Hash("1fa6966628f46a08cbfd1cb0422bdaa67003c8f0b8e141a8e00f7fc1e9354d59"));

        dnsSeeds = new String[] {
            "nuseed.coinerella.com",
        };
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }

    @Override
    public String toString() {
        return "NuBits";
    }

}
