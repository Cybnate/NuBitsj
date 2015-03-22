package com.matthewmitchell.nubitsj.examples;

import com.matthewmitchell.nubitsj.core.*;
import com.matthewmitchell.nubitsj.kits.WalletAppKit;
import com.matthewmitchell.nubitsj.params.TestNet3Params;
import com.matthewmitchell.nubitsj.script.Script;

import java.io.File;
import java.util.List;

/**
 * The following example shows how to use the by nubitsj provided WalletAppKit.
 * The WalletAppKit class wraps the boilerplate (Peers, BlockChain, BlockStorage, Wallet) needed to set up a new SPV nubitsj app.
 * 
 * In this example we also define a WalletEventListener class with implementors that are called when the wallet changes (for example sending/receiving money)
 */
public class Kit {

    public static void main(String[] args) {

        // First we configure the network we want to use.
        // The available options are:
        // - MainNetParams
        // - TestNet3Params
        // - RegTestParams
        // While developing your application you probably want to use the Regtest mode and run your local nubits network. Run nubitsd with the -regtest flag
        // To test you app with a real network you can use the testnet. The testnet is an alternative nubits network that follows the same rules as main network. Coins are worth nothing and you can get coins for example from http://faucet.xeno-genesis.com/
        // 
        // For more information have a look at: https://nubitsj.github.io/testing and https://nubits.org/en/developer-examples#testing-applications
        NetworkParameters params = TestNet3Params.get();

        // Now we initialize a new WalletAppKit. The kit handles all the boilerplate for us and is the easiest way to get everything up and running.
        // Have a look at the WalletAppKit documentation and its source to understand what's happening behind the scenes: https://github.com/nubitsj/nubitsj/blob/master/core/src/main/java/com.matthewmitchell.nubitsj/kits/WalletAppKit.java
        WalletAppKit kit = new WalletAppKit(params, new File("."), "walletappkit-example");

        // In case you want to connect with your local nubitsd tell the kit to connect to localhost.
        // You must do that in reg test mode.
        //kit.connectToLocalHost();

        // Now we start the kit and sync the blockchain.
        // nubitsj is working a lot with the Google Guava libraries. The WalletAppKit extends the AbstractIdleService. Have a look at the introduction to Guava services: https://code.google.com/p/guava-libraries/wiki/ServiceExplained
        kit.startAsync();
        kit.awaitRunning();

        // To observe wallet events (like coins received) we implement a EventListener class that extends the AbstractWalletEventListener nubitsj then calls the different functions from the EventListener class
        WalletListener wListener = new WalletListener();
        kit.wallet().addEventListener(wListener);

        // Ready to run. The kit syncs the blockchain and our wallet event listener gets notified when something happens.
        // To test everything we create and print a fresh receiving address. Send some coins to that address and see if everything works.
        System.out.println("send money to: " + kit.wallet().freshReceiveAddress().toString());

        // Make sure to properly shut down all the running services when you manually want to stop the kit. The WalletAppKit registers a runtime ShutdownHook so we actually do not need to worry about that when our application is stopping.
        //System.out.println("shutting down again");
        //kit.stopAsync();
        //kit.awaitTerminated();
    }

    // The Wallet event listener its implementations get called on wallet changes.
    static class WalletListener extends AbstractWalletEventListener {

        @Override
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            System.out.println("-----> coins resceived: " + tx.getHashAsString());
            System.out.println("received: " + tx.getValue(wallet));
        }

        @Override
        public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {
            System.out.println("-----> confidence changed: " + tx.getHashAsString());
            TransactionConfidence confidence = tx.getConfidence();
            System.out.println("new block depth: " + confidence.getDepthInBlocks());
        }

        @Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            System.out.println("coins sent");
        }

        @Override
        public void onReorganize(Wallet wallet) {
        }

        @Override
        public void onWalletChanged(Wallet wallet) {
        }

        @Override
        public void onKeysAdded(List<ECKey> keys) {
            System.out.println("new key added");
        }

        @Override
        public void onScriptsAdded(Wallet wallet, List<Script> scripts) {
            System.out.println("new script added");
        }
    }

}
