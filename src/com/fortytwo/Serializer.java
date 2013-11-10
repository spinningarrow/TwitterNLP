package com.fortytwo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Radzinzki
 * Date: 10/11/13
 * Time: 8:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class Serializer {
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        GZIPOutputStream g = new GZIPOutputStream(b);
        ObjectOutputStream o = new ObjectOutputStream(g);
        o.writeObject(obj);
        o.close();
        return b.toByteArray();
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        GZIPInputStream g = new GZIPInputStream(b);
        ObjectInputStream o = new ObjectInputStream(g);
        Object obj = o.readObject();
        o.close();
        return obj;
    }
}
