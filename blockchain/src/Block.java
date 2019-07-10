import com.google.common.hash.HashCode;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.ArrayList;

public class Block {
    //TODO: determine how to make size dynamic
    //TODO: !!!signature!!!
    private final int BLOCK_SIZE = 2;


    private int id;
    private Transaction[] transactions;
    private int nextEmpty;
    private byte[] signature;
    private HashCode hashCode;
    private HashCode prevHashCode;

    public Block(int id) {
        transactions = new Transaction[BLOCK_SIZE];
        this.id = id;
        nextEmpty = 0;
        prevHashCode = null;
    }

    void addTransaction(Transaction transaction) {
        transactions[nextEmpty] = transaction;
        nextEmpty++;

    }

    int getId() {
        return id;
    }

    boolean isFull() {
        return transactions[BLOCK_SIZE - 1] != null;
    }

    public Transaction[] getTransactions() {
        return transactions;
    }

    String getInfo() {
        return "Block #" + id + "\t Hashcode: " + hashCode + "\t Previous hashcode: " + prevHashCode + "\t Signature: " + (signature == null ? "null" : Hasher.bytesToHex(signature));
    }

    String getTruncInfo() {
        return "Block #" + id + "\t Hashcode: " + (hashCode == null ? "null" : truncate(hashCode.toString())) + "\t Previous hashcode: " + (prevHashCode == null ? "null" : truncate(prevHashCode.toString())) + "\t Signature: " + (signature == null ? "null" : truncate(Hasher.bytesToHex(signature)));
    }

    String truncate(String string) {
        if (string == null)
            return null;
        return string.substring(0, 6) + "..." + string.substring(string.length() - 8, string.length() - 1);
    }


    void pack(KeyPair keyPair, HashCode prevHashCode) throws Exception {
        hashCode = HashCode.fromString(createMercleRoot());
        Signature ecdsa = Signature.getInstance("SHA256withECDSA");
        ecdsa.initSign(keyPair.getPrivate());
        ecdsa.update(hashCode.asBytes());
        signature = ecdsa.sign();
        this.prevHashCode = prevHashCode;
    }

    private String createMercleRoot() throws NoSuchAlgorithmException {
        if (transactions.length == 0)
            return completeShaString("");
        ArrayList<String> shas = new ArrayList<>(transactions.length);
        for (Transaction transaction : transactions) {
            ParcelDataTransaction parcelDataTransaction = (ParcelDataTransaction) transaction;
            shas.add(parcelDataTransaction.getHashCode().toString());
        }
        int count = transactions.length;
        int offset = 0;
        int newCount;
        boolean last;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        while (count != 1) {
            newCount = 0;
            last = (count & 1) == 1;
            if (last)
                count--;
            for (int i = offset; i < count + offset; i += 2) {
                BigInteger dblSha = new BigInteger(shas.get(i) + shas.get(i + 1), 16);
                shas.add(completeShaString(new BigInteger(1, digest.digest(digest.digest(dblSha.toByteArray()))).toString(16)));
                newCount++;
            }
            if (last) {
                BigInteger dblSha = new BigInteger(shas.get(count + offset) + shas.get(count + offset), 16);
                shas.add(completeShaString(new BigInteger(1, digest.digest(digest.digest(dblSha.toByteArray()))).toString(16)));
                newCount++;
                count++;
            }

            offset += count;
            count = newCount;
        }
        return shas.get(shas.size() - 1);
    }

    public static String completeShaString(String sha) {
        return completeStringWith(sha, 64);
    }

    public HashCode getHashCode() {
        return hashCode;
    }

    private static String completeStringWith(String string, int len) {
        int need = len - string.length();
        if (need == 0)
            return string;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < need; i++) {
            builder.append("0");
        }
        return builder.append(string).toString();
    }
}
