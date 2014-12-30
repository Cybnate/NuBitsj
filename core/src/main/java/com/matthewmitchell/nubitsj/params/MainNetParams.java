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
        proofOfWorkLimit = Utils.decodeCompactBits(0x1e0fffffL);
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
        subsidyDecreaseBlockCount = 210000;
        spendableCoinbaseDepth = 500;
        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("000003cc2da5a0a289ad0a590c20a8b975219ddc1204efd169e947dd4cbad73f"), genesisHash);

        checkpoints.put(40987, new Sha256Hash("3a80731d3c6c278b3e4c6dd853a108ef9cd2486fa8141f23fa9bdabe3aa8e33c"));
        checkpoints.put(80302, new Sha256Hash("675a4c5b8cf878f203d3a4866d595cb2421859b2bddda4f2d4ba643446e4be41"));
        checkpoints.put(120320, new Sha256Hash("d6ed61ffe1ba2b63fbbe54b8aab43763467b406fa9d0374ede4d8397d880ffaf"));
    }

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }
}
