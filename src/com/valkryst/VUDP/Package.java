package com.valkryst.VUDP;

import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.DatagramPacket;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public interface Package {
    /**
     * Converts this packable object into a GZIPed array of bytes.
     *
     * @return
     *          The GZIPed bytes.
     *
     * @throws IOException
     *          If an IO exception occurs.
     */
    default byte[] toBytes() throws IOException {
        // Create IO Streams
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);

        // Create Shutdown Hook for IO Streams
        final Thread shutdownCode = new Thread(() -> {
            try {
                objectOutputStream.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });

        Runtime.getRuntime().addShutdownHook(shutdownCode);

        // Write Object to Stream
        objectOutputStream.writeObject(this);
        objectOutputStream.flush();

        // Remove Shutdown Hook and Close Streams
        Runtime.getRuntime().removeShutdownHook(shutdownCode);
        shutdownCode.run();

        // Retrieve Compressed Object Bytes from Byte Output Stream
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Attempts to pack this packable object into a DatagramPacket.
     *
     * @return
     *          The packet.
     *
     * @throws IOException
     *          If an IO exception occurs.
     */
    default DatagramPacket toPacket() throws IOException {
        final byte[] buffer = toBytes();
        return new DatagramPacket(buffer, buffer.length);
    }

    /**
     * Attempts to read a packable object from a set of GZIPed bytes.
     *
     * @param data
     *          The byte data.
     *
     * @return
     *          The object.
     *
     * @throws IOException
     *          If there's an IO error.
     *
     * @throws ClassNotFoundException
     *          If the class of the serialized object, represented by the
     *          input data, cannot be found.
     */
    static Package fromBytes(final byte[] data) throws IOException, ClassNotFoundException {
        // Create IO Streams
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        final GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        final ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);

        // Create Shutdown Hook for IO Streams
        final Thread shutdownCode = new Thread(() -> {
            try {
                objectInputStream.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        });

        Runtime.getRuntime().addShutdownHook(shutdownCode);

        // Convert Packet Bytes into Message
        final Package object = (Package) objectInputStream.readObject();

        // Remove Shutdown Hook and Close Streams
        Runtime.getRuntime().removeShutdownHook(shutdownCode);
        shutdownCode.run();

        return object;
    }

    /**
     * Attempts to read a packable object from a DatagramPacket.
     *
     * @param packet
     *          The packet.
     *
     * @return
     *          The object, or null if no object could be read.
     */
    static Package fromPacket(final DatagramPacket packet) {
        if (packet == null) {
            return null;
        }

        try {
            return Package.fromBytes(packet.getData());
        } catch (final IOException | ClassNotFoundException e) {
            LogManager.getLogger().error(e.getMessage());
            return null;
        }
    }
}
