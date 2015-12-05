/**
 * Copyright 2014 Rimbit Developers.
 * Copyright 2014 Matthew Mitchell
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

import com.google.common.base.Charsets;
import static com.google.common.base.Preconditions.checkNotNull;
import com.matthewmitchell.nubitsj.core.AbstractBlockChain;
import com.matthewmitchell.nubitsj.core.Coin;
import com.matthewmitchell.nubitsj.core.Sha256Hash;
import com.matthewmitchell.nubitsj.core.StoredBlock;
import com.matthewmitchell.nubitsj.core.Utils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.OutputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

public class ValidHashStore {

    private static final Logger log = LoggerFactory.getLogger(ValidHashStore.class);

    private File filePath;
    private ArrayList<byte[]> validHashesArray = new ArrayList<byte[]>();
    int index;
    boolean initialFind = true;

    private static String GENESIS_MINI_HASH = "3fd7ba4cdd47e969d1ef0412dc9d2175"; 

    public interface TrustedServersInterface {
        /**
         * Implement the retrieval of server URLs.
         * @param didFail True when the last server failed. If true then a different server should be given, or null if there are no more servers to try.
         * @return The URL of the next server, or null if there are no more servers to try.
         */
        public URL getNext(boolean didFail);
        /**
         * Should return true if the server list has invalidated the previous returned server.
         */
        public boolean invalidated();
        /**
         * Marks the last returned server as failing or succeeding.
         */
        public void markSuccess(boolean success);
    }

    private TrustedServersInterface servers;

    public ValidHashStore(File filePath, TrustedServersInterface servers) throws IOException {

        this.filePath = filePath;
        this.servers = servers;

        long len = filePath.length();

        index = 0;

        if (len == 0) {
            // Add genesis hash and that is all
            BufferedOutputStream file = getOutputStream();
            writeHash(Hex.decode(GENESIS_MINI_HASH), file);
            file.flush();
            file.close();
            return;
        }

        // Load valid hashes from file.

        FileInputStream file = new FileInputStream(filePath);

        byte[] data = new byte[(int)len];
        file.read(data, 0, (int)len);

        byte[] b = new byte[16];

        for (int x = 0; x < len; x += 16) {
            System.arraycopy(data, x, b, 0, 16);
            validHashesArray.add(b);
            b = new byte[16];
        }

        file.close();

    }

    private BufferedOutputStream getOutputStream() throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(this.filePath));
    }

    private void writeHash(byte[] hash, BufferedOutputStream file) throws IOException {

        validHashesArray.add(hash);
        file.write(hash, 0, 16);

    }

    private boolean isInValidHashes(byte[] cmpHash) {

        for (;index < validHashesArray.size(); index++) {

            if (Arrays.equals(validHashesArray.get(index), cmpHash)) {
                index++;
                initialFind = false;
                return true;
            }

            // Else if we are finding initial index continue, else fail.
            if (!initialFind) return false;

        }

        return false;

    }

    private byte[] getHashFromInputStream(InputStream is) throws IOException {

        byte[] hash = new byte[16];
        int x = 0, res;

        while (x < 16 && (res = is.read()) != -1)
            hash[x++] = (byte) res;

        if (x != 16)
            return null;

        return hash;

    }
    
    private HttpURLConnection getConnection(final URL url, String contentType) throws ProtocolException, IOException {
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Accept-Encoding", ""); 
        connection.setDoOutput(true);
        
        return connection;
        
    }

    private Void downloadHashes(final URL server, final byte[] locator, final int locatorSize) throws IOException {

        try {

            HttpURLConnection connection = getConnection(server, "application/octet-stream");
            OutputStream os = connection.getOutputStream();
            os.write(locator, 0, locatorSize);
            os.flush();
            os.close();
            
            InputStream is = null;

            try {

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {

                    is = new BufferedInputStream(connection.getInputStream());

                    // We are going to replace the valid hashes with the new ones

                    BufferedOutputStream file = getOutputStream();
                    validHashesArray.clear();
                    index = 0;
                    initialFind = true;

                    // Write new hashes. Ensure a limit of 50,000 hashes.

                    byte[] b;

                    for (int x = 0; (b = getHashFromInputStream(is)) != null && x < 50000; x++)
                        writeHash(b, file);

                    file.flush();
                    file.close();
                    
                    return null;

                }

            } finally {
                if (is != null)
                    is.close();
                connection.disconnect();
            }

        } catch (IOException e) {
            log.warn("Got IO error when receiving valid block hashes from " + server.toString(), e);
        }
        
        throw new IOException();
        
    }
    
    private Coin getFee(URL server, byte[] urlParams) throws MalformedURLException, IOException {
        
        server = new URL(server.toString() + "/getfee");
        
        try {
            
            HttpURLConnection connection = getConnection(server, "application/x-www-form-urlencoded");
            OutputStream os = connection.getOutputStream();
            os.write(urlParams);
            os.flush();
            os.close();
            
            InputStream is = null;
            
            try {

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    
                    is = connection.getInputStream();

                    Reader reader = new InputStreamReader(is, Charsets.UTF_8);
                    
                    final StringBuilder content = new StringBuilder();
                    final char[] buffer = new char[10];
                    int n = 0;
                    while (-1 != (n = reader.read(buffer)))
                        content.append(buffer, 0, n);
                    
                    return Coin.parseCoin(content.toString());
                    
                }
                
            } finally {
                if (is != null)
                    is.close();
                connection.disconnect();
            }    
            
        } catch (IOException e) {
            log.warn("Got IO error when obtaining transaction fee from " + server.toString(), e);
        }
        
        throw new IOException();
        
    }
    
    private abstract class ServerTask <RetType> {
        
        abstract RetType run (final URL server) throws IOException;
    
        public RetType runAll(boolean mark) throws IOException {

            URL server;
            boolean failed = false;
            RetType ret = null;

            do {

                if (failed && mark)
                    servers.markSuccess(false);

                server = servers.getNext(failed);
                if (server == null)
                    throw new IOException("No more servers to try.");
                
                try {
                    ret = run(server);
                    failed = false;
                } catch (IOException e) {
                    failed = true;
                } 

            } while (failed);

            if (mark)
                servers.markSuccess(true);
            
            return ret;

        }
        
    }
    
    public Coin getFee(int bytes, Coin amount) throws IOException {
        
        String urlParams = String.format("bytes=%d&amount=%s", bytes, amount.toPlainString());
        final byte[] urlParamsBytes = urlParams.getBytes(Charsets.UTF_8);
        
        try {
            
            return new ServerTask <Coin> () {

                @Override
                Coin run(final URL server) throws IOException {
                    return getFee(server, urlParamsBytes);
                }

            }.runAll(false);
            
        } catch (IOException e) {
            throw new IOException("Could not obtain fees from any server.");
        }
        
    }

    public boolean isValidHash(Sha256Hash hash, AbstractBlockChain blockChain, boolean waitForServer) throws IOException {

        // Get 16 bytes only
        byte[] cmpHash = new byte[16];
        System.arraycopy(Utils.reverseBytes(hash.getBytes()), 0, cmpHash, 0, 16);

        // First check the existing hashes
        if (!servers.invalidated() && isInValidHashes(cmpHash))
            return true;

        // Nope. We need to ensure the valid hashes is synchronised with the server

        // Create POST data locator

        final byte[] locator = new byte[3200];

        BlockStore store = checkNotNull(blockChain).getBlockStore();
        StoredBlock chainHead = blockChain.getChainHead();

        StoredBlock cursor = chainHead;
        int offset = 0;

        for (int i = 100; cursor != null && i > 0; i--, offset += 32) {
            System.arraycopy(Utils.reverseBytes(cursor.getHeader().getHash().getBytes()), 0, locator, offset, 32);

            try {
                cursor = cursor.getPrev(store);
            } catch (BlockStoreException e) {
                throw new RuntimeException(e);
            }
        }

        // Now download hashes from server.

        // But if waitForServer is true, first wait a while in case the server hasn't received or processed this block yet.
        // We assume the server is well connected and 30 seconds would therefore be more than enough in most cases.
        if (waitForServer)
            Utils.sleep(30000);
        
        final int locSize = offset;
        
        new ServerTask<Void>() {

            @Override
            public Void run(final URL server) throws IOException {
                return downloadHashes(server, locator, locSize);
            }
            
        }.runAll(true);

        // Lastly check valid hashes again
        return isInValidHashes(cmpHash);

    }

    public void close(){
        // Dummy method in case we add/remove it.
    }

}
