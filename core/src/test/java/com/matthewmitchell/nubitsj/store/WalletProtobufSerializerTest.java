/**
 * Copyright 2012 Google Inc.
 * Copyright 2014 Andreas Schildbach
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

package com.matthewmitchell.nubitsj.store;


import com.matthewmitchell.nubitsj.core.*;
import com.matthewmitchell.nubitsj.core.Transaction.Purpose;
import com.matthewmitchell.nubitsj.core.TransactionConfidence.ConfidenceType;
import com.matthewmitchell.nubitsj.crypto.DeterministicKey;
import com.matthewmitchell.nubitsj.params.MainNetParams;
import com.matthewmitchell.nubitsj.params.UnitTestParams;
import com.matthewmitchell.nubitsj.script.ScriptBuilder;
import com.matthewmitchell.nubitsj.testing.FakeTxBuilder;
import com.matthewmitchell.nubitsj.testing.FooWalletExtension;
import com.matthewmitchell.nubitsj.utils.BriefLogFormatter;
import com.matthewmitchell.nubitsj.utils.Threading;
import com.matthewmitchell.nubitsj.wallet.DeterministicKeyChain;
import com.matthewmitchell.nubitsj.wallet.KeyChain;
import com.google.protobuf.ByteString;
import com.matthewmitchell.nubitsj.wallet.MarriedKeyChain;
import com.matthewmitchell.nubitsj.wallet.Protos;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import static com.matthewmitchell.nubitsj.core.Coin.*;
import static com.matthewmitchell.nubitsj.testing.FakeTxBuilder.createFakeTx;
import static org.junit.Assert.*;
import static com.google.common.base.Preconditions.checkNotNull;

public class WalletProtobufSerializerTest {
    static final NetworkParameters params = UnitTestParams.get();
    private ECKey myKey;
    private ECKey myWatchedKey;
    private Address myAddress;
    private Wallet myWallet;

    public static String WALLET_DESCRIPTION  = "The quick brown fox lives in \u4f26\u6566"; // Beijing in Chinese
    private long mScriptCreationTime;

    @Before
    public void setUp() throws Exception {
        BriefLogFormatter.initVerbose();
        Context ctx = new Context(params);
        myWatchedKey = new ECKey();
        myWallet = new Wallet(params, null);
        myKey = new ECKey();
        myKey.setCreationTimeSeconds(123456789L);
        myWallet.importKey(myKey);
        myAddress = myKey.toAddress(params);
        myWallet = new Wallet(params, null);
        myWallet.importKey(myKey);
        mScriptCreationTime = new Date().getTime() / 1000 - 1234;
        myWallet.addWatchedAddress(myWatchedKey.toAddress(params), mScriptCreationTime);
        myWallet.setDescription(WALLET_DESCRIPTION);
    }

    @Test
    public void empty() throws Exception {
        // Check the base case of a wallet with one key and no transactions.
        Wallet wallet1 = roundTrip(myWallet);
        assertEquals(0, wallet1.getTransactions(true).size());
        assertEquals(Coin.ZERO, wallet1.getBalance());
        assertArrayEquals(myKey.getPubKey(),
                wallet1.findKeyFromPubHash(myKey.getPubKeyHash()).getPubKey());
        assertArrayEquals(myKey.getPrivKeyBytes(),
                wallet1.findKeyFromPubHash(myKey.getPubKeyHash()).getPrivKeyBytes());
        assertEquals(myKey.getCreationTimeSeconds(),
                wallet1.findKeyFromPubHash(myKey.getPubKeyHash()).getCreationTimeSeconds());
        assertEquals(mScriptCreationTime,
                wallet1.getWatchedScripts().get(0).getCreationTimeSeconds());
        assertEquals(1, wallet1.getWatchedScripts().size());
        assertEquals(ScriptBuilder.createOutputScript(myWatchedKey.toAddress(params)),
                wallet1.getWatchedScripts().get(0));
        assertEquals(WALLET_DESCRIPTION, wallet1.getDescription());
    }

    @Test
    public void oneTx() throws Exception {
        // Check basic tx serialization.
        Coin v1 = COIN;
        Transaction t1 = createFakeTx(params, v1, myAddress);
        t1.getConfidence().markBroadcastBy(new PeerAddress(InetAddress.getByName("1.2.3.4")));
        t1.getConfidence().markBroadcastBy(new PeerAddress(InetAddress.getByName("5.6.7.8")));
        t1.getConfidence().setSource(TransactionConfidence.Source.NETWORK);
        myWallet.receivePending(t1, null);
        Wallet wallet1 = roundTrip(myWallet);
        assertEquals(1, wallet1.getTransactions(true).size());
        assertEquals(v1, wallet1.getBalance(Wallet.BalanceType.ESTIMATED));
        Transaction t1copy = wallet1.getTransaction(t1.getHash());
        assertArrayEquals(t1.nubitsSerialize(), t1copy.nubitsSerialize());
        assertEquals(2, t1copy.getConfidence().numBroadcastPeers());
        assertEquals(TransactionConfidence.Source.NETWORK, t1copy.getConfidence().getSource());
        
        Protos.Wallet walletProto = new WalletProtobufSerializer().walletToProto(myWallet);
        assertEquals(Protos.Key.Type.ORIGINAL, walletProto.getKey(0).getType());
        assertEquals(0, walletProto.getExtensionCount());
        assertEquals(1, walletProto.getTransactionCount());
        assertEquals(6, walletProto.getKeyCount());
        
        Protos.Transaction t1p = walletProto.getTransaction(0);
        assertEquals(0, t1p.getBlockHashCount());
        assertArrayEquals(t1.getHash().getBytes(), t1p.getHash().toByteArray());
        assertEquals(Protos.Transaction.Pool.PENDING, t1p.getPool());
        assertFalse(t1p.hasLockTime());
        assertFalse(t1p.getTransactionInput(0).hasSequence());
        assertArrayEquals(t1.getInputs().get(0).getOutpoint().getHash().getBytes(),
                t1p.getTransactionInput(0).getTransactionOutPointHash().toByteArray());
        assertEquals(0, t1p.getTransactionInput(0).getTransactionOutPointIndex());
        assertEquals(t1p.getTransactionOutput(0).getValue(), v1.value);
    }

    @Test
    public void doubleSpend() throws Exception {
        // Check that we can serialize double spends correctly, as this is a slightly tricky case.
        FakeTxBuilder.DoubleSpends doubleSpends = FakeTxBuilder.createFakeDoubleSpendTxns(params, myAddress);
        // t1 spends to our wallet.
        myWallet.receivePending(doubleSpends.t1, null);
        // t2 rolls back t1 and spends somewhere else.
        myWallet.receiveFromBlock(doubleSpends.t2, null, BlockChain.NewBlockType.BEST_CHAIN, 0);
        Wallet wallet1 = roundTrip(myWallet);
        assertEquals(1, wallet1.getTransactions(true).size());
        Transaction t1 = wallet1.getTransaction(doubleSpends.t1.getHash());
        assertEquals(ConfidenceType.DEAD, t1.getConfidence().getConfidenceType());
        assertEquals(Coin.ZERO, wallet1.getBalance());

        // TODO: Wallet should store overriding transactions even if they are not wallet-relevant.
        // assertEquals(doubleSpends.t2, t1.getConfidence().getOverridingTransaction());
    }
    
    @Test
    public void testKeys() throws Exception {
        for (int i = 0 ; i < 20 ; i++) {
            myKey = new ECKey();
            myAddress = myKey.toAddress(params);
            myWallet = new Wallet(params, null);
            myWallet.importKey(myKey);
            Wallet wallet1 = roundTrip(myWallet);
            assertArrayEquals(myKey.getPubKey(), wallet1.findKeyFromPubHash(myKey.getPubKeyHash()).getPubKey());
            assertArrayEquals(myKey.getPrivKeyBytes(), wallet1.findKeyFromPubHash(myKey.getPubKeyHash()).getPrivKeyBytes());
        }
    }

    @Test
    public void testLastBlockSeenHash() throws Exception {
        // Test the lastBlockSeenHash field works.

        // LastBlockSeenHash should be empty if never set.
        Wallet wallet = new Wallet(params, null);
        Protos.Wallet walletProto = new WalletProtobufSerializer().walletToProto(wallet);
        ByteString lastSeenBlockHash = walletProto.getLastSeenBlockHash();
        assertTrue(lastSeenBlockHash.isEmpty());

        // Create a block.
        Block block = new Block(params, BlockTest.blockBytes);
        Sha256Hash blockHash = block.getHash();
        wallet.setLastBlockSeenHash(blockHash);
        wallet.setLastBlockSeenHeight(1);

        // Roundtrip the wallet and check it has stored the blockHash.
        Wallet wallet1 = roundTrip(wallet);
        assertEquals(blockHash, wallet1.getLastBlockSeenHash());
        assertEquals(1, wallet1.getLastBlockSeenHeight());

        // Test the Satoshi genesis block (hash of all zeroes) is roundtripped ok.
        Block genesisBlock = MainNetParams.get().getGenesisBlock();
        wallet.setLastBlockSeenHash(genesisBlock.getHash());
        Wallet wallet2 = roundTrip(wallet);
        assertEquals(genesisBlock.getHash(), wallet2.getLastBlockSeenHash());
    }

    @Test
    public void testSequenceNumber() throws Exception {
        Wallet wallet = new Wallet(params, null);
        Transaction tx1 = createFakeTx(params, Coin.COIN, wallet.currentReceiveAddress());
        tx1.getInput(0).setSequenceNumber(TransactionInput.NO_SEQUENCE);
        wallet.receivePending(tx1, null);
        Transaction tx2 = createFakeTx(params, Coin.COIN, wallet.currentReceiveAddress());
        tx2.getInput(0).setSequenceNumber(TransactionInput.NO_SEQUENCE - 1);
        wallet.receivePending(tx2, null);
        Wallet walletCopy = roundTrip(wallet);
        Transaction tx1copy = checkNotNull(walletCopy.getTransaction(tx1.getHash()));
        assertEquals(TransactionInput.NO_SEQUENCE, tx1copy.getInput(0).getSequenceNumber());
        Transaction tx2copy = checkNotNull(walletCopy.getTransaction(tx2.getHash()));
        assertEquals(TransactionInput.NO_SEQUENCE - 1, tx2copy.getInput(0).getSequenceNumber());
    }

    @Test
    public void testAppearedAtChainHeightDepthAndWorkDone() throws Exception {
        // Test the TransactionConfidence appearedAtChainHeight, depth and workDone field are stored.

        BlockChain chain = new BlockChain(params, myWallet, new MemoryBlockStore(params), null);

        final ArrayList<Transaction> txns = new ArrayList<Transaction>(2);
        myWallet.addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                txns.add(tx);
            }
        });

        // Start by building two blocks on top of the genesis block.
        Block b1 = params.getGenesisBlock().createNextBlock(myAddress);
        BigInteger work1 = b1.getWork();
        assertTrue(work1.signum() > 0);

        Block b2 = b1.createNextBlock(myAddress);
        BigInteger work2 = b2.getWork();
        assertTrue(work2.signum() > 0);

        assertTrue(chain.add(b1));
        assertTrue(chain.add(b2));

        // We now have the following chain:
        //     genesis -> b1 -> b2

        // Check the transaction confidence levels are correct before wallet roundtrip.
        Threading.waitForUserCode();
        assertEquals(2, txns.size());

        TransactionConfidence confidence0 = txns.get(0).getConfidence();
        TransactionConfidence confidence1 = txns.get(1).getConfidence();

        assertEquals(1, confidence0.getAppearedAtChainHeight());
        assertEquals(2, confidence1.getAppearedAtChainHeight());

        assertEquals(2, confidence0.getDepthInBlocks());
        assertEquals(1, confidence1.getDepthInBlocks());

        // Roundtrip the wallet and check it has stored the depth and workDone.
        Wallet rebornWallet = roundTrip(myWallet);

        Set<Transaction> rebornTxns = rebornWallet.getTransactions(false);
        assertEquals(2, rebornTxns.size());

        // The transactions are not guaranteed to be in the same order so sort them to be in chain height order if required.
        Iterator<Transaction> it = rebornTxns.iterator();
        Transaction txA = it.next();
        Transaction txB = it.next();

        Transaction rebornTx0, rebornTx1;
         if (txA.getConfidence().getAppearedAtChainHeight() == 1) {
            rebornTx0 = txA;
            rebornTx1 = txB;
        } else {
            rebornTx0 = txB;
            rebornTx1 = txA;
        }

        TransactionConfidence rebornConfidence0 = rebornTx0.getConfidence();
        TransactionConfidence rebornConfidence1 = rebornTx1.getConfidence();

        assertEquals(1, rebornConfidence0.getAppearedAtChainHeight());
        assertEquals(2, rebornConfidence1.getAppearedAtChainHeight());

        assertEquals(2, rebornConfidence0.getDepthInBlocks());
        assertEquals(1, rebornConfidence1.getDepthInBlocks());
    }

    private static Wallet roundTrip(Wallet wallet) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new WalletProtobufSerializer().writeWallet(wallet, output);
        ByteArrayInputStream test = new ByteArrayInputStream(output.toByteArray());
        assertTrue(WalletProtobufSerializer.isWallet(test));
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        return new WalletProtobufSerializer().readWallet(input, null);
    }

    @Test
    public void testRoundTripNormalWallet() throws Exception {
        Wallet wallet1 = roundTrip(myWallet);     
        assertEquals(0, wallet1.getTransactions(true).size());
        assertEquals(Coin.ZERO, wallet1.getBalance());
        assertArrayEquals(myKey.getPubKey(),
                wallet1.findKeyFromPubHash(myKey.getPubKeyHash()).getPubKey());
        assertArrayEquals(myKey.getPrivKeyBytes(),
                wallet1.findKeyFromPubHash(myKey.getPubKeyHash()).getPrivKeyBytes());
        assertEquals(myKey.getCreationTimeSeconds(),
                wallet1.findKeyFromPubHash(myKey.getPubKeyHash()).getCreationTimeSeconds());
    }

    @Test
    public void testRoundTripMarriedWallet() throws Exception {
        // create 2-of-2 married wallet
        myWallet = new Wallet(params, null);
        final DeterministicKeyChain partnerChain = new DeterministicKeyChain(new SecureRandom());
        DeterministicKey partnerKey = DeterministicKey.deserializeB58(null, partnerChain.getWatchingKey().serializePubB58(params), params);
        MarriedKeyChain chain = MarriedKeyChain.builder()
                .random(new SecureRandom())
                .followingKeys(partnerKey)
                .threshold(2).build();
        myWallet.addAndActivateHDChain(chain);

        myAddress = myWallet.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS);

        Wallet wallet1 = roundTrip(myWallet);
        assertEquals(0, wallet1.getTransactions(true).size());
        assertEquals(Coin.ZERO, wallet1.getBalance());
        assertEquals(2, wallet1.getActiveKeychain().getSigsRequiredToSpend());
        assertEquals(myAddress, wallet1.currentAddress(KeyChain.KeyPurpose.RECEIVE_FUNDS));
    }

    @Test
    public void tags() throws Exception {
        myWallet.setTag("foo", ByteString.copyFromUtf8("bar"));
        assertEquals("bar", myWallet.getTag("foo").toStringUtf8());
        myWallet = roundTrip(myWallet);
        assertEquals("bar", myWallet.getTag("foo").toStringUtf8());
    }

    @Test
    public void extensions() throws Exception {
        myWallet.addExtension(new FooWalletExtension("com.whatever.required", true));
        Protos.Wallet proto = new WalletProtobufSerializer().walletToProto(myWallet);
        // Initial extension is mandatory: try to read it back into a wallet that doesn't know about it.
        try {
            new WalletProtobufSerializer().readWallet(params, null, proto, null);
            fail();
        } catch (UnreadableWalletException e) {
            assertTrue(e.getMessage().contains("mandatory"));
        }
        Wallet wallet = new WalletProtobufSerializer().readWallet(params,
                new WalletExtension[]{ new FooWalletExtension("com.whatever.required", true) },
                proto, null);
        assertTrue(wallet.getExtensions().containsKey("com.whatever.required"));

        // Non-mandatory extensions are ignored if the wallet doesn't know how to read them.
        Wallet wallet2 = new Wallet(params, null);
        wallet2.addExtension(new FooWalletExtension("com.whatever.optional", false));
        Protos.Wallet proto2 = new WalletProtobufSerializer().walletToProto(wallet2);
        Wallet wallet5 = new WalletProtobufSerializer().readWallet(params, null, proto2, null);
        assertEquals(0, wallet5.getExtensions().size());
    }

    @Test
    public void extensionsWithError() throws Exception {
        WalletExtension extension = new WalletExtension() {

            @Override
            public String getWalletExtensionID() {
                return "test";
            }

            @Override
            public boolean isWalletExtensionMandatory() {
                return false;
            }

            @Override
            public byte[] serializeWalletExtension() {
                return new byte[0];
            }

            @Override
            public void deserializeWalletExtension(Wallet containingWallet, byte[] data) throws Exception {
                throw new NullPointerException();  // Something went wrong!
            }
        };
        myWallet.addExtension(extension);
        Protos.Wallet proto = new WalletProtobufSerializer().walletToProto(myWallet);
        Wallet wallet = new WalletProtobufSerializer().readWallet(params, new WalletExtension[]{extension}, proto, null);
        assertEquals(0, wallet.getExtensions().size());
}

    @Test(expected = UnreadableWalletException.FutureVersion.class)
    public void versions() throws Exception {
        Protos.Wallet.Builder proto = Protos.Wallet.newBuilder(new WalletProtobufSerializer().walletToProto(myWallet));
        proto.setVersion(2);
        new WalletProtobufSerializer().readWallet(params, null, proto.build(), null);
    }
}
