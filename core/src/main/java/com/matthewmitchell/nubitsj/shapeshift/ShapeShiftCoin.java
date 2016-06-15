/*
 * Copyright (C) 2015-2016 NuBits Developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.matthewmitchell.nubitsj.shapeshift;

import com.matthewmitchell.nubitsj.core.CoinDetails;
import com.matthewmitchell.nubitsj.core.Monetary;
import com.matthewmitchell.nubitsj.core.NetworkParameters;
import com.matthewmitchell.nubitsj.utils.MonetaryFormat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;

/**
 * The {@link com.matthewmitchell.nubitsj.core.NetworkParameters} for a ShapeShift coin
 *
 * @author Matthew Mitchell
 */
public class ShapeShiftCoin extends CoinDetails {
	
    private static MonetaryFormat FORMAT8 = new MonetaryFormat().shift(0).minDecimals(2).repeatOptionalDecimals(1, 6).noCode();
    private static MonetaryFormat FORMAT6 = new MonetaryFormat().shift(0).minDecimals(2).repeatOptionalDecimals(1, 4).noCode();
    private static MonetaryFormat FORMAT5 = new MonetaryFormat().shift(0).minDecimals(2).repeatOptionalDecimals(1, 3).noCode();

    private final String name;
    private final String coinCode;
    private final int exponent;

    static public int ADDRESS_PREFIX_NULL = -1;
	
    /**
     * Construct a ShapeShiftCoin instance with given parameters.
     *
     * @param name The human readable coin name
     * @param uriPrefix The prefix before ":" on coin URIs
     * @param coinCode The code used to identify the coin, in capitals and typically 3-5 characters
     * @param exponent The number of decimals after the decimal place.
     * @param addressHeader The integer of the address prefix for pubKeyHash addresses
     * @param p2shHeader The integer of the address prefix for P2SH addresses
     */
    public ShapeShiftCoin(String name, String uriPrefix, String coinCode, int exponent, int addressHeader, int p2shHeader) {
        this.name = name;
        this.id = uriPrefix;
        this.coinCode = coinCode;
        this.exponent = exponent;
        this.addressHeader = addressHeader;
        this.p2shHeader = p2shHeader;
        this.acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
	    in.defaultReadObject();
    }

    @Override
    public ShapeShiftMonetary parseCoin(String str) {
	    return new ShapeShiftMonetary(new BigDecimal(str).movePointRight(exponent).toBigIntegerExact().longValue(), exponent);
    }
	
    /**
     * Gets a {@link com.matthewmitchell.nubitsj.shapeshift.ShapeShiftMonetary} by rounding amounts
     */
    public ShapeShiftMonetary parseCoinInexact(String str) {
	    return new ShapeShiftMonetary(new BigDecimal(str).movePointRight(exponent).setScale(0, BigDecimal.ROUND_HALF_UP).toBigInteger().longValue(), exponent);
    }
     
    public String getCoinCode() {
        return coinCode;
    }
    
    public int getExponent() {
        return exponent;
    }
    
    @Override
    public boolean isShapeShift() {
	    return true;
    }

    @Override
    public String toString() {
	    return name;
    }

    @Override
    public String getUriScheme() {
        return name;
    }

    @Override
    public MonetaryFormat getMonetaryFormat() {
        
         if (exponent == 8) 
		    return FORMAT8;
        
	    if (exponent == 6) 
		    return FORMAT6;
        
	    return FORMAT5;
        
    }
	
}
