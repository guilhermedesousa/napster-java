package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ServerImpl extends UnicastRemoteObject implements ServerInterface {
    private Map<String, List<String>> filePeersMap;

    public ServerImpl() throws RemoteException {
        super();
        this.filePeersMap = new HashMap<>();
    }

    private void registerPeer(String peerData) {
        String[] peerDataParts = peerData.split(" ");
        String ip = peerDataParts[0];
        String port = peerDataParts[1];
        String[] fileNames = Arrays.copyOfRange(peerDataParts, 2, peerDataParts.length);

        for (String fileName: fileNames) {
            List<String> peersWithFile = filePeersMap.getOrDefault(fileName, new ArrayList<>());
            peersWithFile.add(String.format("%s %s", ip, port));
            filePeersMap.put(fileName, peersWithFile);
        }

        System.out.println(String.format("Peer %s:%s adicionado com arquivos %s", ip, port, String.join(" ", fileNames)));
    }

    public String join(String peerData) throws RemoteException {
        registerPeer(peerData);
        return "JOIN_OK";
    }

    public List<String> search(String fileName) throws RemoteException {
        // TODO: get ip and port of the peer and print it
        return filePeersMap.getOrDefault(fileName, new ArrayList<>());
    }
}
