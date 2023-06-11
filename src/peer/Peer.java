package peer;

import server.ServerInterface;

import java.io.*;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Peer {
    private String ip;
    private int port;
    private String folder;
    List<String> files;

    public Peer(String ip, int port, String folder) {
        this.ip = ip;
        this.port = port;
        this.folder = folder;

        File peerFolder = new File(folder);
        if (!peerFolder.exists()) {
            if (peerFolder.mkdirs()) {
                System.out.println("Peer's folder created on the path: " + folder);
            } else {
                System.out.println("Error to create the peer's folder on the path: " + folder);
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

    public static void main(String[] args)  {
        if (args.length < 3) {
            System.out.println("Usage: java peer.Peer <IP> <port> <folder>");
            return;
        }

        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        String folder = args[2];

        Peer peer = new Peer(ip, port, folder);

        // run join() on the server
        peer.send();
    }
}
