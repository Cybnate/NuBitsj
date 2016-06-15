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

import com.matthewmitchell.nubitsj.core.*;
import com.matthewmitchell.nubitsj.utils.MonetaryFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parameters for Nubits-like networks.
 */
public abstract class AbstractNubitsNetParams extends NetworkParameters {

    /**
     * Scheme part for Nubits URIs.
     */
    public static final String NUBITS_SCHEME = "Nu";

    private static final Logger log = LoggerFactory.getLogger(AbstractNubitsNetParams.class);

    public AbstractNubitsNetParams() {
        super();
    }

    @Override
    public String toString() {
        return "NuBits";
    }

    @Override
    public Coin getMaxMoney() {
        return MAX_MONEY;
    }

    @Override
    public Coin getMinOutputValue() {
        return Transaction.MIN_OUTPUT_VALUE;
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        return new MonetaryFormat();
    }

    @Override
    public String getUriScheme() {
        return NUBITS_SCHEME;
    }

    @Override
    public boolean hasMaxMoney() {
        return true;
    }

}
