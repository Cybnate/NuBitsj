package com.matthewmitchell.nubitsj.core;

import com.google.common.collect.ImmutableList;
import com.matthewmitchell.nubitsj.params.MainNetParams;
import com.matthewmitchell.nubitsj.script.Script;
import com.matthewmitchell.nubitsj.script.ScriptBuilder;
import com.matthewmitchell.nubitsj.script.ScriptOpCodes;
import com.matthewmitchell.nubitsj.testing.TestWithWallet;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransactionOutputTest extends TestWithWallet {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testMultiSigOutputToString() throws Exception {
        sendMoneyToWallet(Coin.COIN.add(Coin.CENT), AbstractBlockChain.NewBlockType.BEST_CHAIN);
        ECKey myKey = new ECKey();
        this.wallet.importKey(myKey);

        // Simulate another signatory
        ECKey otherKey = new ECKey();

        // Create multi-sig transaction
        Transaction multiSigTransaction = new Transaction(params);
        ImmutableList<ECKey> keys = ImmutableList.of(myKey, otherKey);

        Script scriptPubKey = ScriptBuilder.createMultiSigOutputScript(2, keys);
        multiSigTransaction.addOutput(Coin.COIN, scriptPubKey);

        Wallet.SendRequest req = Wallet.SendRequest.forTx(multiSigTransaction);
        this.wallet.completeTx(req);
        TransactionOutput multiSigTransactionOutput = multiSigTransaction.getOutput(0);

        assertThat(multiSigTransactionOutput.toString(), CoreMatchers.containsString("CHECKMULTISIG"));
    }

    @Test
    public void testP2SHOutputScript() throws Exception {
        String P2SHAddressString = "BXgpcAEKv7XedBVtqhr26WHipykZfdYZeT";
        Address P2SHAddress = new Address(MainNetParams.get(), P2SHAddressString);
        Script script = ScriptBuilder.createOutputScript(P2SHAddress);
        Transaction tx = new Transaction(MainNetParams.get());
        tx.addOutput(Coin.COIN, script);
        assertEquals(P2SHAddressString, tx.getOutput(0).getAddressFromP2SH(MainNetParams.get()).toString());
    }

    @Test
    public void getAddressTests() throws Exception {
        Transaction tx = new Transaction(MainNetParams.get());
        Script script = new ScriptBuilder().op(ScriptOpCodes.OP_RETURN).data("hello world!".getBytes()).build();
        tx.addOutput(Coin.CENT, script);
        assertNull(tx.getOutput(0).getAddressFromP2SH(params));
        assertNull(tx.getOutput(0).getAddressFromP2PKHScript(params));
    }
}
