package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ServerImpl extends UnicastRemoteObject implements ServerInterface {
    private Map<String, List<String[]>> filePeersMap;

    public ServerImpl() throws RemoteException {
        super();
        this.filePeersMap = new HashMap<>();
    }

    /**
     * Insert a new Peer in the data structure.
     *
     * @param ip the ip address of the peer
     * @param port the port of the peer
     * @param fileNames the file names of the peer
     * @return JOIN_OK
     * @throws RemoteException exception if something goes wrong
     */
    public String join(String ip, String port, List<String> fileNames) throws RemoteException {
        for (String fileName : fileNames) {
            List<String[]> peersWithFile = filePeersMap.getOrDefault(fileName, new ArrayList<>());
            peersWithFile.add(new String[]{ip, port});
            filePeersMap.put(fileName, peersWithFile);
        }

        System.out.printf("Peer %s:%s adicionado com arquivos %s%n", ip, port, String.join(" ", fileNames));
        return "JOIN_OK";
    }

    /**
     * Search for a given file name in the data structure.
     *
     * @param ip the ip address of the peer requesting
     * @param port the port of the peer requesting
     * @param fileName the requested file name
     * @return a list of peers who contain the file, or an empty list otherwise
     * @throws RemoteException exception if something goes wrong
     */
    public List<String[]> search(String ip, String port, String fileName) throws RemoteException {
        System.out.printf("Peer %s:%s solicitou arquivo %s%n", ip, port, fileName);
        return filePeersMap.getOrDefault(fileName, new ArrayList<>());
    }

    /**
     * Update the peer information with the newest file
     *
     * @param ip the ip address of the peer
     * @param port the port of the peer
     * @param fileName the newest file name
     * @return UPDATE_OK
     * @throws RemoteException exception if something goes wrong
     */
    public String update(String ip, String port, String fileName) throws RemoteException {
        List<String[]> peersWithFile = filePeersMap.getOrDefault(fileName, new ArrayList<>());
        peersWithFile.add(new String[]{ip, port});
        filePeersMap.put(fileName, peersWithFile);
        return "UPDATE_OK";
    }
}
