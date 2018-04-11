![](https://codebuild.us-east-1.amazonaws.com/badges?uuid=eyJlbmNyeXB0ZWREYXRhIjoiSk5CSnJDUm10YnYwLzIyMnZhOTkxZDVkWlhjUGtuMWFOVEMvNW14ZitUSU5iMWdUdG1WSWlUN1pneDE3L0xUYWZVMFl4M0hzMzdqSEdLTWJJRVQrTFNZPSIsIml2UGFyYW1ldGVyU3BlYyI6ImJqeGlzS3h4ZFVtaHliZ2UiLCJtYXRlcmlhbFNldFNlcmlhbCI6MX0%3D&branch=master) [![Release](https://jitpack.io/v/Valkryst/VUDP.svg)](https://jitpack.io/#Valkryst/VUDP)

## Links

* [Examples](https://github.com/Valkryst/VUDP/wiki)

## Jar Files & Maven

The Maven dependency is hosted off of JitPack, so you will need to add JitPack as a repository before you add VUDP as a dependency.

### Maven

JitPack ([Example](https://github.com/Valkryst/VTerminal/blob/master/pom.xml)):

    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

Dependency:

    <dependency>
        <groupId>com.github.Valkryst</groupId>
        <artifactId>VUDP</artifactId>
        <version>1.1.0</version>
    </dependency>

### Jar

Jar files can be found on the [releases](https://github.com/Valkryst/VUDP/releases) page.


## Example

In this example, we create a `PacketReadWriter`, then we continuously send, receive, and display 100 packets on the local machine.

```java
package com.valkryst.VUDP;

import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;

public class Driver {
    public static void main(String[] args) throws IOException, InterruptedException {
        final String serverHost = "127.0.0.1";
        final int serverPort = 9998;
        final int clientPort = 9998;
        final PacketReadWriter readWriter = new PacketReadWriter(serverHost, serverPort, clientPort);
        readWriter.start();

        for (int i = 0 ; i < 100 ; i++) {
            final Message sendMessage = new Message("Packet #" + i);
            readWriter.queuePacket(sendMessage.toPacket());

            final DatagramPacket packet = readWriter.dequeuePacket();
            final Message receivedMessage = (Message) Package.fromPacket(packet);
            System.out.println("Received Packet: " + receivedMessage.getMessage());

            Thread.sleep(100);
        }

        readWriter.shutdown();
    }

    private static class Message implements Package, Serializable {
        /** The message. */
        @Getter
        private final String message;

        /**
         * Constructs a new Message.
         *
         * @param message
         *          The message.
         */
        public Message(final @NonNull String message) {
            this.message = message;
        }
    }
}
```