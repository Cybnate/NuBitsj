/*
 * Copyright 2014 Giannis Dzegoutanis
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

import com.google.common.collect.Lists;
import com.matthewmitchell.nubitsj.core.CoinDetails;
import com.matthewmitchell.nubitsj.core.NetworkParameters;
import java.util.Collection;
import java.util.List;

/**
 * Utility class that holds all the registered CoinDetails types used for Address auto discovery.
 * By default only MainNetParams is used. 
 */
public class Networks {
    /** Registered networks */
    private static final List<CoinDetails> networks = Lists.newArrayList((CoinDetails) MainNetParams.get());

    public static List<CoinDetails> get() {
        return networks;
    }

    public static CoinDetails get(String id) {

        for (CoinDetails network: networks) {
            if (network.getId().equals(id))
                return network;
        }
        return null;

    }

    public static void register(CoinDetails network) {
        register(Lists.newArrayList(network));
    }

    public static void register(Collection<? extends CoinDetails> networks) {
        Networks.networks.addAll(networks);
    }

    public static void unregister(CoinDetails network) {
        networks.remove(network);
    }

}

