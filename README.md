# HW1 Peer to Peer Overlay

This code implements routing schemes for a P2P network overlay. The overlay consists of two main actors: a registry, and a group of messaging nodes. The registry handles registration of messaging nodes, dispatches routing tables, orders message sends, and collects traffic summaries. The messaging nodes send, receive, and relay messages to other messaging nodes as well as reporting a summary of their traffic to the registry.

# Running the program

1. Use make to build the project.
2. Start the registry using `java cs455.overlay.node.Registry <portnum>`. The registry will print out it's IP address and portnumber that it is listening on.
3. For any number of messaging nodes use this command to start up and connect to the server: `java cs455.overlay.node.MessagingNode <registry-IP> <registry-port>`
4. Once you have connected all the messaging nodes you want use the `setup-overlay <routing-table-size>` command to setup the overlay. *Note: the routing table size must be no greater than half the number of nodes in the overlay.*
5. To start sending messages use the `start <number-of-messages>` command. This can be used multiple times, just make sure to wait for the traffic summaries to print.


# Packages

This repo is seperated into 5 main packages: node, routing, transport, util, and wireframes.

## Node

Contains the code for the Registry and Messaging Nodes. They both work on an event based design where incoming messages are treated depending on their message type (more on this in wireframes). When a new message is received the onEvent(Event e) function is called in a node. Each node has specific behavior.

### Registry

#### Behavior

1. Handles registration requests, by registering nodes in the manifest.
2. Handles deregistration request, by removing nodes from the overlay and manifest.
3. Reports status of node registrations.
4. Tracks which nodes are finished with their message sending task for each 'start' command.
5. Receives traffic summaries and prints a formatted table.

#### Commands

1. `setup-overlay <routing-table-size>` : sets up the overlay by dispatching routing tables to each node.
2. `start <number-of-messages-to-send>` : sends a request to each node to start sending messages. After all nodes have finished requests and displays a traffic summary.
3. `list-messaging-nodes` : lists messaging nodes in the overlay.
4. `list-routing-tables` : lists routing tables for each messaging node.
5. `q[uit]` : shuts down the registry.

### MessagingNode

#### Behavior
1. Upon start up sends registration request to registry.
2. When given a routing table connects to nodes in that table.
3. When given a task initiate request begins to send messages.
4. When given a traffic summary request sends a traffic summary of it's messages sent, relayed, and received. Then clears all its counters.

#### Commands
1. `print-counters-and-diagnostics` : prints all current counters and diagnostic information for messages sent, received, and relayed.
2. `exit-overlay` : sends a deregistration request to the registry and the shuts down on successful deregistration.
3. `q[uit]` : shuts down the messaging node, but does not deregister.

## Routing

Contains all code used by the registry and messaging nodes to maintain routing tables and manifests.

### Routing Table

A map that represents a routing table or manifest. Has API for safely adding and removing entries, printing the manifest, printing routing tables, and determine routing tables for a given node and routing table size.

### Routing Table Entry

Represents an entry in the routing table containing data about a messaging nodes IP address, port number, registered ID, and whether that node has finished its task (for the registry to track finished tasks).

## Transport

Contains classes for managing TCP connections for both the registry and messaging nodes.

### TCP Connection

Represents a TCP Connection by encapsulating a socket. Consists of two subclasses, a sender and receiver.

#### TCP Sender Thread

Can be threaded to pull messages off a queue. These messages are sent to on the output stream of the connection. Can also just be used to directly send a message (byte []) to the client.

#### TCP Receiver Thread

A threaded that listens for new data to come over the socket. Upon receiving new data creates an Event using the EventFactory. Then aysnchronously calls it's master's (either a Registry or MessagingNode) onEvent method by using a thread pool.

### TCP Connection Cache

A collection that contains a series of TCP Connections. Has behavior for adding and removing connections, as well as retrieving a connection by registered node id.

## Util

A package for helper / utility classes. Only consists of one class StatisticsCollectorAndDisplay.

### StatisticsCollectorAndDisplay

Collects statistics on messages sent, relayed, and received. As well as payload sums, packets, touched and other data. Used by nodes to collect data as they are relaying messages. The registry can use the StatisticsCollectorAndDisplay to print formatted output of traffic summaries.

## Wireframes

Consists of all the classes for each specific message type. Every wireframe implements the Event class which defines each wireframe as having a getType() function as well as methods for marshalling and unmarshalling bytestreams. To read more about each specific wireframe please check the assignment [description](https://www.cs.colostate.edu/~cs455/CS455-Spring18-HW1-PC.pdf).