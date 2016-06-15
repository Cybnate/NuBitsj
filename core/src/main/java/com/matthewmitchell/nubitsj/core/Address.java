/**
 * Copyright 2011 Google Inc.
 * Copyright 2014 Giannis Dzegoutanis
 * Copyright 2015 Andreas Schildbach
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import com.matthewmitchell.nubitsj.params.Networks;
import com.matthewmitchell.nubitsj.script.Script;
import javax.annotation.Nullable;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>A Nubits address looks like 1MsScoe2fTJoq4ZPdQgqyhgWeoNamYPevy and is derived from an elliptic curve public key
 * plus a set of network parameters. Not to be confused with a {@link PeerAddress} or {@link AddressMessage}
 * which are about network (TCP) addresses.</p>
 *
 * <p>A standard address is built by taking the RIPE-MD160 hash of the public key bytes, with a version prefix and a
 * checksum suffix, then encoding it textually as base58. The version prefix is used to both denote the network for
 * which the address is valid (see {@link NetworkParameters}, and also to indicate how the bytes inside the address
 * should be interpreted. Whilst almost all addresses today are hashes of public keys, another (currently unsupported
 * type) can contain a hash of a script instead.</p>
 */
public class Address extends VersionedChecksummedBytes {
    /**
     * An address is a RIPEMD160 hash of a public key, therefore is always 160 bits or 20 bytes.
     */
    public static final int LENGTH = 20;

    private transient List<CoinDetails> coinDetails;

    /**
     * Construct an address from a list of parameters, the address version, and the hash160 form. Example:<p>
     *
     * <pre>new Address(Arrays.asList(NetworkParameters.prodNet()), NetworkParameters.getAddressHeader(), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     */
    public Address(List<CoinDetails> coinList, int version, byte[] hash160) throws WrongNetworkException {

        super(version, hash160);
        checkNotNull(coinList);
        checkArgument(hash160.length == 20, "Addresses are 160-bit hashes, so you must provide 20 bytes");

        for (CoinDetails coin: coinList) {
            if (!isAcceptableVersion(coin, version))
                throw new WrongNetworkException(version, coin.getAcceptableAddressCodes());
        }

        this.coinDetails = coinList;

    }

    /**
     * Construct an address from parameters, the address version, and the hash160 form. Example:<p>
     *
     * <pre>new Address(NetworkParameters.prodNet(), NetworkParameters.getAddressHeader(), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     */
    public Address(CoinDetails coinDetails, int version, byte[] hash160) throws WrongNetworkException {
        this(Arrays.asList(coinDetails), version, hash160);	
    }

    /** Returns an Address that represents the given P2SH script hash. */
    public static Address fromP2SHHash(NetworkParameters params, byte[] hash160) {
        try {
            return new Address(params, params.getP2SHHeader(), hash160);
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /** Returns an Address that represents the script hash extracted from the given scriptPubKey */
    public static Address fromP2SHScript(NetworkParameters params, Script scriptPubKey) {
        checkArgument(scriptPubKey.isPayToScriptHash(), "Not a P2SH script");
        return fromP2SHHash(params, scriptPubKey.getPubKeyHash());
    }

    /**
     * Construct an address from parameters and the hash160 form. Example:<p>
     *
     * <pre>new Address(Arrays.asList(NetworkParameters.prodNet()), Hex.decode("4a22c3c4cbb31e4d03b15550636762bda0baf85a"));</pre>
     */
    public Address(List<CoinDetails> coinList, byte[] hash160) {
        super(coinList.get(0).getAddressHeader(), hash160);
        checkArgument(hash160.length == 20, "Addresses are 160-bit hashes, so you must provide 20 bytes");
        this.coinDetails = coinList;
    }

    public Address(CoinDetails coinDetails, byte[] hash160) {
        this(Arrays.asList(coinDetails), hash160);
    }

    /**
     * Construct an address from a list of parameters and the standard "human readable" form. Example:<p>
     *
     * <pre>new Address(Arrays.asList(NetworkParameters.prodNet()), "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");</pre><p>
     *
     * @param paramsList The expected NetworkParameters or null if you don't want validation.
     * @param address The textual form of the address, such as "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL"
     * @throws AddressFormatException if the given address doesn't parse or the checksum is invalid
     * @throws WrongNetworkException if the given address is valid but for a different chain (eg testnet vs prodnet)
     */
    public Address(@Nullable List<CoinDetails> coinList, String address) throws AddressFormatException {
        
        super(address);
        
        if (coinList != null) {

            for (CoinDetails coin: coinList) {
                if (!isAcceptableVersion(coin, version)) {
                    throw new WrongNetworkException(version, coin.getAcceptableAddressCodes());
                }
            }

            this.coinDetails = coinList;

        } else {

            ArrayList<CoinDetails> coinsFound = new ArrayList<CoinDetails>();

            for (CoinDetails p : Networks.get())
                if (isAcceptableVersion(p, version))
                    coinsFound.add(p);

            if (coinsFound.isEmpty())
                throw new AddressFormatException("No network found for " + address);

            this.coinDetails = coinsFound;

        }
    }

    /**
     * Construct an address from a standard "human readable" form. Example:<p>
     *
     * <pre>new Address("17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");</pre><p>
     *
     * @param address The textual form of the address, such as "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL"
     * @throws AddressFormatException if the given address doesn't parse or the checksum is invalid
     * @throws WrongNetworkException if the given address is valid but for a different chain (eg testnet vs prodnet)
     */
    public Address(String address) throws AddressFormatException {
        this((List<CoinDetails>) null, address);
    }

    /**
     * Construct an address from parameters and the standard "human readable" form. Example:<p>
     *
     * <pre>new Address(NetworkParameters.prodNet(), "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL");</pre><p>
     *
     * @param params The expected NetworkParameters or null if you don't want validation.
     * @param address The textual form of the address, such as "17kzeh4N8g49GFvdDzSf8PjaPfyoD1MndL"
     * @throws AddressFormatException if the given address doesn't parse or the checksum is invalid
     * @throws WrongNetworkException if the given address is valid but for a different chain (eg testnet vs prodnet)
     */
    public Address(@Nullable CoinDetails coinDetails, String address) throws AddressFormatException {
        this(coinDetails == null ? null : Arrays.asList(coinDetails), address);
    }

    /** The (big endian) 20 byte hash that is the core of a Nubits address. */
    public byte[] getHash160() {
        return bytes;
    }

    /*
     * Returns true if this address is a Pay-To-Script-Hash (P2SH) address for the given network.
     * See also https://github.com.matthewmitchell/bips/blob/master/bip-0013.mediawiki: Address Format for pay-to-script-hash
     */
    public boolean isP2SHAddress(CoinDetails params) {
        return params != null && this.version == params.p2shHeader;
    }

    public boolean isSelectedP2SHAddress() {
        return isP2SHAddress(coinDetails.get(0));
    }

    /**
     * Returns a list of CoinDetails. If you aren't sure which coin the address is intended for (eg, it was provided by
     * a user), you can use this to decide if it is compatible with the current wallet. You should be able to handle a 
     * null response from this method. 
     *
     * @return a list of CoinDetails representing the coins the address is intended for, or null if unknown.
     */
    public List<CoinDetails> getCoinDetails() {
        return coinDetails;
    }

    /**
     * Examines the version byte of the address and attempts to find the matching CoinDetails. This is similar to
     * getCoinDetails() but returns the first NetworkParameters in the list.
     *
     * @return a CoinDetails representing the coin the address is intended for, or null if unknown.
     */
    public CoinDetails getSelectedCoinDetails() {

        if (coinDetails == null)
            return null;

        return coinDetails.get(0);

    }

    /**
     * Given an address, examines the version byte and attempts to find matching CoinDetails. If you aren't sure
     * which coins the address is intended for (eg, it was provided by a user), you can use this to decide if it is
     * compatible with the current wallet.
     * @return A list of CoinDetails or null if the string wasn't of a known version.
     */
    @Nullable
    public static List<CoinDetails> getCoinsFromAddress(String address) throws AddressFormatException {
        try {
            return new Address(address).getCoinDetails();
        } catch (WrongNetworkException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    /**
     * Check if a given address version is valid given the CoinDetails.
     */
    private static boolean isAcceptableVersion(CoinDetails coinDetails, int version) {
        for (int v : coinDetails.getAcceptableAddressCodes()) {
            if (version == v) {
                return true;
            }
        }
        return false;
    }

    /**
     * This implementation narrows the return type to <code>Address</code>.
     */
    @Override
    public Address clone() throws CloneNotSupportedException {
        return (Address) super.clone();
    }

    // Java serialization

    private void writeObject(ObjectOutputStream out) throws IOException {

        out.defaultWriteObject();
        out.writeInt(coinDetails.size());
        for (CoinDetails paramsX: coinDetails)
            out.writeUTF(paramsX.id);

    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();
        
        int paramNum = in.readInt();
        
        coinDetails = new ArrayList(paramNum);
        
        for (int x = 0; x < paramNum; x++)
            coinDetails.add(
                Networks.get(in.readUTF())
            );

    }
}
