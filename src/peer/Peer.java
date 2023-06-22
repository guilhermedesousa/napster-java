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

    public Peer(String ip, String port, String folder) {
        this.ip = ip;
        this.port = port;
        this.folder = folder;

        this.createPeerFolder();
        this.files = this.listFiles();
    }

    private void createPeerFolder() {
        File peerFolder = new File(folder);

        if (!peerFolder.exists()) {
            peerFolder.mkdirs();
        }
    }

    private List<String> listFiles() {
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

    private String filesToString() {
        return String.join(" ", this.files);
    }

    public void send() {
        String fileNames = this.filesToString();

        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface sic = (ServerInterface) registry.lookup("rmi://127.0.0.1/server");
            String response = sic.join(String.format("%s %s %s", ip, port, fileNames));

            if (response.equals("JOIN_OK")) {
                System.out.println(String.format("Sou peer %s:%s com arquivos %s", ip, port, fileNames));
            }
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public void requestFileToPeer(String serverIP, String serverPort, String fileName) throws IOException {
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

        fos.close();
        is.close();
        s.close();
    }

    public List<String[]> getListOfPeers(String fileName) {
        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface sic = (ServerInterface) registry.lookup("rmi://127.0.0.1/server");
            List<String[]> response = sic.search(fileName, ip, port);

            StringBuilder builder = new StringBuilder();
            builder.append("peers com arquivo solicitado: ");

            for (String[] peer : response) {
                builder.append(String.format("%s:%s ", peer[0], peer[1]));
            }

            System.out.println(builder.toString());
            return response;
        } catch (NotBoundException | IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void updatePeerList(String fileName) {
        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface sic = (ServerInterface) registry.lookup("rmi://127.0.0.1/server");
            String response = sic.update(fileName, ip, port);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    private static String getPeerInput() throws IOException {
        InputStreamReader is = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(is);
        return reader.readLine();
    }

    private void startFileServer(String serverPort) {
        try {
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(serverPort));

            while (true) {
                Socket clientSocket = serverSocket.accept();

                Thread clientThread = new Thread(() -> {
                    handleClientConnection(clientSocket, this);
                });

                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClientConnection(Socket clientSocket, Peer peer) {
        try {
            InputStreamReader is = new InputStreamReader(clientSocket.getInputStream());
            BufferedReader reader = new BufferedReader(is);

            OutputStream os = clientSocket.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);

            String fileName = reader.readLine();

            File fileToSend = new File(folder, fileName);

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: java peer.Peer <IP> <port> <folder>");
            return;
        }

        String ip = args[0];
        String port = args[1];
        String folder = args[2];

        Peer peer = new Peer(ip, port, folder);

        peer.send();

        Thread fileServerThread = new Thread(() -> {
            peer.startFileServer(port);
        });
        fileServerThread.start();

        String requestedFile = "";

        while (true) {
            String peerInput = getPeerInput();

            if (peerInput.equalsIgnoreCase("exit")) {
                System.out.println("Closing the Peer...");
                break;
            }
            
            boolean isSearch = peerInput.matches("^[^/\\\\?*:|\"<>\\s]+\\.[\\w]+$");
            boolean isDownload = peerInput.matches("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)$");

            if (isSearch) {
                requestedFile = peerInput;
                List<String[]> listOfPeers = peer.getListOfPeers(peerInput);
            } else if (isDownload) {
                String serverIP = peerInput.split(":")[0];
                String serverPort = peerInput.split(":")[1];

                if (!serverIP.equals(ip) || !serverPort.equals(port)) {
                    peer.requestFileToPeer(serverIP, serverPort, requestedFile);
                    peer.updatePeerList(requestedFile);
                    System.out.printf("Arquivo %s baixado com sucesso na pasta %s\n", requestedFile, folder);
                }
            }
        }
    }
}
