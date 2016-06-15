/*
 * Copyright 2016 NuBits Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.matthewmitchell.nubitsj.core;

import com.matthewmitchell.nubitsj.utils.MonetaryFormat;

/**
 *
 * @author Matthew Mitchell
 */
public abstract class CoinDetails {
    
    protected int addressHeader;
    protected int p2shHeader;
    protected int[] acceptableAddressCodes;
    
    /**
     * See getId(). This may be null for old deserialized wallets. In that case we derive it heuristically
     * by looking at the port number.
     */
    protected String id;
    
    /**
     * Returns true if the CoinDetails is for a ShapeShift coin. ie. not NuBits
     */
    public abstract boolean isShapeShift();
    
    public abstract String toString();
    
    /**
     * String acting as unique ID for these parameters
     */
    public String getId() {
        return id;
    }
    
    /**
     * Used to parse a coin string into a Monetary for this network.
     */
    public abstract Monetary parseCoin(String str);
    
    /**
     * The monetary object for this currency.
     */
    public abstract MonetaryFormat getMonetaryFormat();
    
    /**
     * Scheme part for URIs, for example "nubits".
     */
    public abstract String getUriScheme();
    
    /**
     * First byte of a base58 encoded address. See {@link com.matthewmitchell.nubitsj.core.Address}. This is the same as acceptableAddressCodes[0] and
     * is the one used for "normal" addresses. Other types of address may be encountered with version codes found in
     * the acceptableAddressCodes array.
     */
    public int getAddressHeader() {
        return addressHeader;
    }

    /**
     * First byte of a base58 encoded P2SH address.  P2SH addresses are defined as part of BIP0013.
     */
    public int getP2SHHeader() {
        return p2shHeader;
    }
    
    /**
     * The version codes that prefix addresses which are acceptable on this network. Although Satoshi intended these to
     * be used for "versioning", in fact they are today used to discriminate what kind of data is contained in the
     * address and to prevent accidentally sending coins across chains which would destroy them.
     */
    public int[] getAcceptableAddressCodes() {
        return acceptableAddressCodes;
    }
    
}
