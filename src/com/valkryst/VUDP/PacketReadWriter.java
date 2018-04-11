package com.valkryst.VUDP;

import org.apache.logging.log4j.LogManager;

import java.net.*;

public class PacketReadWriter extends Thread {
    /** The host address of the server to communicate with. */
    private final InetAddress serverHost;
    /** The port of the server to communicate with. */
    private final int serverPort;

    /** The socket to read/write packets with. */
    private final DatagramSocket socket;

    /** The packet writer. */
    private final PacketWriter writer;
    /** The packet reader. */
    private PacketReader reader;

    /**
     * Constructs a new PacketReadWriter.
     *
     * @param serverHost
     *          The host address of the server to communicate with.
     *
     * @param serverPort
     *          The port of the server to communicate with.
     *
     * @param clientPort
     *          The port to listen for packets, from the server, on.
     *
     * @param readBufferSize
     *          The size of the buffer used to receive packets.
     *
     * @throws IllegalArgumentException
     *          If the host is empty.
     *          If the port isn't within the range of 0-65535.
     *
     * @throws UnknownHostException
     *          If the host is unknown.
     *
     * @throws SocketException
     *          If there is an error getting/setting the SoTimeout of the
     *          DatagramSocket.
     */
    public PacketReadWriter(final String serverHost, final int serverPort, final int clientPort, final int readBufferSize) throws UnknownHostException, SocketException {
        if (serverHost != null && serverHost.isEmpty()) {
            throw new IllegalArgumentException("You must specify a server host.");
        }

        if (serverPort != -1 && (serverPort < 0 || serverPort > 65535)) {
            throw new IllegalArgumentException("The server port must be an unused port from 0-65535 or set to -1.");
        }

        if (clientPort < 0 || clientPort > 65535) {
            throw new IllegalArgumentException("The client port must be an unused port from 0-65535.");
        }

        this.serverHost = InetAddress.getByName(serverHost);
        this.serverPort = serverPort;

        socket = new DatagramSocket(clientPort);
        socket.setSoTimeout(10_000);

        reader = new PacketReader(socket, readBufferSize);
        writer = new PacketWriter(socket);
    }

    /**
     * Constructs a new PacketReadWriter.
     *
     * Defaults the readBufferSize to 1024.
     *
     * @param serverHost
     *          The host address of the server to communicate with.
     *
     * @param serverPort
     *          The port of the server to communicate with.
     *
     * @param clientPort
     *          The port to listen for packets, from the server, on.
     *
     * @throws IllegalArgumentException
     *          If the host is empty.
     *          If the port isn't within the range of 0-65535.
     *
     * @throws UnknownHostException
     *          If the host is unknown.
     *
     * @throws SocketException
     *          If there is an error getting/setting the SoTimeout of the
     *          DatagramSocket.
     */
    public PacketReadWriter(final String serverHost, final int serverPort, final int clientPort) throws UnknownHostException, SocketException {
        this(serverHost, serverPort, clientPort, 1024);
    }

    @Override
    public void run() {
        reader.start();
        writer.start();

        try {
            reader.join();
            writer.join();
        } catch (final InterruptedException e) {
            LogManager.getLogger().error(e);
        }

        socket.close();
    }

    /**
     * Adds a packet to the tail of the queue of packets to be sent.
     * Waiting, if necessary, for room to be made, in the queue, for the
     * new packet.
     *
     * Attempts to set the destination address/port if it hasn't already
     * been set.
     *
     * @param packet
     *          The packet.
     *
     * @throws InterruptedException
     *          If interrupted while waiting to put a packet in the queue.
     *
     * @throws IllegalArgumentException
     *          If the packet's destination address of port have not been
     *          set and the PacketReadWriter was not created with a
     *          server host/port.
     */
    public void queuePacket(final DatagramPacket packet) throws InterruptedException {
        if (packet.getAddress() == null) {
            if (serverHost == null) {
                throw new IllegalArgumentException("You must manually set the destination address of the packet. The PacketReadWriter was not created with a server host.");
            } else {
                packet.setAddress(serverHost);
            }
        }

        if (packet.getPort() == -1) {
            if (serverPort == -1) {
                throw new IllegalArgumentException("You must manually set the destination port of the packet. The PacketReadWriter was not created with a server port.");
            } else {
                packet.setPort(serverPort);
            }
        }

        writer.queuePacket(packet);
    }

    /**
     * Retrieves the head packet from the queue of packets received.
     * Waiting, if necessary, for a packet to be added to the queue.
     *
     * @throws InterruptedException
     *          If interrupted while waiting to take a packet from the queue.
     */
    public DatagramPacket dequeuePacket() throws InterruptedException {
        return reader.dequeuePacket();
    }

    /** Shuts down the reader and writer. */
    public void shutdown() {
        reader.setRunning(false);
        writer.setRunning(false);
    }
}
