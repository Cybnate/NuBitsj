package com.matthewmitchell.nubitsj.store;

import com.matthewmitchell.nubitsj.core.*;
import com.matthewmitchell.nubitsj.params.*;
import org.junit.*;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class LevelDBBlockStoreTest {
    @Test
    public void basics() throws Exception {
        File f = File.createTempFile("leveldbblockstore", null);
        f.delete();

        NetworkParameters params = UnitTestParams.get();
        Context context = new Context(params);
        LevelDBBlockStore store = new LevelDBBlockStore(context, f);
        store.reset();

        // Check the first block in a new store is the genesis block.
        StoredBlock genesis = store.getChainHead();
        assertEquals(params.getGenesisBlock(), genesis.getHeader());
        assertEquals(0, genesis.getHeight());

        // Build a new block.
        Address to = new Address(params, "BFf9dmGBgHB3rLTKo6Lt2HNYtRNKAXo6zE");
        StoredBlock b1 = genesis.build(genesis.getHeader().createNextBlock(to).cloneAsHeader());
        store.put(b1);
        store.setChainHead(b1);
        store.close();

        // Check we can get it back out again if we rebuild the store object.
        store = new LevelDBBlockStore(context, f);
        try {
            StoredBlock b2 = store.get(b1.getHeader().getHash());
            assertEquals(b1, b2);
            // Check the chain head was stored correctly also.
            StoredBlock chainHead = store.getChainHead();
            assertEquals(b1, chainHead);
        } finally {
            store.close();
            store.destroy();
        }
    }
}