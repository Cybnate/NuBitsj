package com.matthewmitchell.nubitsj.wallet;

import com.matthewmitchell.nubitsj.core.ECKey;

import java.util.List;

public class AbstractKeyChainEventListener implements KeyChainEventListener {
    @Override
    public void onKeysAdded(List<ECKey> keys) {
    }
}

