/*
 * Copyright 2015 NuBits Develoeprs
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

import com.matthewmitchell.nubitsj.core.Address;
import com.matthewmitchell.nubitsj.core.AddressFormatException;
import com.matthewmitchell.nubitsj.core.Coin;
import com.matthewmitchell.nubitsj.params.MainNetParams;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link com.matthewmitchell.nubitsj.shapeshift.ShapeShiftInterface}
 *
 * @author Matthew Mitchell
 */
public class ShapeShiftComm extends ShapeShiftInterface {
	
	private static final AsyncHttpClient client = new AsyncHttpClient();
	private static final String API_URL = "https://shapeshift.io/";
	private static final String CONTENT_TYPE = "application/json";
	
	private abstract class ResponseCallback implements AsyncHttpClient.HttpResponseCallbacks {
		
		@Override
		public void onFailure(int responseCode, String error) {
			cbks.networkError(responseCode, String.format("%d %s", responseCode, error));
		}
		
	}
	
	private boolean checkError(JSONObject json) throws JSONException {
		
		if (!json.has("error"))
			return false;
		
		cbks.networkError(AsyncHttpClient.API_ERROR, json.getString("error"));
		return true;
			
	}
	
	@Override
	public void sendAmount(final ShapeShiftCoin destCoin, Address destAddr, ShapeShiftMonetary amount, Address refund) {
		
		JSONObject jsonRequest = new JSONObject();
		
		try {
			jsonRequest.put("pair", "nbt_" + destCoin.coinCode);
			jsonRequest.put("amount", amount.toPlainString());
			jsonRequest.put("withdrawal", destAddr.toString());
			jsonRequest.put("returnAddress", refund.toString());
		} catch (JSONException ex) {
			throw new IllegalArgumentException();
		}
		
		client.post(API_URL + "sendamount", jsonRequest.toString(), CONTENT_TYPE, new ResponseCallback() {

			@Override
			public void onSuccess(String response) {
				try {
					
					JSONObject jsonResult = new JSONObject(response);
					
					if (checkError(jsonResult))
						return;
					
					jsonResult = jsonResult.getJSONObject("success");
					Address deposit = new Address(MainNetParams.get(), jsonResult.getString("deposit"));
					Coin amount = Coin.parseCoin(jsonResult.getString("depositAmount"));
					ShapeShiftMonetary foreignRate = destCoin.format.parseShapeShiftCoin(jsonResult.getString("quotedRate"), destCoin.exponent);
					Coin rate = foreignRate.toCoinRate();
					long expiry = jsonResult.getLong("expiration");
					
					cbks.sendAmountResponse(deposit, amount, expiry, rate);
					
				} catch (JSONException ex) {
					cbks.networkError(AsyncHttpClient.PARSE_ERROR, "");
				} catch (AddressFormatException ex) {
					cbks.networkError(AsyncHttpClient.PARSE_ERROR, "");
				}catch (ArithmeticException e) {
					cbks.networkError(AsyncHttpClient.PARSE_ERROR, "");
				}
			}
			
		});
		
	}
	
	@Override
	public void cancelPending(Address depositAddr) {
		
		JSONObject jsonRequest = new JSONObject();
		
		try {
			jsonRequest.put("address", depositAddr.toString());
		} catch (JSONException ex) {
			throw new IllegalArgumentException();
		}
		
		client.post(API_URL + "cancelpending", jsonRequest.toString(), CONTENT_TYPE, new ResponseCallback() {

			@Override
			public void onSuccess(String response) {
				cbks.cancelPendingResponse();
			}
		
		});
		
	}

	@Override
	public void shift(ShapeShiftCoin destCoin, Address destAddr, Address refund) {
		
		JSONObject jsonRequest = new JSONObject();
		
		try {
			jsonRequest.put("pair", "nbt_" + destCoin.coinCode);
			jsonRequest.put("withdrawal", destAddr.toString());
			jsonRequest.put("returnAddress", refund.toString());
		} catch (JSONException ex) {
			throw new IllegalArgumentException();
		}
		
		client.post(API_URL + "shift", jsonRequest.toString(), CONTENT_TYPE, new ResponseCallback() {

			@Override
			public void onSuccess(String response) {
				try {
					
					JSONObject jsonResult = new JSONObject(response);

					if (checkError(jsonResult))
						return;
					
					Address deposit = new Address(MainNetParams.get(), jsonResult.getString("deposit"));
					cbks.shiftResponse(deposit);
				
				} catch (JSONException ex) {
					cbks.networkError(AsyncHttpClient.PARSE_ERROR, "");
				} catch (AddressFormatException ex) {
					cbks.networkError(AsyncHttpClient.PARSE_ERROR, "");
				}
				
			}
			
		});
		
	}

	@Override
	public void limit(ShapeShiftCoin destCoin) {
		client.get(API_URL + "limit/nbt_" + destCoin.coinCode, new ResponseCallback() {

			@Override
			public void onSuccess(String response) {
				try {
					
					JSONObject jsonResult = new JSONObject(response);
					
					if (checkError(jsonResult))
						return;
					
					Coin max = Coin.parseCoin(jsonResult.getString("limit"));
					Coin min = Coin.parseCoin(jsonResult.getString("min"));
							
					cbks.limitResponse(max, min);
					
				} catch (JSONException ex) {
					cbks.networkError(AsyncHttpClient.PARSE_ERROR, "");
				} catch (ArithmeticException e) {
					cbks.networkError(AsyncHttpClient.PARSE_ERROR, "");
				}
			}
		});
	}

	@Override
	public void rate(ShapeShiftCoin destCoin) {
		client.get(API_URL + "rate/" + destCoin.coinCode + "_nbt", new ResponseCallback() {

			@Override
			public void onSuccess(String response) {
				try {
					
					JSONObject jsonResult = new JSONObject(response);
					
					if (checkError(jsonResult))
						return;
					
					Coin rate = Coin.parseCoinInexact(jsonResult.getString("rate"));
					cbks.rateResponse(rate);
					
				} catch (JSONException ex) {
					cbks.networkError(AsyncHttpClient.PARSE_ERROR, "");
				}
			}
			
		});
	}
	
	@Override
	public void marketInfo(final ShapeShiftCoin destCoin) {
		client.get(API_URL + "marketinfo/nbt_" + destCoin.coinCode, new ResponseCallback() {

			@Override
			public void onSuccess(String response) {
				try {
					
					JSONObject jsonResult = new JSONObject(response);
					
					if (checkError(jsonResult))
						return;
					
					Coin min = Coin.parseCoinInexact(jsonResult.getString("minimum"));
					Coin max = Coin.parseCoinInexact(jsonResult.getString("limit"));
					ShapeShiftMonetary rate = destCoin.parseCoinInexact(jsonResult.getString("rate"));
					ShapeShiftMonetary fee = destCoin.parseCoinInexact(jsonResult.getString("minerFee"));
					
					cbks.marketInfoResponse(rate, fee, max, min);
					
				} catch (JSONException ex) {
					cbks.networkError(AsyncHttpClient.PARSE_ERROR, "");
				}
			}
			
		});
	}
	
}
