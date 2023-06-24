package peer;

import server.ServerInterface;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class Peer {
    private String ip;
    private String port;
    private String folder;
    List<String> files;
    ServerInterface sic;

    /**
     * Peer thread to deal with file requests
     */
    public class PeerServiceThread extends Thread {
        private Socket node = null;

        public PeerServiceThread(Socket node) {
            this.node = node;
        }

        public void run() {
            try {
                InputStreamReader is = new InputStreamReader(node.getInputStream());
                BufferedReader reader = new BufferedReader(is);

                OutputStream os = node.getOutputStream();
                DataOutputStream writer = new DataOutputStream(os);

                String fileName = reader.readLine();
                File fileToSend = new File(folder, fileName);

                boolean connectionAccepted = Math.random() >= 0.5;

                if (connectionAccepted) {
                    if (fileToSend.exists() && fileToSend.canRead()) {
                        FileInputStream fileInputStream = new FileInputStream(fileToSend);

                        long fileSize = fileToSend.length();
                        writer.writeLong(fileSize);

                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            writer.write(buffer, 0, bytesRead);
                        }

                        fileInputStream.close();
                    }
                } else {
                    writer.writeLong(-1L);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Create an instance of the Peer.
     *
     * @param ip ip address of the peer
     * @param port port of the peer
     * @param folder folder path of the peer to retrieve and store files
     */
    public Peer(String ip, String port, String folder) {
        this.ip = ip;
        this.port = port;
        this.folder = folder;

        this.createPeerFolder();
        this.files = this.listFileNames();
        this.createServerInterface();
    }

    /**
     * Create the Peer folder, if it does not exist yet.
     */
    private void createPeerFolder() {
        File peerFolder = new File(folder);

        if (!peerFolder.exists()) {
            peerFolder.mkdirs();
        }
    }

    /**
     * Returns a list of file names from the peer folder.
     * @return the list of file names
     */
    private List<String> listFileNames() {
        List<String> fileNames = new ArrayList<>();

        File peerFolder = new File(folder);
        File[] files = peerFolder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }
        }

        return fileNames;
    }

    /**
     * Create the connection between the client and the server through RMI.
     */
    private void createServerInterface() {
        try {
            Registry registry = LocateRegistry.getRegistry();
            this.sic = (ServerInterface) registry.lookup("rmi://127.0.0.1/server");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Perform the join operation in the server through RMI.
     */
    private void join() {
        try {
            String response = sic.join(ip, port, files);

            if (response.equals("JOIN_OK")) {
                System.out.println(String.format("Sou peer %s:%s com arquivos %s", ip, port, String.join(" ", files)));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Perform the search operation in the server through RMI.
     *
     * @param fileName the requested file
     * @return a list of peers who contain the file, or an empty list otherwise
     */
    public void search(String fileName) {
        try {
            List<String[]> response = sic.search(ip, port, fileName);

            StringBuilder builder = new StringBuilder();
            builder.append("peers com arquivo solicitado: ");

            for (String[] peer : response) {
                builder.append(String.format("%s:%s ", peer[0], peer[1]));
            }

            System.out.println(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the peer information in the server with the new downloaded file.
     *
     * @param fileName the downloaded file
     */
    public void update(String fileName) {
        try {
            String response = sic.update(ip, port, fileName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start the peer server to attend to requests.
     * @param serverPort the peer server port
     */
    private void startFileServer(String serverPort) {
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(serverPort));

            while (true) {
                Socket clientSocket = serverSocket.accept();

                PeerServiceThread thread = new PeerServiceThread(clientSocket);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request a specific file to another Peer in the network.
     *
     * @param serverIP the server ip address
     * @param serverPort the server port
     * @param fileName the file name
     */
    public void requestFileToPeer(String serverIP, String serverPort, String fileName) {
        try {
            Socket s = new Socket(serverIP, Integer.parseInt(serverPort));

            OutputStream os = s.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);

            writer.writeBytes(fileName + "\n");

            File directory = new File(folder);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            DataInputStream dis = new DataInputStream(s.getInputStream());
            long fileSize = dis.readLong();

            if (fileSize == -1L) {
                System.out.println("Conexao rejeitada pelo servidor.");
                return;
            }

            File fileToReceive = new File(directory, fileName);

            InputStream is = s.getInputStream();
            FileOutputStream fos = new FileOutputStream(fileToReceive);

            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;

            while (totalBytesRead < fileSize && ((bytesRead = is.read(buffer)) != -1)) {
                fos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            System.out.printf("Arquivo %s baixado com sucesso na pasta %s\n", fileName, folder);
            this.update(fileName);

            fos.close();
            is.close();
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the peer input from the console.
     *
     * @return the input string
     * @throws IOException exception when reading from the console
     */
    private static String getPeerInput() throws IOException {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(is);
        return reader.readLine();
    }

    /**
     * Check if a given file name is valid.
     *
     * @param fileName the file name
     * @return true if it is a valid file name, false otherwise
     */
    private static boolean isValidFileName(String fileName) {
        return fileName.matches("^[^/\\\\?*:|\"<>\\s]+\\.[\\w]+$");
    }

    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                throw new IllegalArgumentException("Uso: java peer.Peer <IP> <port> <folder>");
            }

            String ip = args[0];
            String port = args[1];
            String folder = args[2];

            // Peer initialization
            Peer peer = new Peer(ip, port, folder);

            // Perform the JOIN operation
            peer.join();

            // Create a Thread to listen to requests
            Thread fileServerThread = new Thread(() -> {
                peer.startFileServer(port);
            });
            fileServerThread.start();

            String lastRequestedFile = null;

            while (true) {
                String peerInput = getPeerInput();

                if (peerInput.equalsIgnoreCase("exit")) {
                    System.out.println("Fechando o Peer...");
                    break;
                }

                String[] inputParts = peerInput.split(":");

                boolean isSearch = inputParts.length == 1;
                boolean isDownload = inputParts.length == 2;

                if (isSearch && isValidFileName(peerInput)) {
                    lastRequestedFile = peerInput;
                    peer.search(lastRequestedFile);
                } else if (isDownload) {
                    String serverIP = inputParts[0];
                    String serverPort = inputParts[1];

                    if ((!serverIP.equals(ip) || !serverPort.equals(port)) && lastRequestedFile != null) {
                        peer.requestFileToPeer(serverIP, serverPort, lastRequestedFile);
                    } else {
                        System.out.println("Erro: nao pode requisitar o arquivo para si proprio");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}