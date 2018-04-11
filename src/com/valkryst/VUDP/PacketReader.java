package com.valkryst.VUDP;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class PacketReader extends Thread {
    /** The size of the buffer used to receive packets. */
    private int bufferSize = 1024;

    /** The socket to read from. */
    private final DatagramSocket socket;

    /** Whether to continue running. */
    @Getter @Setter private boolean running = true;

    /** The FIFO queue of packets received. */
    private final BlockingQueue<DatagramPacket> queue = new LinkedBlockingQueue<>(10_000);

    /**
     * Constructs a new PacketReader.
     *
     * The socket will have it's SoTimeout value set to 10_000 if it has
     * not been set.
     *
     * @param socket
     *          The socket to read from.
     *
     * @throws SocketException
     *          If there is an error getting/setting the SoTimeout.
     */
    public PacketReader(final @NonNull DatagramSocket socket) throws SocketException {
        this.socket = socket;

        if (socket.getSoTimeout() == 0) {
            socket.setSoTimeout(10_000);
        }
    }

    /**
     * Constructs a new PacketReader.
     *
     * The socket will have it's SoTimeout value set to 10_000 if it has
     * not been set.
     *
     * The bufferSize will default to 1024 if it's set to a negative value
     * or zero.
     *
     * @param socket
     *          The socket to read from.
     *
     * @param bufferSize
     *          The size of the buffer used to receive packets.
     *
     * @throws SocketException
     *          If there is an error getting/setting the SoTimeout.
     */
    public PacketReader(final @NonNull DatagramSocket socket, final int bufferSize) throws SocketException {
        this(socket);

        if (bufferSize > 0) {
            this.bufferSize = bufferSize;
        } else {
            this.bufferSize = 1024;
        }
    }

    @Override
    public void run() {
        while (running) {
            final byte[] buffer = new byte[bufferSize];
            final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                socket.receive(packet);

                for (int attempt = 0 ; attempt < 4 ; attempt++) {
                    try {
                        queue.put(packet);
                        break;
                    } catch (InterruptedException e) {
                        LogManager.getLogger().error(e.getMessage());
                    }
                }
            } catch (final SocketTimeoutException ignored) {
                // Happens so the `running` var can be re-checked.
            } catch (final IOException e) {
                LogManager.getLogger().error(e.getMessage());
            }
        }
    }

    /**
     * Retrieves the head packet from the queue. Waiting, if necessary,
     * for a packet to be added to the queue.
     *
     * @throws InterruptedException
     *          If interrupted while waiting to take a packet from the queue.
     */
    public DatagramPacket dequeuePacket() throws InterruptedException {
        final DatagramPacket packet = queue.take();
        queue.remove(packet);
        return packet;
    }
}
