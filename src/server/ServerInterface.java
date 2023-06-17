package server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerInterface extends Remote {
    public String join(String peerName) throws RemoteException;
    public List<String> search(String fileName, String ip, int port) throws RemoteException;
    public String update(String newFileName, String ip, int port) throws RemoteException;
}
