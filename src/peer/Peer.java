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

        File peerFolder = new File(folder);
        if (!peerFolder.exists()) {
            if(!peerFolder.mkdirs()) {
                return;
            }
        }

        this.files = listFiles();
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
            ServerInterface sic = (ServerInterface) registry.lookup("server");
            String response = sic.join(String.format("%s %s %s", ip, port, fileNames));

            if (response.equals("JOIN_OK")) {
                System.out.println(String.format("Sou peer %s:%s com arquivos %s", ip, port, fileNames));
            }
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }

    public void requestFileToPeer(List<String[]> peers, String fileName) throws IOException {
        if (peers.size() > 0) {
            String[] selectedPeer = peers.get(0);
            Socket s = new Socket(selectedPeer[0], Integer.parseInt(selectedPeer[1]));

            OutputStream os = s.getOutputStream();
            DataOutputStream writer = new DataOutputStream(os);

//            InputStreamReader isr = new InputStreamReader(s.getInputStream());
//            BufferedReader reader = new BufferedReader(isr);

            writer.writeBytes(fileName + "\n");

            File directory = new File(folder);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            DataInputStream dis = new DataInputStream(s.getInputStream());
            long fileSize = dis.readLong();
//            long fileSize = Long.parseLong(reader.readLine());
            System.out.println("ok");

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
    }

    public List<String[]> getListOfPeers(String fileName) {
        try {
            Registry registry = LocateRegistry.getRegistry();
            ServerInterface sic = (ServerInterface) registry.lookup("server");
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

//    public void updatePeerList(String fileName) {
//        try {
//            Registry registry = LocateRegistry.getRegistry();
//            ServerInterface sic = (ServerInterface) registry.lookup("server");
//            String response = sic.update(fileName, ip, port);
//        } catch (RemoteException | NotBoundException e) {
//            e.printStackTrace();
//        }
//    }

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

        while (true) {
            String peerInput = getPeerInput();

            if (peerInput.equalsIgnoreCase("exit")) {
                System.out.println("Closing the Peer...");
                break;
            }
            
            boolean isSearch = peerInput.matches("^(.+)/.(.+)$");
            boolean isDownload = peerInput.matches("^(.+)/:(.+)$");
            
            String[] inputParts = peerInput.split(" ");

            if (inputParts.length == 1) {
                List<String[]> listOfPeers = peer.getListOfPeers(inputParts[0]);
            } 
        }
    }
}
