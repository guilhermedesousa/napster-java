<div align="center">
<h1>Napster</h1>
</div>

This project consists of a Peer-to-Peer (P2P) system developed in Java, allowing the transfer of large video files (MP4) transfer among peers.

The project is a simple implementation of a system that was very popular in the year 1999, known as Napster. It was also based on P2P file sharing, but with a focus on audio files.

***

### Comunication

The data transfer between the parts of the system occurs through Remote Method Invocation (RMI) and the Transmission Control Protocol (TCP). The RMI is used for the peer-server communication, where the Peer invokes methods on the Server and receive responses. On the other hand, TCP is used for the video data transfer between peers.

***

### Architecture

The system architecture is based on a centralized Server that keeps peer information such as IP address, port, and downloaded files, along with the Peers themselves.

**1. Server**

The server is responsible for storing the information of peers that join the network in a data structure, updating this information as peers perform download operations, and conducting searches for peers that possess the requested files.

**1.1 Data structure**

The storage of peer information is done non-persistently using Java HashMap. The selection of this structure is justified by its ease of storing data in a key-value mode and performing seaches based on specific keys.

- `key`: file name with its extension
- `value`: list of peers that contain the file

**2. Peer**

Peer is an entity capable to request video files from other peers in the network, as well as to provide files, done with TCP.

**Threads**

To make the system scalable and prevent process from becoming blocked, threads were employed within the peer entity.

To support multiple requests coming from other peers, each peer creates a thread named `FileServerThread`, which is responsible for accepting the connections initiated by other peers.

**Support large video files**

To transfer large video files, specifically those exceeding 1 gigabyte in size, the video's bytes is sent in small, separated packages.

***

### Compilation

The compilation of the source code should be done in the terminal, compiling all the .java files simultaneously. Here's the suggested compilation code:

```
javac -d .\bin\ .\src\server\ServerInterface.java .\src\server\ServerImpl.java .\src\server\Server.java .\src\peer\Peer.java
```

***

### Console operations

**Server startup**

```
java server.Server <ip> <RMI port>
```

**Peer startup (JOIN)**

```
java peer.Peer <ip> <port> <directory>
```

**SEARCH operation in the Peer console**

```
<file name with its extension>
```

**DOWNLOAD operation in the Peer console**

```
<peer ip>:<peer port>
```

***

<div align="center">
Developed by <a href="https://github.com/guilhermedesousa">Guilherme Santos</a>
</div>